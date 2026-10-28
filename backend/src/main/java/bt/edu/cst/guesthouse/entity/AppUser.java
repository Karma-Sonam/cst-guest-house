package bt.edu.cst.guesthouse.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id public UUID id;
    @Column(nullable = false) public String fullName;
    @Column(nullable = false, unique = true) public String email;
    public String phone;
    @Column(nullable = false) public String passwordHash;
    @Column(nullable = false) public String role;
    public boolean active = true;
    public Instant createdAt = Instant.now();
}
