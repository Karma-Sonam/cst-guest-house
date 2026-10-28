package bt.edu.cst.guesthouse.dto;
import bt.edu.cst.guesthouse.entity.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
public final class FacilityDtos {
    private FacilityDtos() {}
    public enum Type { CONVENTION,DINING }
    public enum MealPeriod { BREAKFAST,LUNCH,DINNER,OTHER }
    public record Convention(UUID bookingId,@NotBlank @Size(max=160) String title,
        @NotBlank @Size(max=1000) String purpose,@NotNull LocalDate eventDate,
        @NotNull LocalTime startTime,@NotNull LocalTime endTime,@Min(1) int expectedParticipants,
        @Size(max=1000) String remark) {}
    public record Dining(UUID bookingId,@NotBlank @Size(max=160) String title,
        @NotBlank @Size(max=1000) String purpose,@NotNull LocalDate usageDate,
        @NotNull MealPeriod mealPeriod,LocalTime startTime,LocalTime endTime,
        @Min(1) int numberOfGuests,@Size(max=1000) String remark) {}
    public record Decision(@NotBlank @Size(max=1000) String remark) {}
    public record Summary(UUID id,String facilityType,UUID userId,UUID bookingId,String title,String purpose,
        LocalDate usageDate,LocalTime startTime,LocalTime endTime,String mealPeriod,int participants,
        String status,String userRemark,String adminRemark,boolean schedulingConflict,Instant createdAt) {
        public static Summary from(FacilityRequest r,String type,boolean conflict) {
            return new Summary(r.id,type,r.userId,r.bookingId,r.title,r.purpose,r.usageDate,r.startTime,r.endTime,
                r.mealPeriod,r.participants,r.status,r.userRemark,r.adminRemark,conflict,r.createdAt);
        }
    }
    public record Event(String previousStatus,String newStatus,UUID changedBy,String remark,Instant changedAt) {
        public static Event from(FacilityHistory h) { return new Event(h.previousStatus,h.newStatus,h.changedBy,h.remark,h.changedAt); }
    }
    public record Detail(Summary request,List<Event> timeline) {}
}
