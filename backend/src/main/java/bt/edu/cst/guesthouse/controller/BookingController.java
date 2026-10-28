package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.BookingDtos.*;
import bt.edu.cst.guesthouse.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;
    public BookingController(BookingService service) { this.service=service; }
    private boolean admin(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Summary create(@Valid @RequestBody Request request,@AuthenticationPrincipal Jwt jwt) { return service.create(request,UUID.fromString(jwt.getSubject())); }
    @GetMapping("/my") public PageResult<Summary> mine(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(UUID.fromString(jwt.getSubject()),false,status,page,size);
    }
    @GetMapping("/{id}") public Detail detail(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,Authentication auth) {
        return service.detail(id,UUID.fromString(jwt.getSubject()),admin(auth));
    }
    @PatchMapping("/{id}/cancel") public Summary cancel(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,Authentication auth) {
        return service.cancel(id,UUID.fromString(jwt.getSubject()),admin(auth));
    }
}
