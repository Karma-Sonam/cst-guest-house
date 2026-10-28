package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.BookingDtos.*;
import bt.edu.cst.guesthouse.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin")
public class AdminBookingController {
    private final BookingService service;
    public AdminBookingController(BookingService service) { this.service=service; }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @GetMapping("/bookings") public PageResult<Summary> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String status,
        @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(actor(jwt),true,status,search,page,size); }
    @GetMapping("/bookings/{id}") public Detail detail(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.detail(id,actor(jwt),true); }
    @PatchMapping("/bookings/{id}/approve") public Summary approve(@PathVariable UUID id,@Valid @RequestBody Approve request,@AuthenticationPrincipal Jwt jwt) { return service.approve(id,request,actor(jwt)); }
    @PatchMapping("/bookings/{id}/assign-room") public Summary reassign(@PathVariable UUID id,@Valid @RequestBody Approve request,@AuthenticationPrincipal Jwt jwt) { return service.reassign(id,request,actor(jwt)); }
    @PatchMapping("/bookings/{id}/reject") public Summary reject(@PathVariable UUID id,@Valid @RequestBody Remark request,@AuthenticationPrincipal Jwt jwt) { return service.reject(id,request.remark(),actor(jwt)); }
    @PatchMapping("/bookings/{id}/extra-bed/setup") public Summary setup(@PathVariable UUID id,@RequestBody BedSetup request,@AuthenticationPrincipal Jwt jwt) { return service.setup(id,request.added(),actor(jwt)); }
    @PatchMapping("/bookings/{id}/check-in") public Summary checkIn(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.checkIn(id,actor(jwt)); }
    @PatchMapping("/bookings/{id}/check-out") public Summary checkOut(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.checkOut(id,actor(jwt)); }
    @GetMapping("/rooms") public List<RoomView> rooms() { return service.rooms(); }
    @PutMapping("/rooms/{id}") public RoomView roomDetails(@PathVariable UUID id,@Valid @RequestBody RoomDetails request,@AuthenticationPrincipal Jwt jwt) { return service.roomDetails(id,request,actor(jwt)); }
    @PatchMapping("/rooms/{id}/status") public RoomView status(@PathVariable UUID id,@Valid @RequestBody RoomStatus request,@AuthenticationPrincipal Jwt jwt) { return service.roomStatus(id,request.status(),actor(jwt)); }
    @PutMapping("/rooms/{id}/capacity") public RoomView capacity(@PathVariable UUID id,@Valid @RequestBody Capacity request,@AuthenticationPrincipal Jwt jwt) { return service.capacity(id,request,actor(jwt)); }
    @PatchMapping("/bookings/{id}/extra-bed") public Summary bed(@PathVariable UUID id,@Valid @RequestBody BedDecision request,@AuthenticationPrincipal Jwt jwt) { return service.bedDecision(id,request,actor(jwt)); }
    @PatchMapping("/bookings/{id}/remarks") public Summary remarks(@PathVariable UUID id,@Valid @RequestBody Remark request,@AuthenticationPrincipal Jwt jwt) { return service.remarks(id,request.remark(),actor(jwt)); }
    @PatchMapping("/bookings/{id}/reopen") public Summary reopen(@PathVariable UUID id,@Valid @RequestBody Remark request,@AuthenticationPrincipal Jwt jwt) { return service.reopen(id,request.remark(),actor(jwt)); }
}
