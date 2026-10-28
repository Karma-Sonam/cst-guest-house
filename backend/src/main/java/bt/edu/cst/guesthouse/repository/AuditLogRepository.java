package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AuditLogRepository extends JpaRepository<AuditLog,UUID> {}
