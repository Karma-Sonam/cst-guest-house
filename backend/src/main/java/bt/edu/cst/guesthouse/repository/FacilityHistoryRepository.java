package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.FacilityHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FacilityHistoryRepository extends JpaRepository<FacilityHistory,UUID> {
    List<FacilityHistory> findByRequestIdOrderByChangedAtAsc(UUID requestId);
}
