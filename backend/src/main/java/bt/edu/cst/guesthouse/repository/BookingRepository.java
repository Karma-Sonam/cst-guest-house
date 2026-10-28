package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.*;
public interface BookingRepository extends JpaRepository<Booking,UUID>, JpaSpecificationExecutor<Booking> {
    Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId,Pageable page);
    Page<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId,String status,Pageable page);
    Page<Booking> findByStatusOrderByCreatedAtDesc(String status,Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Booking b where b.id=:id")
    Optional<Booking> lockById(UUID id);
    @Query("select count(b) from Booking b where b.assignedRoomId=:room and b.status in ('APPROVED','CHECKED_IN') and b.checkIn < :departure and b.checkOut > :arrival and b.id <> :excluded")
    long overlaps(UUID room,LocalDate arrival,LocalDate departure,UUID excluded);
    @Query("select count(b) from Booking b where b.assignedRoomId=:room and b.status in ('APPROVED','CHECKED_IN')")
    long activeForRoom(UUID room);
    boolean existsByAssignedRoomIdAndStatusAndIdNot(UUID room,String status,UUID id);
    long countByStatus(String status);
    long countByUserId(UUID userId);
    long countByUserIdAndStatus(UUID userId,String status);
    @Query("select count(b) from Booking b where b.checkIn=:date and b.status=:status")
    long countByCheckInAndStatus(LocalDate date,String status);
    @Query("select count(b) from Booking b where b.checkOut=:date and b.status=:status")
    long countByCheckOutAndStatus(LocalDate date,String status);
    @Query("select b from Booking b where b.checkIn=:date and b.status=:status order by b.createdAt")
    Page<Booking> findByCheckInAndStatus(LocalDate date,String status,Pageable page);
    @Query("select b from Booking b where b.checkOut=:date and b.status=:status order by b.createdAt")
    Page<Booking> findByCheckOutAndStatus(LocalDate date,String status,Pageable page);
    @Query("select coalesce(sum(b.guests),0) from Booking b where b.status='CHECKED_IN'")
    long currentGuestCount();
    @Query("select count(b) from Booking b where b.extraBedRequested=true and b.extraBedDecision='PENDING' and b.status in ('PENDING','APPROVED')")
    long pendingExtraBeds();
}
