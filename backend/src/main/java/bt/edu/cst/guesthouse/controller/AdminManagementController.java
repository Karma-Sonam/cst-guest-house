package bt.edu.cst.guesthouse.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    private final JdbcTemplate jdbc;
    public AdminManagementController(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Activation(@NotNull Boolean active) {}
    @GetMapping("/users")
    public Map<String,Object> users(@RequestParam(defaultValue="") String search,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        if (page<0 || page>100000 || size<1 || size>100 || search.length()>120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid search or pagination");
        String term=search.strip().toLowerCase(Locale.ROOT);
        String where=" WHERE position(? in lower(full_name || ' ' || email))>0";
        return Map.of("content",jdbc.queryForList("SELECT id, full_name AS \"fullName\",email,phone,role,active,created_at AS \"createdAt\" FROM app_users"+where+" ORDER BY created_at DESC,id LIMIT ? OFFSET ?",term,size,page*size),
            "totalElements",jdbc.queryForObject("SELECT count(*) FROM app_users"+where,Long.class,term),"page",page,"size",size);
    }
    @PatchMapping("/users/{id}/active")
    @Transactional
    public Map<String,Object> active(@PathVariable UUID id,@Valid @RequestBody Activation request,@AuthenticationPrincipal Jwt jwt) {
        UUID actor=UUID.fromString(jwt.getSubject());
        // Serialize activation decisions, including concurrent attempts to disable administrators.
        jdbc.execute("SELECT pg_advisory_xact_lock(73402119)");
        var rows=jdbc.queryForList("SELECT role,active FROM app_users WHERE id=? FOR UPDATE",id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found");
        if (!request.active() && actor.equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"You cannot deactivate your own account");
        if (!request.active() && rows.getFirst().get("role").equals("ADMIN") && Boolean.TRUE.equals(rows.getFirst().get("active")) &&
            jdbc.queryForObject("SELECT count(*) FROM app_users WHERE role='ADMIN' AND active",Long.class)<=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"The last active administrator must remain active");
        if (!rows.getFirst().get("active").equals(request.active())) {
            jdbc.update("UPDATE app_users SET active=? WHERE id=?",request.active(),id);
            jdbc.update("DELETE FROM auth_sessions WHERE user_id=?",id);
            jdbc.update("INSERT INTO audit_log(id,actor_id,action,entity_id,description,created_at) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID(),actor,"USER_ACTIVE_CHANGED",id,"Account active: "+request.active(),Instant.now().atOffset(ZoneOffset.UTC));
        }
        return Map.of("id",id,"active",request.active());
    }
    @GetMapping("/reports/summary")
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Map<String,Object> report(@RequestParam LocalDate from,@RequestParam LocalDate to) {
        long days=ChronoUnit.DAYS.between(from,to);
        if (days<1 || days>366) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an end date after the start, within 366 days");
        var bookings=jdbc.queryForList("SELECT status,count(*) AS count FROM bookings WHERE check_in<? AND check_out>? GROUP BY status ORDER BY status",to,from);
        var facilities=jdbc.queryForList("SELECT f.type AS facility,r.status,count(*) AS count FROM facility_requests r JOIN facilities f ON f.id=r.facility_id WHERE usage_date>=? AND usage_date<? GROUP BY f.type,r.status ORDER BY f.type,r.status",from,to);
        Long nights=jdbc.queryForObject("SELECT coalesce(sum(least(check_out,?)-greatest(check_in,?)),0) FROM bookings WHERE check_in<? AND check_out>? AND status IN ('APPROVED','CHECKED_IN','COMPLETED')",Long.class,to,from,to,from);
        return Map.of("from",from,"to",to,"days",days,"bookingsByStatus",bookings,"facilitiesByStatus",facilities,"reservedRoomNights",nights);
    }
}
