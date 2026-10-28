package bt.edu.cst.guesthouse.service;

import bt.edu.cst.guesthouse.dto.AuthDtos.*;
import bt.edu.cst.guesthouse.entity.*;
import bt.edu.cst.guesthouse.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final long expiration;
    private final boolean registrationEnabled;
    private final String dummyHash;
    public AuthService(UserRepository users, SessionRepository sessions, PasswordEncoder passwords,
        JwtEncoder encoder, @Value("${app.jwt-expiration}") long expiration,
        @Value("${app.registration-enabled}") boolean registrationEnabled) {
        if (expiration < 60 || expiration > 86400) throw new IllegalArgumentException("JWT expiration must be 60–86400 seconds");
        this.users=users; this.sessions=sessions; this.passwords=passwords;
        this.encoder=encoder; this.expiration=expiration; this.registrationEnabled=registrationEnabled;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    @Transactional
    public User register(Register request) {
        if (!registrationEnabled) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Registration is disabled");
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes");
        String email=normalize(request.email());
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already registered");
        AppUser user=new AppUser(); user.id=UUID.randomUUID(); user.email=email;
        user.fullName=request.fullName().strip(); user.phone=request.phone();
        user.passwordHash=passwords.encode(request.password()); user.role="USER";
        return User.from(users.saveAndFlush(user));
    }
    @Transactional
    public Token login(Login request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        AppUser user=users.findByEmail(normalize(request.email())).orElse(null);
        boolean valid=passwords.matches(request.password(), user == null ? dummyHash : user.passwordHash);
        if (user == null || !valid || !user.active) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        Instant now=Instant.now();
        AuthSession session=new AuthSession(); session.id=UUID.randomUUID(); session.userId=user.id;
        session.expiresAt=now.plusSeconds(expiration); sessions.save(session);
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer("guesthouse").subject(user.id.toString())
            .id(session.id.toString()).issuedAt(now).expiresAt(session.expiresAt).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Token(token,expiration,User.from(user));
    }
    public User me(Jwt jwt) { return User.from(users.findById(UUID.fromString(jwt.getSubject())).orElseThrow()); }
    @Transactional public User profile(Jwt jwt,Profile request) {
        AppUser user=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow();
        user.fullName=request.fullName().strip(); user.phone=request.phone(); return User.from(users.saveAndFlush(user));
    }
    @Transactional
    public void logout(Jwt jwt) { sessions.deleteById(UUID.fromString(jwt.getId())); }
}
