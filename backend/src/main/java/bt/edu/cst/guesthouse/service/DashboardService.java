package bt.edu.cst.guesthouse.service;
import bt.edu.cst.guesthouse.dto.BookingDtos.*;
import bt.edu.cst.guesthouse.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.UUID;
@Service @Transactional(readOnly=true)
public class DashboardService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final FacilityRepository facilities;
    private final FacilityRequestRepository requests;
    public DashboardService(BookingRepository bookings,RoomRepository rooms,FacilityRepository facilities,FacilityRequestRepository requests) {
        this.bookings=bookings; this.rooms=rooms; this.facilities=facilities; this.requests=requests;
    }
    public record Counts(long total,long pending,long approved,long completed) {}
    public Counts own(UUID user) { return new Counts(bookings.countByUserId(user),bookings.countByUserIdAndStatus(user,"PENDING"),bookings.countByUserIdAndStatus(user,"APPROVED"),bookings.countByUserIdAndStatus(user,"COMPLETED")); }
    public record Summary(long totalRooms,long standardRooms,long specialRooms,long availableRooms,long occupiedRooms,long maintenanceRooms,
        long pendingBookings,long approvedBookings,long currentGuests,long todayArrivals,long todayDepartures,long extraBedRequests,long conventionHallPending,long diningHallPending) {}
    public Summary summary() {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Thimphu")); var inventory=rooms.findAll(); UUID none=new UUID(0,0);
        long available=inventory.stream().filter(r -> "ACTIVE".equals(r.serviceStatus))
            .filter(r -> r.normalCapacity!=null && r.maxCapacity!=null)
            .filter(r -> bookings.overlaps(r.id,today,today.plusDays(1),none)==0 && !bookings.existsByAssignedRoomIdAndStatusAndIdNot(r.id,"CHECKED_IN",none)).count();
        long occupied=inventory.stream().filter(r -> bookings.existsByAssignedRoomIdAndStatusAndIdNot(r.id,"CHECKED_IN",none)).count();
        return new Summary(inventory.size(),inventory.stream().filter(r -> "STANDARD".equals(r.category)).count(),inventory.stream().filter(r -> "SPECIAL".equals(r.category)).count(),
            available,occupied,inventory.stream().filter(r -> "MAINTENANCE".equals(r.serviceStatus)).count(),bookings.countByStatus("PENDING"),bookings.countByStatus("APPROVED"),
            bookings.currentGuestCount(),bookings.countByCheckInAndStatus(today,"APPROVED"),bookings.countByCheckOutAndStatus(today,"CHECKED_IN"),bookings.pendingExtraBeds(),
            requests.countByFacilityIdAndStatus(facilities.findByType("CONVENTION").orElseThrow().id,"PENDING"),requests.countByFacilityIdAndStatus(facilities.findByType("DINING").orElseThrow().id,"PENDING"));
    }
    public PageResult<bt.edu.cst.guesthouse.dto.BookingDtos.Summary> movements(LocalDate date,boolean arrivals,int page,int size) {
        if (page<0 || size<1 || size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
        var result=arrivals?bookings.findByCheckInAndStatus(date,"APPROVED",PageRequest.of(page,size)):bookings.findByCheckOutAndStatus(date,"CHECKED_IN",PageRequest.of(page,size));
        return new PageResult<>(result.getContent().stream().map(bt.edu.cst.guesthouse.dto.BookingDtos.Summary::from).toList(),result.getTotalElements(),page,size);
    }
}
