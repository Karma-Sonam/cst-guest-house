package bt.edu.cst.guesthouse;
import bt.edu.cst.guesthouse.service.FacilityRules;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class FacilityRulesTests {
    private final LocalDate date=LocalDate.of(2026,11,1);
    private final LocalDateTime now=date.atTime(9,0);
    @Test void futureSameDayIntervalIsAllowed() {
        assertDoesNotThrow(() -> FacilityRules.schedule(date,LocalTime.of(10,0),LocalTime.of(12,0),now));
    }
    @Test void equalReversedAndMissingTimesAreRejected() {
        assertThrows(ResponseStatusException.class,() -> FacilityRules.schedule(date,LocalTime.NOON,LocalTime.NOON,now));
        assertThrows(ResponseStatusException.class,() -> FacilityRules.schedule(date,LocalTime.NOON,LocalTime.of(10,0),now));
        assertThrows(ResponseStatusException.class,() -> FacilityRules.schedule(date,null,null,now));
    }
    @Test void pastStartIsRejectedEvenWhenDateIsToday() {
        assertThrows(ResponseStatusException.class,() -> FacilityRules.schedule(date,LocalTime.of(8,0),LocalTime.NOON,now));
    }
}
