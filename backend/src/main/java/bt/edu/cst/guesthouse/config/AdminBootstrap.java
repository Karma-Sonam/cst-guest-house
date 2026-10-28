package bt.edu.cst.guesthouse.config;

import bt.edu.cst.guesthouse.entity.AppUser;
import bt.edu.cst.guesthouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String email;
    private final String password;
    public AdminBootstrap(UserRepository users, PasswordEncoder passwords,
        @Value("${ADMIN_EMAIL:}") String email, @Value("${ADMIN_PASSWORD:}") String password) {
        this.users=users; this.passwords=passwords; this.email=email.strip().toLowerCase(Locale.ROOT); this.password=password;
    }
    @Override @Transactional
    public void run(ApplicationArguments arguments) {
        if (email.isEmpty() && password.isEmpty()) return;
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || password.length()<12
            || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Admin bootstrap requires a valid email and a password of at least 12 characters and at most 72 UTF-8 bytes");
        var existing=users.findByEmail(email);
        if (existing.isPresent()) {
            if (!"ADMIN".equals(existing.get().role)) throw new IllegalStateException("Bootstrap email belongs to a non-admin account");
            return; // Never reset an existing administrator password on startup.
        }
        var user=new AppUser(); user.id=UUID.randomUUID(); user.fullName="Guest House Administrator";
        user.email=email; user.passwordHash=passwords.encode(password); user.role="ADMIN";
        users.saveAndFlush(user);
    }
}
