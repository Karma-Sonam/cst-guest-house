package bt.edu.cst.guesthouse.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="facilities")
public class Facility {
    @Id public UUID id;
    public String type;
    public String name;
    public boolean active;
}
