package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.AuthDtos.*;
import bt.edu.cst.guesthouse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody Register request) { return auth.register(request); }
    @PostMapping("/login") public Token login(@Valid @RequestBody Login request) { return auth.login(request); }
    @GetMapping("/me") public User me(@AuthenticationPrincipal Jwt jwt) { return auth.me(jwt); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) { auth.logout(jwt); }
}
