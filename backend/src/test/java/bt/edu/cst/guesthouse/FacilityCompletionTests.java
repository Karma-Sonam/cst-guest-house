package bt.edu.cst.guesthouse;
import bt.edu.cst.guesthouse.entity.*;
import bt.edu.cst.guesthouse.repository.*;
import bt.edu.cst.guesthouse.service.FacilityService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FacilityCompletionTests {
    private final FacilityRepository facilities=mock(FacilityRepository.class);
    private final FacilityRequestRepository requests=mock(FacilityRequestRepository.class);
    private final FacilityHistoryRepository history=mock(FacilityHistoryRepository.class);
    private final FacilityService service=new FacilityService(facilities,requests,history,mock(BookingRepository.class),mock(EntityManager.class),
        LocalTime.of(7,0),LocalTime.of(9,0),LocalTime.NOON,LocalTime.of(14,0),LocalTime.of(18,0),LocalTime.of(20,0));
    private FacilityRequest request(LocalDate date) {
        var facility=new Facility(); facility.id=UUID.randomUUID(); facility.type="CONVENTION";
        var request=new FacilityRequest(); request.id=UUID.randomUUID(); request.facilityId=facility.id;
        request.status="APPROVED"; request.usageDate=date; request.startTime=LocalTime.of(10,0); request.endTime=LocalTime.NOON;
        when(requests.lockById(request.id)).thenReturn(Optional.of(request));
        when(facilities.findById(facility.id)).thenReturn(Optional.of(facility)); return request;
    }
    @Test void completedPastUsageRecordsActorAndHistory() {
        var request=request(LocalDate.now(ZoneId.of("Asia/Thimphu")).minusDays(1)); var actor=UUID.randomUUID();
        assertEquals("COMPLETED",service.complete(request.id,actor,"Event ended").status());
        var event=org.mockito.ArgumentCaptor.forClass(FacilityHistory.class); verify(history).save(event.capture());
        assertEquals("APPROVED",event.getValue().previousStatus);
        assertEquals(actor,event.getValue().changedBy);
    }
    @Test void futureUsageCannotBeCompletedEarly() {
        var request=request(LocalDate.now(ZoneId.of("Asia/Thimphu")).plusDays(1));
        assertThrows(ResponseStatusException.class,() -> service.complete(request.id,UUID.randomUUID(),"Too early"));
        verify(requests,never()).saveAndFlush(any());
    }
}
