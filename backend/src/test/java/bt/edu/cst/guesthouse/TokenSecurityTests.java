package bt.edu.cst.guesthouse.config;

import bt.edu.cst.guesthouse.config.SecurityConfig;
import bt.edu.cst.guesthouse.entity.*;
import bt.edu.cst.guesthouse.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenSecurityTests {
    @Test void validSignedTokenIsRejectedAfterSessionRevocation() {
        var config=new SecurityConfig();
        var users=mock(UserRepository.class);
        var sessions=mock(SessionRepository.class);
        var secret="test-only-secret-with-at-least-32-bytes";
        var user=new AppUser(); user.id=UUID.randomUUID(); user.role="USER";
        var session=new AuthSession(); session.id=UUID.randomUUID(); session.userId=user.id;
        session.expiresAt=Instant.now().plusSeconds(3600);
        when(users.findById(user.id)).thenReturn(Optional.of(user));
        when(sessions.findById(session.id)).thenReturn(Optional.of(session));
        var encoder=config.jwtEncoder(secret);
        var claims=JwtClaimsSet.builder().issuer("guesthouse").subject(user.id.toString())
            .id(session.id.toString()).issuedAt(Instant.now()).expiresAt(session.expiresAt).build();
        var header=JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        var token=encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
        var decoder=config.jwtDecoder(secret,sessions,users);
        assertEquals(user.id.toString(),decoder.decode(token).getSubject());
        when(sessions.findById(session.id)).thenReturn(Optional.empty());
        assertThrows(JwtValidationException.class,() -> decoder.decode(token));
    }
}
