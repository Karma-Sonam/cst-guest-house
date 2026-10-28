package bt.edu.cst.guesthouse.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
@RestController
public class PublicConfigController {
    private final boolean registration;
    public PublicConfigController(@Value("${app.registration-enabled}") boolean registration) { this.registration=registration; }
    public record Config(boolean registrationEnabled) {}
    @GetMapping("/api/public/config") public Config config() { return new Config(registration); }
}
