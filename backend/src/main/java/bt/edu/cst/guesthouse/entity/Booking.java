package bt.edu.cst.guesthouse.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="bookings")
public class Booking {
    @Id public UUID id;
    public String reference;
    public UUID userId;
    public String fullName;
    public String email;
    public String phone;
    public String organization;
    public String designation;
    public String guestCategory;
    public String purpose;
    public int guests;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public String preferredCategory;
    public UUID assignedRoomId;
    public boolean extraBedRequested;
    public String extraBedDecision;
    public boolean extraBedAdded;
    public String status;
    public String userRemark;
    public String adminRemark;
    public Instant actualCheckIn;
    public Instant actualCheckOut;
    public Instant createdAt;
    public Instant updatedAt;
}
