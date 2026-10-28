package bt.edu.cst.guesthouse.controller;
import bt.edu.cst.guesthouse.dto.BookingDtos.*;
import bt.edu.cst.guesthouse.service.BookingService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/rooms")
public class RoomController {
    private final BookingService service;
    public RoomController(BookingService service) { this.service=service; }
    @GetMapping public List<RoomView> rooms() { return service.rooms(); }
    @GetMapping("/available") public List<RoomView> available(@RequestParam LocalDate checkIn,@RequestParam LocalDate checkOut,@RequestParam int guests) {
        return service.available(checkIn,checkOut,guests);
    }
    @GetMapping("/{id}") public RoomView room(@PathVariable UUID id) { return service.room(id); }
}
