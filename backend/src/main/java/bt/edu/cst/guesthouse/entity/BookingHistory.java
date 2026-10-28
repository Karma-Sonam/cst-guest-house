package bt.edu.cst.guesthouse.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="booking_status_history")
public class BookingHistory {
    @Id public UUID id;
    public UUID bookingId;
    public String previousStatus;
    public String newStatus;
    public UUID changedBy;
    public String remark;
    public Instant changedAt;
}
