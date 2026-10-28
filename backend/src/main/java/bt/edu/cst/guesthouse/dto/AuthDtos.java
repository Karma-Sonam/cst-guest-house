package bt.edu.cst.guesthouse.dto;
import jakarta.validation.constraints.*;
import bt.edu.cst.guesthouse.entity.AppUser;
import java.util.UUID;
public final class AuthDtos {
    private AuthDtos() {}
    public record Register(@NotBlank @Size(max=120) String fullName,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=12,max=72) String password,
        @Size(max=30) String phone) {}
    public record Login(@NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=72) String password) {}
    public record User(UUID id, String fullName, String email, String phone, String role) {
        public static User from(AppUser u) { return new User(u.id,u.fullName,u.email,u.phone,u.role); }
    }
    public record Token(String token, long expiresIn, User user) {}
    public record Profile(@NotBlank @Size(max=120) String fullName,@Size(max=30) String phone) {}
}
