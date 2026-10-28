package bt.edu.cst.guesthouse.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
@RestController
public class PublicConfigController {
    private final boolean registration;
    private final bt.edu.cst.guesthouse.repository.RoomRepository rooms;
    private final bt.edu.cst.guesthouse.repository.FacilityRepository facilities;
    private final String phone,email,address;
    public PublicConfigController(@Value("${app.registration-enabled}") boolean registration,
        bt.edu.cst.guesthouse.repository.RoomRepository rooms,bt.edu.cst.guesthouse.repository.FacilityRepository facilities,
        @Value("${app.contact.phone:}") String phone,@Value("${app.contact.email:}") String email,@Value("${app.contact.address:}") String address) {
        this.registration=registration; this.rooms=rooms; this.facilities=facilities; this.phone=phone; this.email=email; this.address=address;
    }
    public record Contact(String phone,String email,String address) {}
    public record Config(boolean registrationEnabled,long totalRooms,long standardRooms,long specialRooms,long totalFacilities,Contact contact) {}
    @GetMapping("/api/public/config") public Config config() {
        var inventory=rooms.findAll();
        return new Config(registration,inventory.size(),inventory.stream().filter(r -> "STANDARD".equals(r.category)).count(),
            inventory.stream().filter(r -> "SPECIAL".equals(r.category)).count(),facilities.count(),new Contact(phone,email,address));
    }
}
