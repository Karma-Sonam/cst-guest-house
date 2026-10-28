package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.FacilityRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
public interface FacilityRequestRepository extends JpaRepository<FacilityRequest,UUID>,JpaSpecificationExecutor<FacilityRequest> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from FacilityRequest r where r.id=:id")
    Optional<FacilityRequest> lockById(UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from FacilityRequest r where r.bookingId=:booking and r.status='PENDING' order by r.id")
    List<FacilityRequest> lockPendingForBooking(UUID booking);
    @Query("select count(r) from FacilityRequest r where r.facilityId=:facility and r.status='APPROVED' and r.usageDate=:date and r.startTime < :end and r.endTime > :start and r.id <> :excluded")
    long overlaps(UUID facility,LocalDate date,LocalTime start,LocalTime end,UUID excluded);
    long countByFacilityIdAndStatus(UUID facilityId,String status);
}
