package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface SessionRepository extends JpaRepository<AuthSession, UUID> {}
