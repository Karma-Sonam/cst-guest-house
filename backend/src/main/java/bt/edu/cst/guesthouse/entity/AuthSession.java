package bt.edu.cst.guesthouse.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public Instant expiresAt;
}
