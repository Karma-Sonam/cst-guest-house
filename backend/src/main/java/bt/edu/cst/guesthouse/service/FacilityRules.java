package bt.edu.cst.guesthouse.service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
public final class FacilityRules {
    private FacilityRules() {}
    public static void schedule(LocalDate date,LocalTime start,LocalTime end,LocalDateTime now) {
        if (date==null || start==null || end==null || !end.isAfter(start) || LocalDateTime.of(date,start).isBefore(now))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a future start and an end after it on the same date");
    }
}
