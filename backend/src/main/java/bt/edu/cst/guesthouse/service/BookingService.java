package bt.edu.cst.guesthouse.service;

import bt.edu.cst.guesthouse.dto.BookingDtos.*;
import bt.edu.cst.guesthouse.entity.*;
import bt.edu.cst.guesthouse.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class BookingService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final BookingHistoryRepository history;
    private final AuditLogRepository audit;
    private final FacilityService facilities;
    private static final UUID NONE=new UUID(0,0);
    private static final ZoneId ZONE=ZoneId.of("Asia/Thimphu");
    public BookingService(BookingRepository bookings,RoomRepository rooms,BookingHistoryRepository history,AuditLogRepository audit,FacilityService facilities) {
        this.bookings=bookings; this.rooms=rooms; this.history=history; this.audit=audit; this.facilities=facilities;
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
    private Booking get(UUID id,UUID actor,boolean admin,boolean lock) {
        Booking b=(lock?bookings.lockById(id):bookings.findById(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!admin && !b.userId.equals(actor)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return b;
    }
    private void state(Booking b,String... allowed) {
        if (!List.of(allowed).contains(b.status)) throw conflict("Action is not permitted for status "+b.status);
    }
    private void event(Booking b,String previous,String next,UUID actor,String remark) {
        BookingHistory h=new BookingHistory(); h.id=UUID.randomUUID(); h.bookingId=b.id;
        h.previousStatus=previous; h.newStatus=next; h.changedBy=actor; h.remark=remark; h.changedAt=Instant.now(); history.save(h);
    }
    private Summary save(Booking b,UUID actor,String next,String remark) {
        String previous=b.status; b.status=next; b.updatedAt=Instant.now();
        if (remark!=null) b.adminRemark=remark;
        bookings.saveAndFlush(b); event(b,previous,next,actor,remark); return Summary.from(b);
    }
    public List<RoomView> rooms() { return rooms.findAllByOrderByRoomNumberAsc().stream().map(r -> RoomView.from(r,0)).toList(); }
    public RoomView room(UUID id) { return RoomView.from(rooms.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)),0); }
    public List<RoomView> available(LocalDate arrival,LocalDate departure,int guests) {
        return available(arrival,departure,guests,Category.STANDARD);
    }
    public List<RoomView> available(LocalDate arrival,LocalDate departure,int guests,Category category) {
        BookingRules.dates(arrival,departure,LocalDate.now(ZONE));
        if (guests<1 || guests>3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"One standard-room request supports 1–3 guests; submit separate requests for larger parties");
        return rooms.findAllByOrderByRoomNumberAsc().stream()
            .filter(r -> category.name().equals(r.category) && "ACTIVE".equals(r.serviceStatus))
            .filter(r -> r.maxCapacity!=null && guests<=r.maxCapacity && (guests<=r.normalCapacity || r.supportsExtraBed))
            .filter(r -> bookings.overlaps(r.id,arrival,departure,NONE)==0).map(r -> RoomView.from(r,guests)).toList();
    }
    @Transactional public Summary create(Request request,UUID actor) {
        BookingRules.dates(request.checkIn(),request.checkOut(),LocalDate.now(ZONE));
        if (request.preferredCategory()==Category.STANDARD && request.guests()==3 && !request.extraBedRequested())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Three guests require an extra-bed request");
        if (request.preferredCategory()==Category.SPECIAL && request.extraBedRequested())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Extra beds are supported only for standard rooms");
        Booking b=new Booking(); b.id=UUID.randomUUID(); b.reference="GH-"+b.id.toString().replace("-","");
        b.userId=actor; b.fullName=request.fullName().strip(); b.email=request.email().strip().toLowerCase(Locale.ROOT);
        b.phone=request.phone(); b.organization=request.organization(); b.designation=request.designation();
        b.guestCategory=request.guestCategory().name(); b.purpose=request.purpose().strip(); b.guests=request.guests();
        b.checkIn=request.checkIn(); b.checkOut=request.checkOut(); b.preferredCategory=request.preferredCategory().name();
        b.extraBedRequested=request.extraBedRequested(); b.extraBedDecision=b.extraBedRequested?"PENDING":"NOT_REQUESTED";
        b.status="PENDING"; b.userRemark=request.remark(); b.createdAt=Instant.now(); b.updatedAt=b.createdAt;
        Room held=hold(b,request.roomId());
        bookings.saveAndFlush(b); event(b,null,"PENDING",actor,"Request submitted; room "+held.roomNumber+" held pending review"); return Summary.from(b);
    }
    private Room hold(Booking booking,UUID requested) {
        // All creators acquire this small inventory in one order, including automatic allocation.
        var inventory=rooms.lockInventory();
        if (requested!=null && inventory.stream().noneMatch(r -> r.id.equals(requested)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found");
        for (Room room:inventory) {
            if (requested!=null && !room.id.equals(requested)) continue;
            if (!room.category.equals(booking.preferredCategory) || !"ACTIVE".equals(room.serviceStatus)) continue;
            if (room.normalCapacity==null || room.maxCapacity==null || booking.guests>room.maxCapacity) continue;
            if (booking.guests>room.normalCapacity && (!room.supportsExtraBed || !booking.extraBedRequested)) continue;
            if (bookings.overlaps(room.id,booking.checkIn,booking.checkOut,booking.id)>0) continue;
            booking.assignedRoomId=room.id; return room;
        }
        throw conflict(requested==null?"No suitable rooms are available for these dates; special-room capacity must be verified":"This room is no longer available or suitable for these dates. Choose another room");
    }
    public Detail detail(UUID id,UUID actor,boolean admin) {
        Booking b=get(id,actor,admin,false);
        return new Detail(Summary.from(b),history.findByBookingIdOrderByChangedAtAsc(id).stream().map(Event::from).toList());
    }
    public PageResult<Summary> list(UUID actor,boolean admin,String status,int page,int size) {
        return list(actor,admin,status,"",page,size);
    }
    public PageResult<Summary> list(UUID actor,boolean admin,String status,String search,int page,int size) {
        if (page<0 || size<1 || size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
        if (search.length()>120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Search must be at most 120 characters");
        String term=search.strip().toLowerCase(Locale.ROOT);
        Pageable pagination=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"));
        Page<Booking> result=bookings.findAll((org.springframework.data.jpa.domain.Specification<Booking>) (root,query,cb) -> {
            var filters=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!admin) filters.add(cb.equal(root.get("userId"),actor));
            if (status!=null && !status.isBlank()) filters.add(cb.equal(root.get("status"),status));
            if (!term.isEmpty()) filters.add(cb.or(
                cb.gt(cb.locate(cb.lower(root.get("reference")),term),0),
                cb.gt(cb.locate(cb.lower(root.get("fullName")),term),0),
                cb.gt(cb.locate(cb.lower(root.get("email")),term),0)));
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        },pagination);
        return new PageResult<>(result.getContent().stream().map(Summary::from).toList(),result.getTotalElements(),page,size);
    }
    private Room allocate(Booking b,UUID roomId,boolean approveBed) {
        Room r=rooms.lockById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found"));
        if (!r.category.equals(b.preferredCategory)) throw conflict("Room category does not match the booking request");
        if (!"ACTIVE".equals(r.serviceStatus)) throw conflict("Room is not in service");
        if (approveBed && (!b.extraBedRequested || !r.supportsExtraBed || !"STANDARD".equals(r.category)))
            throw conflict("Extra bed was not requested or is unsupported");
        BookingRules.capacity(b.guests,r.normalCapacity,r.maxCapacity,r.supportsExtraBed,approveBed);
        if (bookings.overlaps(r.id,b.checkIn,b.checkOut,b.id)>0) throw conflict("Room is already reserved for those dates");
        b.assignedRoomId=r.id;
        b.extraBedDecision=b.extraBedRequested?(approveBed?"APPROVED":"REJECTED"):"NOT_REQUESTED";
        return r;
    }
    @Transactional public Summary approve(UUID id,Approve request,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"PENDING");
        BookingRules.dates(b.checkIn,b.checkOut,LocalDate.now(ZONE));
        Room r=allocate(b,request.roomId(),request.approveExtraBed());
        return save(b,actor,"APPROVED","Room "+r.roomNumber+" assigned. "+Objects.requireNonNullElse(request.remark(),""));
    }
    @Transactional public Summary reassign(UUID id,Approve request,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"APPROVED"); UUID previous=b.assignedRoomId;
        Room r=allocate(b,request.roomId(),request.approveExtraBed()); b.extraBedAdded=false;
        return save(b,actor,"APPROVED","Reassigned from "+previous+" to room "+r.roomNumber+". "+Objects.requireNonNullElse(request.remark(),""));
    }
    @Transactional public Summary reject(UUID id,String remark,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"PENDING"); return save(b,actor,"REJECTED",remark);
    }
    @Transactional public Summary cancel(UUID id,UUID actor,boolean admin) {
        Booking b=get(id,actor,admin,true); state(b,admin?new String[]{"PENDING","APPROVED"}:new String[]{"PENDING"});
        if ("APPROVED".equals(b.status) && !b.checkIn.isAfter(LocalDate.now(ZONE))) throw conflict("Only future approved stays can be cancelled");
        Summary result=save(b,actor,"CANCELLED","Cancelled by "+(admin?"administrator":"requester"));
        facilities.cancelPendingLinked(b.id,actor); return result;
    }
    @Transactional public Summary setup(UUID id,boolean added,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"APPROVED","CHECKED_IN");
        if (!"APPROVED".equals(b.extraBedDecision)) throw conflict("No approved extra bed");
        if (!added && "CHECKED_IN".equals(b.status) && b.guests>2) throw conflict("Required bed cannot be removed during stay");
        b.extraBedAdded=added; return save(b,actor,b.status,added?"Extra bed added":"Extra bed removed");
    }
    @Transactional public Summary checkIn(UUID id,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"APPROVED"); LocalDate today=LocalDate.now(ZONE);
        if (today.isBefore(b.checkIn) || !today.isBefore(b.checkOut)) throw conflict("Check-in must be within the reserved stay dates");
        Room r=rooms.lockById(b.assignedRoomId).orElseThrow();
        if (!"ACTIVE".equals(r.serviceStatus)) throw conflict("Room is not in service");
        BookingRules.capacity(b.guests,r.normalCapacity,r.maxCapacity,r.supportsExtraBed,"APPROVED".equals(b.extraBedDecision));
        if (b.guests>r.normalCapacity && !b.extraBedAdded) throw conflict("Required extra bed has not been added");
        // A delayed checkout must also prevent another guest entering the same room.
        if (bookings.existsByAssignedRoomIdAndStatusAndIdNot(r.id,"CHECKED_IN",b.id))
            throw conflict("Previous guest has not checked out");
        b.actualCheckIn=Instant.now(); return save(b,actor,"CHECKED_IN","Guest checked in");
    }
    @Transactional public Summary checkOut(UUID id,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"CHECKED_IN");
        b.actualCheckOut=Instant.now(); b.extraBedAdded=false;
        event(b,"CHECKED_IN","CHECKED_OUT",actor,"Guest checked out");
        b.status="CHECKED_OUT"; return save(b,actor,"COMPLETED","Stay completed; room released");
    }
    private void auditRoom(UUID id,UUID actor,String action,String description) {
        AuditLog log=new AuditLog(); log.id=UUID.randomUUID(); log.actorId=actor; log.entityId=id;
        log.action=action; log.description=description; log.createdAt=Instant.now(); audit.save(log);
    }
    private String roomText(String value) { return value==null || value.isBlank()?null:value.strip(); }
    @Transactional public RoomView roomDetails(UUID id,RoomDetails request,UUID actor) {
        Room r=rooms.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found"));
        if (bookings.activeForRoom(id)>0) throw conflict("Resolve active allocations before editing room details");
        String number=request.roomNumber().strip();
        if (rooms.existsByRoomNumberIgnoreCaseAndIdNot(number,id)) throw conflict("Room number is already in use");
        if ("STANDARD".equals(r.category) && !Objects.equals(request.beds(),2))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Standard rooms retain two regular beds");
        String previous=r.roomNumber;
        r.roomNumber=number; r.standardBeds=request.beds(); r.location=roomText(request.location());
        r.description=roomText(request.description()); r.amenities=roomText(request.amenities());
        rooms.saveAndFlush(r);
        auditRoom(id,actor,"ROOM_DETAILS","Room "+previous+" details updated; current number "+number);
        return RoomView.from(r,0);
    }
    @Transactional public RoomView roomStatus(UUID id,ServiceStatus status,UUID actor) {
        Room r=rooms.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (status!=ServiceStatus.ACTIVE && bookings.activeForRoom(id)>0) throw conflict("Resolve active room allocations before changing serviceability");
        String previous=r.serviceStatus;
        r.serviceStatus=status.name(); rooms.saveAndFlush(r);
        auditRoom(id,actor,"ROOM_STATUS",previous+" -> "+r.serviceStatus); return RoomView.from(r,0);
    }
    @Transactional public RoomView capacity(UUID id,Capacity request,UUID actor) {
        Room r=rooms.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"SPECIAL".equals(r.category)) throw conflict("Standard rooms retain capacity 2, or 3 with an extra bed");
        if (bookings.activeForRoom(id)>0) throw conflict("Resolve active allocations before changing capacity");
        if (!Objects.equals(request.normalCapacity(),request.maxCapacity()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Special rooms do not support extra beds; normal and maximum capacity must match");
        r.normalCapacity=request.normalCapacity(); r.maxCapacity=request.maxCapacity(); rooms.saveAndFlush(r);
        auditRoom(id,actor,"ROOM_CAPACITY","Special room capacity configured as "+request.normalCapacity()); return RoomView.from(r,0);
    }
    @Transactional public Summary bedDecision(UUID id,BedDecision request,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"PENDING","APPROVED");
        if (!b.extraBedRequested || !"STANDARD".equals(b.preferredCategory)) throw conflict("No eligible extra-bed request");
        if ("APPROVED".equals(b.status)) {
            Room r=rooms.lockById(b.assignedRoomId).orElseThrow();
            if (request.approved() && !r.supportsExtraBed) throw conflict("Assigned room does not support an extra bed");
            BookingRules.capacity(b.guests,r.normalCapacity,r.maxCapacity,r.supportsExtraBed,request.approved());
        }
        b.extraBedDecision=request.approved()?"APPROVED":"REJECTED";
        if (!request.approved()) b.extraBedAdded=false;
        return save(b,actor,b.status,"Extra bed "+b.extraBedDecision+". "+request.remark());
    }
    @Transactional public Summary remarks(UUID id,String remark,UUID actor) {
        Booking b=get(id,actor,true,true);
        if ("COMPLETED".equals(b.status)) throw conflict("Completed records are immutable");
        return save(b,actor,b.status,remark);
    }
    @Transactional public Summary reopen(UUID id,String remark,UUID actor) {
        Booking b=get(id,actor,true,true); state(b,"REJECTED","CANCELLED");
        BookingRules.dates(b.checkIn,b.checkOut,LocalDate.now(ZONE));
        b.assignedRoomId=null; b.extraBedAdded=false;
        b.extraBedDecision=b.extraBedRequested?"PENDING":"NOT_REQUESTED";
        hold(b,null);
        return save(b,actor,"PENDING",remark);
    }
}
