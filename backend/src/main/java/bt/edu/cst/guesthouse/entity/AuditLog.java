package bt.edu.cst.guesthouse.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="audit_log")
public class AuditLog {
    @Id public UUID id;
    public UUID actorId;
    public String action;
    public UUID entityId;
    public String description;
    public Instant createdAt;
}
