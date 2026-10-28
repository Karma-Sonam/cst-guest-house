package bt.edu.cst.guesthouse;
import bt.edu.cst.guesthouse.service.BookingRules;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class BookingRulesTests {
    private final LocalDate today=LocalDate.of(2026,10,27);
    @Test void invalidDateRangesAreRejected() {
        assertThrows(ResponseStatusException.class,() -> BookingRules.dates(today.minusDays(1),today.plusDays(1),today));
        assertThrows(ResponseStatusException.class,() -> BookingRules.dates(today,today,today));
        assertDoesNotThrow(() -> BookingRules.dates(today,today.plusDays(1),today));
    }
    @Test void thirdGuestNeedsSupportedApprovedBed() {
        assertThrows(ResponseStatusException.class,() -> BookingRules.capacity(3,2,3,true,false));
        assertThrows(ResponseStatusException.class,() -> BookingRules.capacity(3,2,3,false,true));
        assertDoesNotThrow(() -> BookingRules.capacity(3,2,3,true,true));
        assertDoesNotThrow(() -> BookingRules.capacity(2,2,3,true,false));
        assertThrows(ResponseStatusException.class,() -> BookingRules.capacity(4,2,3,true,true));
    }
    @Test void unknownSpecialCapacityCannotBeAllocated() {
        assertThrows(ResponseStatusException.class,() -> BookingRules.capacity(1,null,null,false,false));
    }
}
