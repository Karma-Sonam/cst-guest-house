package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BookingHistoryRepository extends JpaRepository<BookingHistory,UUID> {
    List<BookingHistory> findByBookingIdOrderByChangedAtAsc(UUID bookingId);
}
