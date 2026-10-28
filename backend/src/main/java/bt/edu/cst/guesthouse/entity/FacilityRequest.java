package bt.edu.cst.guesthouse.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="facility_requests")
public class FacilityRequest {
    @Id public UUID id;
    public UUID facilityId;
    public UUID userId;
    public UUID bookingId;
    public String title;
    public String purpose;
    public LocalDate usageDate;
    public LocalTime startTime;
    public LocalTime endTime;
    public String mealPeriod;
    public int participants;
    public String status;
    public String userRemark;
    public String adminRemark;
    public Instant createdAt;
    public Instant updatedAt;
}
