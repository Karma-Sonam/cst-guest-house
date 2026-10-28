package bt.edu.cst.guesthouse.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="facility_request_history")
public class FacilityHistory {
    @Id public UUID id;
    public UUID requestId;
    public String previousStatus;
    public String newStatus;
    public UUID changedBy;
    public String remark;
    public Instant changedAt;
}
