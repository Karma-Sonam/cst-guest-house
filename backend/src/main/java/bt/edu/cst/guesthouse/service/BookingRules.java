package bt.edu.cst.guesthouse.service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
public final class BookingRules {
    private BookingRules() {}
    public static void dates(LocalDate arrival,LocalDate departure,LocalDate today) {
        if (arrival==null || departure==null || arrival.isBefore(today) || !departure.isAfter(arrival))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Arrival must not be in the past and departure must follow arrival");
    }
    public static void capacity(int guests,Integer normal,Integer maximum,boolean supportsExtraBed,boolean approvedBed) {
        if (normal==null || maximum==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Room capacity has not been configured");
        if (guests>maximum || (guests>normal && !(supportsExtraBed && approvedBed)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Room capacity is insufficient without an approved supported extra bed");
    }
}
