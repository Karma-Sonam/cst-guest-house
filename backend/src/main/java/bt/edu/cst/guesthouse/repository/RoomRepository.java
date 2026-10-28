package bt.edu.cst.guesthouse.repository;
import bt.edu.cst.guesthouse.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface RoomRepository extends JpaRepository<Room,UUID> {
    List<Room> findAllByOrderByRoomNumberAsc();
    boolean existsByRoomNumberIgnoreCaseAndIdNot(String roomNumber,UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Room r where r.id=:id")
    Optional<Room> lockById(UUID id);
}
