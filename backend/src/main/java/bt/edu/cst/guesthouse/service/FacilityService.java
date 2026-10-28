package bt.edu.cst.guesthouse.service;

import bt.edu.cst.guesthouse.dto.FacilityDtos.*;
import bt.edu.cst.guesthouse.dto.BookingDtos.PageResult;
import bt.edu.cst.guesthouse.entity.*;
import bt.edu.cst.guesthouse.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.persistence.criteria.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service @Transactional(readOnly=true)
public class FacilityService {
    private final FacilityRepository facilities;
    private final FacilityRequestRepository requests;
    private final FacilityHistoryRepository history;
    private final BookingRepository bookings;
    private final EntityManager entityManager;
    private final Map<MealPeriod,LocalTime[]> periods;
    private static final ZoneId ZONE=ZoneId.of("Asia/Thimphu");
    public FacilityService(FacilityRepository facilities,FacilityRequestRepository requests,
        FacilityHistoryRepository history,BookingRepository bookings,EntityManager entityManager,
        @Value("${app.dining.breakfast-start:07:00}") LocalTime breakfastStart,
        @Value("${app.dining.breakfast-end:09:00}") LocalTime breakfastEnd,
        @Value("${app.dining.lunch-start:12:00}") LocalTime lunchStart,
        @Value("${app.dining.lunch-end:14:00}") LocalTime lunchEnd,
        @Value("${app.dining.dinner-start:18:00}") LocalTime dinnerStart,
        @Value("${app.dining.dinner-end:20:00}") LocalTime dinnerEnd) {
        this.facilities=facilities; this.requests=requests; this.history=history; this.bookings=bookings; this.entityManager=entityManager;
        periods=Map.of(MealPeriod.BREAKFAST,new LocalTime[]{breakfastStart,breakfastEnd},
            MealPeriod.LUNCH,new LocalTime[]{lunchStart,lunchEnd},MealPeriod.DINNER,new LocalTime[]{dinnerStart,dinnerEnd});
        for (var window:periods.values()) if (!window[1].isAfter(window[0])) throw new IllegalArgumentException("Invalid dining period configuration");
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
    private Facility facility(Type type) { return facilities.findByType(type.name()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private void linkedBooking(UUID id,UUID actor,boolean admin) {
        if (id==null) return;
        Booking b=bookings.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Linked booking not found"));
        if (!admin && !b.userId.equals(actor)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Linked booking not found");
        if (!List.of("PENDING","APPROVED","CHECKED_IN").contains(b.status)) throw conflict("Linked accommodation booking is not active");
    }
    private FacilityRequest get(UUID id,UUID actor,boolean admin,boolean lock) {
        FacilityRequest r=(lock?requests.lockById(id):requests.findById(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Approval first reads the immutable link; refresh after waiting for the write lock.
        if (lock) entityManager.refresh(r,LockModeType.PESSIMISTIC_WRITE);
        if (!admin && !r.userId.equals(actor)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return r;
    }
    private Summary view(FacilityRequest r) {
        String type=facilities.findById(r.facilityId).orElseThrow().type;
        boolean conflict="PENDING".equals(r.status) && requests.overlaps(r.facilityId,r.usageDate,r.startTime,r.endTime,r.id)>0;
        return Summary.from(r,type,conflict);
    }
    private void event(FacilityRequest r,String previous,UUID actor,String remark) {
        FacilityHistory h=new FacilityHistory(); h.id=UUID.randomUUID(); h.requestId=r.id;
        h.previousStatus=previous; h.newStatus=r.status; h.changedBy=actor; h.remark=remark; h.changedAt=Instant.now(); history.save(h);
    }
    private Summary change(FacilityRequest r,String status,UUID actor,String remark) {
        String previous=r.status; r.status=status; r.adminRemark=remark; r.updatedAt=Instant.now();
        requests.saveAndFlush(r); event(r,previous,actor,remark); return view(r);
    }
    private Summary create(Type type,UUID booking,UUID actor,String title,String purpose,LocalDate date,
        LocalTime start,LocalTime end,String meal,int count,String remark) {
        FacilityRules.schedule(date,start,end,LocalDateTime.now(ZONE));
        linkedBooking(booking,actor,false);
        Facility facility=facility(type);
        if (!facility.active) throw conflict("Facility is unavailable");
        FacilityRequest r=new FacilityRequest(); r.id=UUID.randomUUID(); r.facilityId=facility.id;
        r.userId=actor; r.bookingId=booking; r.title=title.strip(); r.purpose=purpose.strip();
        r.usageDate=date; r.startTime=start; r.endTime=end; r.mealPeriod=meal; r.participants=count;
        r.userRemark=remark; r.status="PENDING"; r.createdAt=Instant.now(); r.updatedAt=r.createdAt;
        requests.saveAndFlush(r); event(r,null,actor,"Request submitted"); return view(r);
    }
    @Transactional public Summary convention(Convention body,UUID actor) {
        return create(Type.CONVENTION,body.bookingId(),actor,body.title(),body.purpose(),body.eventDate(),
            body.startTime(),body.endTime(),null,body.expectedParticipants(),body.remark());
    }
    @Transactional public Summary dining(Dining body,UUID actor) {
        LocalTime start=body.startTime(),end=body.endTime();
        if (body.mealPeriod()!=MealPeriod.OTHER) {
            var window=periods.get(body.mealPeriod());
            if ((start!=null && !start.equals(window[0])) || (end!=null && !end.equals(window[1])))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Named meal periods use the configured time window");
            start=window[0]; end=window[1];
        }
        return create(Type.DINING,body.bookingId(),actor,body.title(),body.purpose(),body.usageDate(),start,end,
            body.mealPeriod().name(),body.numberOfGuests(),body.remark());
    }
    public Map<MealPeriod,List<LocalTime>> diningPeriods() {
        Map<MealPeriod,List<LocalTime>> result=new EnumMap<>(MealPeriod.class);
        periods.forEach((key,window) -> result.put(key,List.of(window[0],window[1]))); return result;
    }
    public PageResult<Summary> list(Type type,UUID actor,boolean admin,String status,LocalDate date,MealPeriod meal,int page,int size) {
        if (page<0 || size<1 || size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
        if (meal!=null && type!=Type.DINING) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Meal filter is only applicable to dining requests");
        UUID facilityId=facility(type).id;
        var results=requests.findAll((root,query,builder) -> {
            List<Predicate> conditions=new ArrayList<>();
            conditions.add(builder.equal(root.get("facilityId"),facilityId));
            if (!admin) conditions.add(builder.equal(root.get("userId"),actor));
            if (status!=null) conditions.add(builder.equal(root.get("status"),status));
            if (date!=null) conditions.add(builder.equal(root.get("usageDate"),date));
            if (meal!=null) conditions.add(builder.equal(root.get("mealPeriod"),meal.name()));
            return builder.and(conditions.toArray(Predicate[]::new));
        },PageRequest.of(page,size,Sort.by(Sort.Order.desc("usageDate"),Sort.Order.asc("startTime"))));
        return new PageResult<>(results.getContent().stream().map(this::view).toList(),results.getTotalElements(),page,size);
    }
    public Detail detail(UUID id,UUID actor,boolean admin) {
        FacilityRequest r=get(id,actor,admin,false);
        return new Detail(view(r),history.findByRequestIdOrderByChangedAtAsc(id).stream().map(Event::from).toList());
    }
    @Transactional public Summary decide(Type type,UUID id,UUID actor,boolean approve,String remark) {
        // Lock linked booking before request, matching accommodation cancellation's lock order.
        FacilityRequest snapshot=get(id,actor,true,false);
        if (!snapshot.facilityId.equals(facility(type).id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (approve) linkedBooking(snapshot.bookingId,actor,true);
        FacilityRequest r=get(id,actor,true,true);
        if (!"PENDING".equals(r.status)) throw conflict("Only pending requests can be reviewed");
        if (approve) {
            FacilityRules.schedule(r.usageDate,r.startTime,r.endTime,LocalDateTime.now(ZONE));
            Facility f=facilities.lockById(r.facilityId).orElseThrow();
            if (!f.active) throw conflict("Facility is unavailable");
            if (requests.overlaps(r.facilityId,r.usageDate,r.startTime,r.endTime,r.id)>0) throw conflict("Facility schedule overlaps an approved reservation");
        }
        return change(r,approve?"APPROVED":"REJECTED",actor,remark);
    }
    @Transactional public Summary cancel(UUID id,UUID actor,boolean admin,String remark) {
        FacilityRequest r=get(id,actor,admin,true);
        if (!"PENDING".equals(r.status) && !(admin && "APPROVED".equals(r.status))) throw conflict("Request cannot be cancelled in its current status");
        if ("APPROVED".equals(r.status) && !LocalDateTime.of(r.usageDate,r.startTime).isAfter(LocalDateTime.now(ZONE)))
            throw conflict("An approved request can only be cancelled before usage starts");
        return change(r,"CANCELLED",actor,remark);
    }
    @Transactional public void cancelPendingLinked(UUID booking,UUID actor) {
        for (FacilityRequest r:requests.lockPendingForBooking(booking)) change(r,"CANCELLED",actor,"Linked accommodation booking cancelled");
    }
    @Transactional public Summary complete(UUID id,UUID actor,String remark) {
        FacilityRequest r=get(id,actor,true,true);
        if (!"APPROVED".equals(r.status)) throw conflict("Only approved requests can be completed");
        if (LocalDateTime.of(r.usageDate,r.endTime).isAfter(LocalDateTime.now(ZONE))) throw conflict("Facility usage has not ended");
        return change(r,"COMPLETED",actor,remark);
    }
}
