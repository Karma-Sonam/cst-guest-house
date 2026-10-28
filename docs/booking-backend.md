# Accommodation backend

## Implemented scope

Authenticated room catalogue and date-based standard-room search; one-room booking requests; owner-only booking detail and paginated lists; administrative review, rejection, approval with room assignment, reassignment, remarks and eligible reopening; extra-bed decisions and physical setup; check-in and checkout; room serviceability and special-room capacity configuration.

Java sources are under `D:\GuestHouse\backend\src\main\java\bt\edu\cst\guesthouse`, in controller, dto, entity, repository and service packages. Schema changes are versioned in `backend/src/main/resources/db/migration/V2__accommodation_bookings.sql` and `V3__room_audit.sql`.

## API usage

All routes require a bearer token. Administrative routes require ADMIN. User tokens can only access their own bookings; another user's identifier returns 404. Lists return content, totalElements, page and size; size is bounded to 100.

| Method | Path | Purpose |
|---|---|---|
| GET | /api/rooms | Catalogue without guest identities |
| GET | /api/rooms/{id} | Room details |
| GET | /api/rooms/available?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&guests=2 | Available standard rooms |
| POST | /api/bookings | Create pending request |
| GET | /api/bookings/my?page=0&size=20 | Own requests |
| GET | /api/bookings/{id} | Booking and timeline |
| PATCH | /api/bookings/{id}/cancel | USER pending cancellation; ADMIN may also cancel future approved stay |
| GET | /api/admin/bookings?status=PENDING&page=0&size=20 | Review list |
| GET | /api/admin/bookings/{id} | Review details |
| PATCH | /api/admin/bookings/{id}/approve | Atomic approval and allocation |
| PATCH | /api/admin/bookings/{id}/assign-room | Reassign approved booking |
| PATCH | /api/admin/bookings/{id}/reject | Reject pending request, with remark |
| PATCH | /api/admin/bookings/{id}/remarks | Remark with history event |
| PATCH | /api/admin/bookings/{id}/reopen | Rejected/cancelled valid stay → pending; allocation cleared |
| PATCH | /api/admin/bookings/{id}/extra-bed | Extra-bed decision |
| PATCH | /api/admin/bookings/{id}/extra-bed/setup | Record bed physically added/removed |
| PATCH | /api/admin/bookings/{id}/check-in | Record arrival during reserved dates |
| PATCH | /api/admin/bookings/{id}/check-out | Checkout and completion in one transaction |
| GET | /api/admin/rooms | Administrative inventory |
| PATCH | /api/admin/rooms/{id}/status | ACTIVE, MAINTENANCE or UNAVAILABLE |
| PUT | /api/admin/rooms/{id}/capacity | Configure verified SPECIAL room capacity; null/null clears unverified values |

Booking request example:

```json
{
  "fullName": "Example Guest",
  "email": "guest@example.test",
  "phone": "",
  "organization": "College",
  "designation": "Visitor",
  "guestCategory": "GENERAL_GUEST",
  "purpose": "Official visit",
  "guests": 3,
  "checkIn": "2026-11-01",
  "checkOut": "2026-11-03",
  "preferredCategory": "STANDARD",
  "extraBedRequested": true,
  "remark": "Please arrange an extra bed."
}
```

Use valid future dates when running examples. Categories are STANDARD/SPECIAL; guest categories are GENERAL_GUEST, COLLEGE_STAFF_GUEST, OFFICIAL_GUEST, VIP_GUEST, MINISTER, VICE_CHANCELLOR, SENIOR_OFFICIAL and OTHER_DISTINGUISHED_GUEST.

Approval/reassignment body: `{"roomId":"ROOM_UUID","approveExtraBed":true,"remark":"Approved official visit"}`. Rejection, remarks and reopening: `{"remark":"Reason"}`. Bed decision: `{"approved":true,"remark":"Bed approved"}`. Bed setup: `{"added":true}`. Room status: `{"status":"MAINTENANCE"}`. Capacity: `{"normalCapacity":2,"maxCapacity":2}` using verified property values only.

## Consistency rules

Dates use Asia/Thimphu. Stay intervals are [arrival, departure), so same-day changeovers do not overlap. Pending requests do not reserve rooms. Approval locks the request and target room, checks availability and capacity, and writes the approval and timeline transactionally. A PostgreSQL GiST exclusion constraint independently forbids overlapping APPROVED/CHECKED_IN reservations, including concurrent writes bypassing application checks.

The first release stores the single current room directly on the booking rather than duplicating it in a room_allocations table from the initial blueprint. The exclusion constraint uses this same authoritative record. Reassignment is recorded in the timeline. A separate structured allocation-history table can be introduced if multi-room bookings are added.

Room serviceability is independent of reservation dates. Changing a room to maintenance/unavailable is blocked while it has approved/current allocations. A delayed checkout blocks a new physical check-in even if the reserved dates have ended.

Standard capacity is two, or three only with an approved extra bed. Three-person check-in also requires physical bed setup. Capacity cannot be reduced by rejecting a required extra bed after approval. Special capacity starts unset and allocations fail until an administrator configures verified values. Initial one-room requests are limited to three guests, including special requests. Only ADMIN allocates special rooms; choosing a VIP category grants no additional permissions.

Lifecycle changes and reassignment have an actor/time/remark history. Checkout records CHECKED_OUT then COMPLETED atomically, keeping only COMPLETED as the persisted terminal status. Completed records cannot be reopened. Room status and special-capacity changes have audit-log entries. No room deletion endpoint exists.

## Run and test

Use PowerShell 7. Start the database and backend as described in `backend-foundation.md`. Ensure the ignored root .env contains local ADMIN_EMAIL and ADMIN_PASSWORD for bootstrap; do not commit them. The smoke script reads them without displaying them.

```powershell
Set-Location D:\GuestHouse\backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
# Stop this project's running backend before packaging on Windows.
.\mvnw.cmd package
.\run-local.ps1
# In a second PowerShell terminal:
Set-Location D:\GuestHouse\backend
.\test-api.ps1
.\test-bookings.ps1
```

The scripts create uniquely named local test users and audited test bookings. Booking checks include simultaneous approvals, adjacent dates, owner access, role protection, capacity/bed rules, checkout, cancellation, reopening, maintenance and VIP allocation. VIP checks temporarily set synthetic local capacity and clear it afterwards; test capacity is not real guest-house information. Do not run these scripts against production.

## Remaining work

Verification on 27 October 2026: packaging succeeded; all seven unit tests passed; Flyway V2 and V3 applied to the local PostgreSQL database and JPA schema validation passed. Authentication smoke checks and the expanded booking smoke suite passed. The live suite verified simultaneous approvals returning one 200 and one 409, adjacent-date availability, ownership/role restrictions, extra-bed setup and capacity enforcement, check-in/out, room release, cancellation/reopening/rejection, maintenance protection and explicit VIP allocation. Temporary special-room capacity was cleared after testing. `git diff --check` passed.

Facility requests and schedules are the next milestone. Dashboard summaries, arrival/departure views, richer search filters, user management, full room metadata editing and frontend screens follow. Automated PostgreSQL integration tests for CI will supplement the repeatable PowerShell live checks. Explicit lock-timeout/deadlock error formatting, rate limiting and session cleanup remain hardening tasks.
