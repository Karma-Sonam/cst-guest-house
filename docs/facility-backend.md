# Facility scheduling backend

## Scope and structure

Convention hall and dining hall usage requests can be independent or linked to an accommodation booking owned by the requester. Neither module includes food ordering, billing or payments.

The two request types share one scheduling table to enforce interval conflicts consistently. Sources under `backend/src/main/java/bt/edu/cst/guesthouse/` include FacilityController, AdminFacilityController, FacilityService, FacilityRules, dedicated DTOs, entities and repositories. Flyway migration `backend/src/main/resources/db/migration/V4__facility_scheduling.sql` creates requests and actor/time/status history.

## API contract

All endpoints require a bearer token. Owner records are private; another user's identifier returns 404. Administrative endpoints require ADMIN.

| Method | Path | Purpose |
|---|---|---|
| POST | /api/convention-hall/requests | Submit a pending convention request |
| GET | /api/convention-hall/my | Own convention requests |
| POST | /api/dining-hall/requests | Submit a pending dining request |
| GET | /api/dining-hall/my | Own dining requests |
| GET | /api/dining-hall/periods | Configured named meal windows |
| GET | /api/facility-requests/{id} | Own request/admin detail and timeline |
| PATCH | /api/facility-requests/{id}/cancel | Owner pending cancellation; admin may also cancel approved future usage |
| GET | /api/admin/convention-hall/requests | Convention requests/schedule |
| GET | /api/admin/dining-hall/requests | Dining requests/schedule |
| PATCH | /api/admin/convention-hall/{id}/approve | Reserve the convention hall |
| PATCH | /api/admin/convention-hall/{id}/reject | Reject a pending request |
| PATCH | /api/admin/dining-hall/{id}/approve | Reserve the dining hall |
| PATCH | /api/admin/dining-hall/{id}/reject | Reject a pending request |
| PATCH | /api/admin/facility-requests/{id}/complete | Complete approved usage after its end |

Lists accept status, date (YYYY-MM-DD), page and size (1–100). Dining lists also accept mealPeriod (BREAKFAST/LUNCH/DINNER/OTHER). Response: content, totalElements, page and size. Pending requests include schedulingConflict to flag overlapping approved reservations without exposing other users' identities.

Convention request:

```json
{
  "bookingId": null,
  "title": "College workshop",
  "purpose": "Official workshop",
  "eventDate": "2026-12-01",
  "startTime": "10:00",
  "endTime": "12:00",
  "expectedParticipants": 20,
  "remark": "Projector setup requested"
}
```

Dining request:

```json
{
  "bookingId": null,
  "title": "Official visitors",
  "purpose": "Dining hall usage",
  "usageDate": "2026-12-01",
  "mealPeriod": "LUNCH",
  "numberOfGuests": 10,
  "remark": "Official visit"
}
```

OTHER dining requests additionally require startTime/endTime. Named periods use server-configured windows; a client cannot silently override them. Decisions, cancellation and completion require `{"remark":"Reason for action"}`. Use future dates when running examples.

## Scheduling and lifecycle

All event dates/times use Asia/Thimphu. Start must be in the future and end must follow start on the same date. Overnight intervals are outside this release. Participants must be positive; physical hall capacity is not invented or presented as verified.

Pending requests do not reserve space. Approval locks the request and hall, rechecks conflicts and writes the decision/history transactionally. A PostgreSQL GiST exclusion constraint independently prevents overlapping approved intervals for the same hall. Intervals are [start,end), so one event can begin exactly when another ends. Dining usage is exclusive per window in this release; OTHER requests compete against named periods too.

State transitions: PENDING → APPROVED/REJECTED/CANCELLED; APPROVED → CANCELLED before usage by ADMIN, or COMPLETED after usage. Invalid repeated transitions return 409. USER may only cancel PENDING. Completion cannot be used to release a future reservation early.

Linking requires an owned PENDING/APPROVED/CHECKED_IN accommodation booking. Approval revalidates the linked booking under lock. Cancelling accommodation cancels linked pending facility requests in the same transaction and records history; already-approved facility reservations remain approved for explicit administrative review. A cancelled accommodation request cannot receive new linked facility requests. Room approval and facility approval are separate decisions.

Lock ordering is linked booking → request → facility for approval. Request state is refreshed after lock acquisition, preventing approval based on an outdated pending state after a concurrent cancellation.

## Dining configuration

Development defaults: breakfast 07:00–09:00, lunch 12:00–14:00, dinner 18:00–20:00. These are provisional scheduling defaults, not verified guest-house operating hours. Set DINING_BREAKFAST_START/END, DINING_LUNCH_START/END and DINING_DINNER_START/END in the ignored .env, then restart the backend. Existing requests retain their recorded windows. Production hours and shared-use policy need property confirmation.

## Run and verify

Use PowerShell 7 and the startup instructions in backend-foundation.md. Stop this project's backend before rebuilding its JAR on Windows.

```powershell
Set-Location D:\GuestHouse\backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd package
.\run-local.ps1
# In a second PowerShell terminal:
Set-Location D:\GuestHouse\backend
.\test-facilities.ps1
```

The smoke script reads local admin credentials from the ignored .env, creates uniquely named test users, and leaves audited test requests. Run only against a development database. Expected output is PASS for each check and a final success message.

Unit tests cover schedule validation and successful/early completion. The live suite checks ownership, roles, overlapping/adjacent schedules, simultaneous approval, meal windows, custom dining conflicts, rejection/cancellation, linked accommodation cancellation, timelines and list filters. Successful completion after elapsed usage is verified at the service unit-test level; future completion denial is checked through HTTP.

## Remaining scope

Verified on 27 October 2026: Maven packaging succeeded and all twelve unit tests passed. Flyway V4 applied to PostgreSQL and JPA schema validation passed. The complete live facility smoke suite and booking regression suite passed, including concurrent hall approval (one success, one 409), overlap/adjacency, dining conflicts, ownership and role boundaries, linked cancellation/history, and admin date/status/meal filters. A nullable-filter SQL error found during verification was fixed using dynamic JPA predicates. `git diff --check` passed. No frontend pages are claimed as implemented in this milestone.

React forms, calendars, admin schedules and visual conflict flags are next. Facility request editing/rescheduling, hall capacity configuration and recurring events are outside this initial scheduling milestone. Production time windows must be verified before deployment.
