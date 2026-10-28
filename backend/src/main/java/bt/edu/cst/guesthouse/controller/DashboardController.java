package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.BookingDtos.*;
import bt.edu.cst.guesthouse.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.UUID;
@RestController @RequestMapping("/api")
public class DashboardController {
    private final DashboardService service;
    private final BookingService bookings;
    public DashboardController(DashboardService service,BookingService bookings) { this.service=service; this.bookings=bookings; }
    @GetMapping("/bookings/summary") public DashboardService.Counts own(@AuthenticationPrincipal Jwt jwt) { return service.own(UUID.fromString(jwt.getSubject())); }
    @GetMapping("/admin/dashboard/summary") public DashboardService.Summary summary() { return service.summary(); }
    @GetMapping("/admin/current-guests") public PageResult<Summary> guests(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return bookings.list(UUID.fromString(jwt.getSubject()),true,"CHECKED_IN",page,size); }
    @GetMapping("/admin/arrivals-departures") public PageResult<Summary> movements(@RequestParam LocalDate date,@RequestParam(defaultValue="true") boolean arrivals,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.movements(date,arrivals,page,size); }
}
