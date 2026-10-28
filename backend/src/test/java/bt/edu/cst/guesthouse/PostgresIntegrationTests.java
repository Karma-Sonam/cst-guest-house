package bt.edu.cst.guesthouse;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt in only against a disposable database whose name ends in _test. */
@EnabledIfEnvironmentVariable(named="RUN_POSTGRES_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "app.registration-enabled=true","app.jwt-secret=integration-only-jwt-secret-with-at-least-32-bytes",
    "app.frontend-url=http://127.0.0.1:5173","ADMIN_EMAIL=","ADMIN_PASSWORD="})
class PostgresIntegrationTests {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String url=System.getenv("TEST_DATABASE_URL");
        if (url==null || !url.matches("^jdbc:postgresql://[^/]+/[a-zA-Z0-9_]+_test(?:\\?.*)?$"))
            throw new IllegalArgumentException("TEST_DATABASE_URL must name a disposable database ending in _test");
        properties.add("spring.datasource.url",() -> url);
        properties.add("spring.datasource.username",() -> Objects.requireNonNull(System.getenv("TEST_DATABASE_USERNAME")));
        properties.add("spring.datasource.password",() -> Objects.requireNonNull(System.getenv("TEST_DATABASE_PASSWORD")));
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    private final JsonMapper json=new JsonMapper();
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final String PASSWORD="Integration-only-password!";
    private static final String ROOM="00000000-0000-0000-0000-000000000101";
    private String user,admin;
    private final LocalDate today=LocalDate.now(ZoneId.of("Asia/Thimphu"));
    record Result(int status,JsonNode body) {}
    private Result call(String method,String path,Object body,String token) throws Exception {
        return call(method,path,body,token,null);
    }
    private Result call(String method,String path,Object body,String token,String origin) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api"+path)).timeout(Duration.ofSeconds(20));
        if (body!=null) request.header("Content-Type","application/json");
        if (token!=null) request.header("Authorization","Bearer "+token);
        if (origin!=null) request.header("Origin",origin);
        request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=client.send(request.build(),HttpResponse.BodyHandlers.ofString());
        JsonNode data=response.body().isBlank() || !response.headers().firstValue("content-type").orElse("").contains("json")
            ?json.createObjectNode():json.readTree(response.body());
        return new Result(response.statusCode(),data);
    }
    private String login(String email) throws Exception {
        Result response=call("POST","/auth/login",Map.of("email",email,"password",PASSWORD),null);
        assertEquals(200,response.status,response.body.toString()); return response.body.path("token").asText();
    }
    @BeforeEach void fixtures() throws Exception {
        jdbc.execute("TRUNCATE facility_request_history,facility_requests,booking_status_history,bookings,audit_log,auth_sessions,app_users CASCADE");
        jdbc.update("UPDATE rooms SET service_status='ACTIVE'");
        jdbc.update("UPDATE rooms SET room_number=right(id::text,3),location=null,description=null,amenities=null");
        jdbc.update("INSERT INTO app_users(id,full_name,email,password_hash,role,active) VALUES (?,?,?,?,?,true)",UUID.randomUUID(),"Integration Admin","admin@example.test",passwords.encode(PASSWORD),"ADMIN");
        assertEquals(201,call("POST","/auth/register",Map.of("fullName","Integration Guest","email","guest@example.test","password",PASSWORD,"role","ADMIN"),null).status);
        user=login("guest@example.test"); admin=login("admin@example.test");
    }
    private Map<String,Object> booking(int guests,boolean bed,LocalDate arrival) {
        return Map.of("fullName","Integration Guest","email","guest@example.test","guestCategory","GENERAL_GUEST","purpose","PostgreSQL integration test",
            "guests",guests,"checkIn",arrival.toString(),"checkOut",arrival.plusDays(1).toString(),"preferredCategory","STANDARD","extraBedRequested",bed);
    }
    private String createBooking(int guests,boolean bed,LocalDate arrival) throws Exception {
        var result=call("POST","/bookings",booking(guests,bed,arrival),user); assertEquals(201,result.status,result.body.toString()); return result.body.path("id").asText();
    }
    private Result approve(String id,boolean bed) throws Exception { return call("PATCH","/admin/bookings/"+id+"/approve",Map.of("roomId",ROOM,"approveExtraBed",bed,"remark","Test allocation"),admin); }
    @Test void pendingRoomHoldsBlockOverlapAndReleaseOnRejectionOrCancellation() throws Exception {
        LocalDate arrival=today;
        var request=new HashMap<String,Object>(booking(2,false,arrival)); request.put("roomId",ROOM);
        var first=call("POST","/bookings",request,user);
        assertEquals(201,first.status,first.body.toString()); assertEquals(ROOM,first.body.path("assignedRoomId").asText());
        assertEquals("PENDING",first.body.path("status").asText());
        String id=first.body.path("id").asText();
        assertEquals(409,call("POST","/bookings",request,user).status);
        var available=call("GET","/rooms/available?checkIn="+arrival+"&checkOut="+arrival.plusDays(1)+"&guests=2",null,user);
        assertFalse(available.body.toString().contains(ROOM));
        assertEquals(7,call("GET","/admin/dashboard/summary",null,admin).body.path("availableRooms").asInt());
        var adjacent=new HashMap<String,Object>(booking(2,false,arrival.plusDays(1))); adjacent.put("roomId",ROOM);
        assertEquals(201,call("POST","/bookings",adjacent,user).status);
        assertEquals(200,call("PATCH","/admin/bookings/"+id+"/reject",Map.of("remark","Test release"),admin).status);
        assertEquals(8,call("GET","/admin/dashboard/summary",null,admin).body.path("availableRooms").asInt());
        var replacement=call("POST","/bookings",request,user); assertEquals(201,replacement.status,replacement.body.toString());
        assertEquals(200,call("PATCH","/bookings/"+replacement.body.path("id").asText()+"/cancel",null,user).status);
        var after=call("POST","/bookings",request,user); assertEquals(201,after.status);
        assertEquals(200,approve(after.body.path("id").asText(),false).status);
        assertEquals(409,call("POST","/bookings",request,user).status);
    }
    @Test void simultaneousRequestsCannotHoldTheSameRoom() throws Exception {
        var request=new HashMap<String,Object>(booking(2,false,today.plusDays(75))); request.put("roomId",ROOM);
        try (var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(() -> call("POST","/bookings",request,user));
            var second=executor.submit(() -> call("POST","/bookings",request,user));
            var codes=new ArrayList<>(List.of(first.get(30,TimeUnit.SECONDS).status,second.get(30,TimeUnit.SECONDS).status));
            Collections.sort(codes); assertEquals(List.of(201,409),codes);
        }
        var reference=call("GET","/public/config",null,null);
        assertEquals(8,reference.body.path("standardRooms").asInt()); assertEquals(2,reference.body.path("specialRooms").asInt());
    }
    @Test void roomDetailsAreValidatedAuditedAndProtectedByActiveAllocations() throws Exception {
        var details=Map.of("roomNumber","A-101","beds",2,"location","Test wing","description","Test-only room description","amenities","Test desk");
        assertEquals(403,call("PUT","/admin/rooms/"+ROOM,details,user).status);
        var saved=call("PUT","/admin/rooms/"+ROOM,details,admin);
        assertEquals(200,saved.status,saved.body.toString());
        assertEquals("A-101",saved.body.path("roomNumber").asText());
        var view=call("GET","/rooms/"+ROOM,null,user);
        assertEquals("Test wing",view.body.path("location").asText());
        assertEquals("Test desk",view.body.path("amenities").asText());
        assertEquals(2,view.body.path("normalCapacity").asInt());
        assertEquals("STANDARD",view.body.path("category").asText());
        var duplicate=new HashMap<String,Object>(details); duplicate.put("roomNumber","102");
        assertEquals(409,call("PUT","/admin/rooms/"+ROOM,duplicate,admin).status);
        duplicate.put("roomNumber","a-101");
        assertEquals(409,call("PUT","/admin/rooms/00000000-0000-0000-0000-000000000102",duplicate,admin).status);
        var invalid=new HashMap<String,Object>(details); invalid.put("beds",3);
        assertEquals(400,call("PUT","/admin/rooms/"+ROOM,invalid,admin).status);
        invalid.put("beds",2); invalid.put("description","x".repeat(1001));
        assertEquals(400,call("PUT","/admin/rooms/"+ROOM,invalid,admin).status);
        String booking=createBooking(2,false,today.plusDays(60)); assertEquals(200,approve(booking,false).status);
        assertEquals(409,call("PUT","/admin/rooms/"+ROOM,details,admin).status);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action='ROOM_DETAILS'",Integer.class));
    }
    @Test void bookingSearchCombinesStatusAndPaginationAndTreatsWildcardsLiterally() throws Exception {
        String first=createBooking(2,false,today.plusDays(50));
        String second=createBooking(2,false,today.plusDays(51));
        String reference=call("GET","/bookings/"+first,null,user).body.path("booking").path("reference").asText();
        var exact=call("GET","/admin/bookings?search="+reference.toLowerCase(Locale.ROOT),null,admin);
        assertEquals(200,exact.status,exact.body.toString());
        assertEquals(1,exact.body.path("totalElements").asInt());
        assertEquals(first,exact.body.path("content").get(0).path("id").asText());
        assertEquals(2,call("GET","/admin/bookings?search=INTEGRATION",null,admin).body.path("totalElements").asInt());
        var page=call("GET","/admin/bookings?search=guest%40example.test&size=1&page=1",null,admin);
        assertEquals(2,page.body.path("totalElements").asInt());
        assertEquals(1,page.body.path("content").size());
        assertEquals(200,call("PATCH","/bookings/"+second+"/cancel",null,user).status);
        assertEquals(1,call("GET","/admin/bookings?search=integration&status=CANCELLED",null,admin).body.path("totalElements").asInt());
        assertEquals(0,call("GET","/admin/bookings?search=%25",null,admin).body.path("totalElements").asInt());
        assertEquals(0,call("GET","/admin/bookings?search=_",null,admin).body.path("totalElements").asInt());
        assertEquals(400,call("GET","/admin/bookings?search="+"a".repeat(121),null,admin).status);
        assertEquals(403,call("GET","/admin/bookings?search=integration",null,user).status);
        assertEquals(201,call("POST","/auth/register",Map.of("fullName","Other Guest","email","search-other@example.test","password",PASSWORD),null).status);
        assertEquals(0,call("GET","/bookings/my",null,login("search-other@example.test")).body.path("totalElements").asInt());
    }
    @Test void accountManagementRevokesSessionsAndProtectsAdministrators() throws Exception {
        assertEquals(403,call("GET","/admin/users",null,user).status);
        var list=call("GET","/admin/users?search=guest%40example.test",null,admin);
        assertEquals(200,list.status,list.body.toString());
        assertEquals(1,list.body.path("totalElements").asInt());
        assertFalse(list.body.toString().contains("password"));
        String id=list.body.path("content").get(0).path("id").asText();
        assertEquals(200,call("PATCH","/admin/users/"+id+"/active",Map.of("active",false),admin).status);
        assertEquals(401,call("GET","/auth/me",null,user).status);
        assertEquals(401,call("POST","/auth/login",Map.of("email","guest@example.test","password",PASSWORD),null).status);
        assertEquals(200,call("PATCH","/admin/users/"+id+"/active",Map.of("active",true),admin).status);
        assertEquals(401,call("GET","/auth/me",null,user).status);
        assertNotNull(login("guest@example.test"));
        String self=call("GET","/auth/me",null,admin).body.path("id").asText();
        assertEquals(409,call("PATCH","/admin/users/"+self+"/active",Map.of("active",false),admin).status);
        assertEquals(400,call("PATCH","/admin/users/"+id+"/active",Map.of(),admin).status);
        assertEquals(400,call("GET","/admin/users?size=101",null,admin).status);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action='USER_ACTIVE_CHANGED'",Integer.class));
    }
    @Test void reportsClipNightsToRangeAndEnforceAccessAndDates() throws Exception {
        assertEquals(403,call("GET","/admin/reports/summary?from="+today+"&to="+today.plusDays(1),null,user).status);
        String id=createBooking(2,false,today);
        assertEquals(200,approve(id,false).status);
        var report=call("GET","/admin/reports/summary?from="+today+"&to="+today.plusDays(2),null,admin);
        assertEquals(200,report.status,report.body.toString());
        assertEquals(1,report.body.path("reservedRoomNights").asInt());
        assertEquals(1,report.body.path("bookingsByStatus").get(0).path("count").asInt());
        var empty=call("GET","/admin/reports/summary?from="+today.plusDays(1)+"&to="+today.plusDays(2),null,admin);
        assertEquals(0,empty.body.path("reservedRoomNights").asInt());
        assertEquals(0,empty.body.path("bookingsByStatus").size());
        assertEquals(400,call("GET","/admin/reports/summary?from="+today+"&to="+today,null,admin).status);
    }
    @Test void authenticationOwnershipRevocationAndCors() throws Exception {
        assertEquals("USER",call("GET","/auth/me",null,user).body.path("role").asText());
        assertEquals(401,call("GET","/auth/me",null,null).status);
        assertEquals(403,call("GET","/admin/dashboard/summary",null,user).status);
        assertEquals(200,call("GET","/public/config",null,null,"http://127.0.0.1:5173").status);
        assertEquals(403,call("GET","/public/config",null,null,"https://untrusted.example").status);
        String id=createBooking(2,false,today.plusDays(10));
        assertEquals(201,call("POST","/auth/register",Map.of("fullName","Other Guest","email","other@example.test","password",PASSWORD),null).status);
        assertEquals(404,call("GET","/bookings/"+id,null,login("other@example.test")).status);
        assertEquals(204,call("POST","/auth/logout",null,user).status);
        assertEquals(401,call("GET","/auth/me",null,user).status);
    }
    @Test void completeThreeGuestLifecycleRequiresBedAndReleasesInventory() throws Exception {
        assertEquals(400,call("POST","/bookings",booking(3,false,today),user).status);
        String id=createBooking(3,true,today);
        assertEquals(409,approve(id,false).status); assertEquals(200,approve(id,true).status);
        assertEquals(409,call("PATCH","/admin/bookings/"+id+"/check-in",null,admin).status);
        assertEquals(200,call("PATCH","/admin/bookings/"+id+"/extra-bed/setup",Map.of("added",true),admin).status);
        assertEquals(200,call("PATCH","/admin/bookings/"+id+"/check-in",null,admin).status);
        assertEquals(409,call("PATCH","/admin/rooms/"+ROOM+"/status",Map.of("status","MAINTENANCE"),admin).status);
        assertEquals(200,call("PATCH","/admin/bookings/"+id+"/check-out",null,admin).status);
        var detail=call("GET","/bookings/"+id,null,user).body;
        assertEquals("COMPLETED",detail.path("booking").path("status").asText());
        assertTrue(detail.path("timeline").toString().contains("CHECKED_OUT"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM bookings WHERE assigned_room_id=? AND status IN ('APPROVED','CHECKED_IN')",Integer.class,UUID.fromString(ROOM)));
    }
    @Test void concurrentApprovalsProduceOneSuccessAndOneConflict() throws Exception {
        String first=createBooking(2,false,today.plusDays(20)),second=createBooking(2,false,today.plusDays(20));
        try (var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(() -> approve(first,false)); var b=executor.submit(() -> approve(second,false));
            var codes=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS).status,b.get(30,TimeUnit.SECONDS).status));
            Collections.sort(codes); assertEquals(List.of(200,409),codes);
        }
    }
    @Test void postgresConstraintRejectsOverlapEvenWhenServiceIsBypassed() throws Exception {
        String first=createBooking(2,false,today.plusDays(25)),second=createBooking(2,false,today.plusDays(25));
        assertEquals(200,approve(first,false).status);
        assertThrows(DataIntegrityViolationException.class,() -> jdbc.update("UPDATE bookings SET status='APPROVED',assigned_room_id=? WHERE id=?",UUID.fromString(ROOM),UUID.fromString(second)));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM bookings WHERE id=?",String.class,UUID.fromString(second)));
    }
    private Map<String,Object> convention(LocalDate day,String start,String end,String booking) {
        Map<String,Object> body=new HashMap<>(Map.of("title","Workshop","purpose","Integration event","eventDate",day.toString(),"startTime",start,"endTime",end,"expectedParticipants",10));
        if (booking!=null) body.put("bookingId",booking); return body;
    }
    private String convention(LocalDate day,String start,String end) throws Exception {
        var result=call("POST","/convention-hall/requests",convention(day,start,end,null),user); assertEquals(201,result.status); return result.body.path("id").asText();
    }
    @Test void hallConflictsAdjacencyAndLinkedCancellation() throws Exception {
        LocalDate day=today.plusDays(30);
        String first=convention(day,"10:00","12:00"),second=convention(day,"11:00","13:00");
        var reason=Map.of("remark","Integration review");
        assertEquals(200,call("PATCH","/admin/convention-hall/"+first+"/approve",reason,admin).status);
        assertEquals(409,call("PATCH","/admin/convention-hall/"+second+"/approve",reason,admin).status);
        String adjacent=convention(day,"12:00","13:00");
        assertEquals(200,call("PATCH","/admin/convention-hall/"+adjacent+"/approve",reason,admin).status);
        String booking=createBooking(2,false,day.plusDays(1));
        var pending=call("POST","/convention-hall/requests",convention(day.plusDays(1),"10:00","12:00",booking),user).body.path("id").asText();
        assertEquals(200,call("PATCH","/bookings/"+booking+"/cancel",null,user).status);
        assertEquals("CANCELLED",call("GET","/facility-requests/"+pending,null,user).body.path("request").path("status").asText());
        assertEquals(409,call("PATCH","/admin/convention-hall/"+pending+"/approve",reason,admin).status);
    }
    @Test void diningPeriodsAndCustomIntervalsShareConflictProtection() throws Exception {
        LocalDate day=today.plusDays(35); var reason=Map.of("remark","Integration dining review");
        Map<String,Object> lunch=Map.of("title","Guest lunch","purpose","Hall scheduling","usageDate",day.toString(),"mealPeriod","LUNCH","numberOfGuests",8);
        var first=call("POST","/dining-hall/requests",lunch,user); assertEquals(201,first.status);
        String id=first.body.path("id").asText();
        assertEquals(200,call("PATCH","/admin/dining-hall/"+id+"/approve",reason,admin).status);
        var other=new HashMap<>(lunch); other.put("mealPeriod","OTHER"); other.put("startTime","13:00"); other.put("endTime","15:00");
        var conflicting=call("POST","/dining-hall/requests",other,user); assertEquals(201,conflicting.status);
        assertEquals(409,call("PATCH","/admin/dining-hall/"+conflicting.body.path("id").asText()+"/approve",reason,admin).status);
        assertEquals(409,call("PATCH","/admin/facility-requests/"+id+"/complete",reason,admin).status);
        assertEquals(200,call("GET","/admin/dining-hall/requests?mealPeriod=LUNCH&status=APPROVED&date="+day,null,admin).status);
    }
}
