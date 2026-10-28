package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.FacilityDtos.*;
import bt.edu.cst.guesthouse.dto.BookingDtos.PageResult;
import bt.edu.cst.guesthouse.service.FacilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.time.LocalTime;
@RestController @RequestMapping("/api")
public class FacilityController {
    private final FacilityService service;
    public FacilityController(FacilityService service) { this.service=service; }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    private boolean admin(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
    @GetMapping("/dining-hall/periods") public Map<MealPeriod,List<LocalTime>> periods() { return service.diningPeriods(); }
    @PostMapping("/convention-hall/requests") @ResponseStatus(HttpStatus.CREATED)
    public Summary convention(@Valid @RequestBody Convention body,@AuthenticationPrincipal Jwt jwt) { return service.convention(body,actor(jwt)); }
    @PostMapping("/dining-hall/requests") @ResponseStatus(HttpStatus.CREATED)
    public Summary dining(@Valid @RequestBody Dining body,@AuthenticationPrincipal Jwt jwt) { return service.dining(body,actor(jwt)); }
    @GetMapping("/convention-hall/my") public PageResult<Summary> conventions(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String status,
        @RequestParam(required=false) LocalDate date,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(Type.CONVENTION,actor(jwt),false,status,date,null,page,size);
    }
    @GetMapping("/dining-hall/my") public PageResult<Summary> dining(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String status,
        @RequestParam(required=false) LocalDate date,@RequestParam(required=false) MealPeriod mealPeriod,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(Type.DINING,actor(jwt),false,status,date,mealPeriod,page,size);
    }
    @GetMapping("/facility-requests/{id}") public Detail detail(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,Authentication auth) {
        return service.detail(id,actor(jwt),admin(auth));
    }
    @PatchMapping("/facility-requests/{id}/cancel") public Summary cancel(@PathVariable UUID id,@Valid @RequestBody Decision body,@AuthenticationPrincipal Jwt jwt,Authentication auth) {
        return service.cancel(id,actor(jwt),admin(auth),body.remark());
    }
}
