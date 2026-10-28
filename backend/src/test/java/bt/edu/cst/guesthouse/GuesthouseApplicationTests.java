package bt.edu.cst.guesthouse;

import org.junit.jupiter.api.Test;
import bt.edu.cst.guesthouse.dto.AuthDtos;
import bt.edu.cst.guesthouse.entity.AppUser;
import bt.edu.cst.guesthouse.repository.*;
import bt.edu.cst.guesthouse.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GuesthouseApplicationTests {
    private final UserRepository users=mock(UserRepository.class);
    private final SessionRepository sessions=mock(SessionRepository.class);
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder();
    private AuthService service(boolean registration) {
        return new AuthService(users,sessions,passwords,mock(JwtEncoder.class),3600,registration);
    }
    @Test void registrationHashesPasswordAndCannotChooseAdmin() {
        when(users.findByEmail("guest@example.com")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result=service(true).register(new AuthDtos.Register("Guest","Guest@Example.com","a-long-password!",null));
        assertEquals("USER",result.role());
        assertEquals("guest@example.com",result.email());
        var captor=org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(captor.capture());
        assertNotEquals("a-long-password!",captor.getValue().passwordHash);
        assertTrue(passwords.matches("a-long-password!",captor.getValue().passwordHash));
    }
    @Test void disabledRegistrationDoesNotCreateAccount() {
        var error=assertThrows(ResponseStatusException.class,() -> service(false).register(
            new AuthDtos.Register("Guest","guest@example.com","a-long-password!",null)));
        assertEquals(403,error.getStatusCode().value());
        verify(users,never()).saveAndFlush(any());
    }
    @Test void disabledAccountCannotLoginOrCreateSession() {
        var user=new AppUser(); user.active=false; user.passwordHash=passwords.encode("a-long-password!");
        when(users.findByEmail("guest@example.com")).thenReturn(Optional.of(user));
        var error=assertThrows(ResponseStatusException.class,() -> service(true).login(
            new AuthDtos.Login("guest@example.com","a-long-password!")));
        assertEquals(401,error.getStatusCode().value());
        verify(sessions,never()).save(any());
    }
    @Test void oversizedUtf8LoginPasswordIsRejectedBeforeBcrypt() {
        var error=assertThrows(ResponseStatusException.class,() -> service(true).login(
            new AuthDtos.Login("guest@example.test","🙂".repeat(20))));
        assertEquals(401,error.getStatusCode().value());
        verify(users,never()).findByEmail(any());
        verify(sessions,never()).save(any());
    }
}
