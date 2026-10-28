package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.Facility;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface FacilityRepository extends JpaRepository<Facility,UUID> {
    Optional<Facility> findByType(String type);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select f from Facility f where f.id=:id")
    Optional<Facility> lockById(UUID id);
}
