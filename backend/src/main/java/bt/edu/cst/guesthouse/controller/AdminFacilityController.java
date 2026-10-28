package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.FacilityDtos.*;
import bt.edu.cst.guesthouse.dto.BookingDtos.PageResult;
import bt.edu.cst.guesthouse.service.FacilityService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;
@RestController @RequestMapping("/api/admin")
public class AdminFacilityController {
    private final FacilityService service;
    public AdminFacilityController(FacilityService service) { this.service=service; }
    private Type type(String hall) { return hall.equals("convention-hall")?Type.CONVENTION:Type.DINING; }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @GetMapping("/{hall:convention-hall|dining-hall}/requests") public PageResult<Summary> list(@PathVariable String hall,@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) String status,@RequestParam(required=false) LocalDate date,
        @RequestParam(required=false) MealPeriod mealPeriod,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(type(hall),actor(jwt),true,status,date,mealPeriod,page,size); }
    @PatchMapping("/{hall:convention-hall|dining-hall}/{id}/approve") public Summary approve(@PathVariable String hall,@PathVariable UUID id,
        @Valid @RequestBody Decision body,@AuthenticationPrincipal Jwt jwt) { return service.decide(type(hall),id,actor(jwt),true,body.remark()); }
    @PatchMapping("/{hall:convention-hall|dining-hall}/{id}/reject") public Summary reject(@PathVariable String hall,@PathVariable UUID id,
        @Valid @RequestBody Decision body,@AuthenticationPrincipal Jwt jwt) { return service.decide(type(hall),id,actor(jwt),false,body.remark()); }
    @PatchMapping("/facility-requests/{id}/complete") public Summary complete(@PathVariable UUID id,@Valid @RequestBody Decision body,@AuthenticationPrincipal Jwt jwt) {
        return service.complete(id,actor(jwt),body.remark());
    }
}
