package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.AuthDtos.*;
import bt.edu.cst.guesthouse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users/profile")
public class ProfileController {
    private final AuthService service;
    public ProfileController(AuthService service) { this.service=service; }
    @GetMapping public User profile(@AuthenticationPrincipal Jwt jwt) { return service.me(jwt); }
    @PutMapping public User update(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Profile body) { return service.profile(jwt,body); }
}
