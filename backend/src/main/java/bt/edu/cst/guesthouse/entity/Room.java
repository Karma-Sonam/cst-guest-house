package bt.edu.cst.guesthouse.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="rooms")
public class Room {
    @Id public UUID id;
    public String roomNumber;
    public String category;
    public Integer standardBeds;
    public Integer normalCapacity;
    public Integer maxCapacity;
    public boolean supportsExtraBed;
    public String serviceStatus;
    public String location;
    public String description;
    public String amenities;
}
