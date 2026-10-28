package bt.edu.cst.guesthouse.dto;
import jakarta.validation.constraints.*;
import bt.edu.cst.guesthouse.entity.*;
import java.time.*;
import java.util.*;
public final class BookingDtos {
    private BookingDtos() {}
    public record Request(@NotBlank @Size(max=120) String fullName,
        @NotBlank @Email @Size(max=254) String email, @Size(max=30) String phone,
        @Size(max=160) String organization,@Size(max=120) String designation,
        @NotNull GuestCategory guestCategory,@NotBlank @Size(max=1000) String purpose,
        @Min(1) @Max(3) int guests,@NotNull LocalDate checkIn,@NotNull LocalDate checkOut,
        @NotNull Category preferredCategory,boolean extraBedRequested,@Size(max=1000) String remark,UUID roomId) {}
    public enum Category { STANDARD,SPECIAL }
    public enum GuestCategory { GENERAL_GUEST,COLLEGE_STAFF_GUEST,OFFICIAL_GUEST,VIP_GUEST,MINISTER,VICE_CHANCELLOR,SENIOR_OFFICIAL,OTHER_DISTINGUISHED_GUEST }
    public record Approve(@NotNull UUID roomId,boolean approveExtraBed,@Size(max=1000) String remark) {}
    public record Remark(@NotBlank @Size(max=1000) String remark) {}
    public record BedSetup(boolean added) {}
    public record BedDecision(boolean approved,@NotBlank @Size(max=1000) String remark) {}
    public record Capacity(@Min(1) @Max(3) Integer normalCapacity,@Min(1) @Max(3) Integer maxCapacity) {}
    public record RoomStatus(@NotNull ServiceStatus status) {}
    public record RoomDetails(@NotBlank @Size(max=20) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9 -]*") String roomNumber,
        @Min(1) @Max(3) Integer beds,@Size(max=120) String location,
        @Size(max=1000) String description,@Size(max=500) String amenities) {}
    public enum ServiceStatus { ACTIVE,MAINTENANCE,UNAVAILABLE }
    public record RoomView(UUID id,String roomNumber,String category,Integer beds,Integer normalCapacity,
        Integer maxCapacity,boolean supportsExtraBed,String serviceStatus,boolean extraBedRequired,
        String location,String description,String amenities) {
        public static RoomView from(Room r,int guests) { return new RoomView(r.id,r.roomNumber,r.category,r.standardBeds,
            r.normalCapacity,r.maxCapacity,r.supportsExtraBed,r.serviceStatus,r.normalCapacity!=null && guests>r.normalCapacity,
            r.location,r.description,r.amenities); }
    }
    public record Summary(UUID id,String reference,UUID userId,String fullName,String email,String phone,
        String organization,String designation,String guestCategory,String purpose,int guests,
        LocalDate checkIn,LocalDate checkOut,String preferredCategory,UUID assignedRoomId,
        boolean extraBedRequested,String extraBedDecision,boolean extraBedAdded,String status,
        String userRemark,String adminRemark,Instant actualCheckIn,Instant actualCheckOut,Instant createdAt) {
        public static Summary from(Booking b) { return new Summary(b.id,b.reference,b.userId,b.fullName,b.email,b.phone,
            b.organization,b.designation,b.guestCategory,b.purpose,b.guests,b.checkIn,b.checkOut,b.preferredCategory,
            b.assignedRoomId,b.extraBedRequested,b.extraBedDecision,b.extraBedAdded,b.status,b.userRemark,
            b.adminRemark,b.actualCheckIn,b.actualCheckOut,b.createdAt); }
    }
    public record Event(String previousStatus,String newStatus,UUID changedBy,String remark,Instant changedAt) {
        public static Event from(BookingHistory h) { return new Event(h.previousStatus,h.newStatus,h.changedBy,h.remark,h.changedAt); }
    }
    public record Detail(Summary booking,List<Event> timeline) {}
    public record PageResult<T>(List<T> content,long totalElements,int page,int size) {}
}
