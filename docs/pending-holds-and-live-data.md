# Pending room holds and automatic updates

## Accommodation policy

A submitted accommodation request now holds one concrete room immediately. The guest can choose a room from dated availability or let the server choose a suitable room. `POST /api/bookings` accepts an optional `roomId`; when omitted, allocation is automatic. Availability is checked again inside the submission transaction, even if the browser showed an older result.

PENDING, APPROVED and CHECKED_IN bookings block overlapping dates for their held/assigned room. Checkout dates are exclusive: an adjacent stay beginning at checkout is permitted. Approval retains the reservation; rejection or cancellation releases it. Completion also releases allocation. Reopening a rejected/cancelled future request must acquire an available room again and can fail if inventory has filled. No automatic pending-hold expiry was added: the hold remains until a decision/cancellation, as requested.

Creation locks inventory rows in a consistent order. A PostgreSQL GiST exclusion constraint independently prevents overlapping pending/approved/checked-in assignments. A second request for the same room/dates receives HTTP 409; it never creates an overlapping pending booking. New pending records must have a room ID. Serviceability, room-detail editing and capacity changes also respect pending holds.

An administrator can assign a different suitable room during approval, subject to the same conflict checks and matching category. A special room cannot be held until its guest capacity has been verified; bed counts alone do not establish capacity. If none is available, submission fails instead of creating a request without a hold.

V6 extends overlap protection and backfills eligible pre-existing pending requests. An unallocatable legacy request is preserved for explicit administrative allocation/approval or rejection. V7 enforces the room requirement for new or updated pending records without deleting any legacy request. This preserves working data; it is not an automatic acceptance/rejection of existing requests. A private local backup was created before migration.

## Live operational data

Booking lists, details, availability, dashboards, room details, reports, user lists and facility schedules read the backend. Visible pages refresh their resources every three seconds, on window focus and after successful writes. Requests do not overlap themselves, obsolete URL requests are aborted/ignored, and refreshes preserve the current view while new data arrives. Hidden pages suspend periodic refresh until visible again. This is automatic near-real-time polling, not WebSocket/SSE push or a guarantee of instant updates during a network outage. The transactional server check is authoritative for every booking submission.

Dashboard availability excludes rooms with unverified capacities and counts pending holds immediately. Public inventory counts and contact information now come from `/api/public/config`; hard-coded frontend inventory totals and contact values were removed. Configure GUESTHOUSE_PHONE, GUESTHOUSE_EMAIL and GUESTHOUSE_ADDRESS on the backend (and recreate/restart it when changing configuration). Confirmed local contact values were retained. No password, token or guest list is included in public configuration.

Per-room campus stand-in images were removed. Unprovided room photographs, descriptions, locations and amenities remain explicitly unprovided. General campus imagery/branding, explanatory text, form choices and business rules remain ordinary website content; they do not represent simulated bookings or availability. Existing stored test and user records were not deleted. Changing contact or inventory configuration is distinct from changing a booking's live status.

## Verification on 28 October 2026

- 25 backend tests passed, including overlapping pending requests, concurrent same-room submissions, release on rejection/cancellation, retained reservation on approval and adjacent-date allowance.
- 16 frontend tests passed, including selected-room submission and automatic refresh/mutation/cleanup behavior. The production frontend build passed.
- Browser submission for a clearly labelled verification request held room 101 for 10–11 February 2027. The API excluded it from availability and rejected an overlapping duplicate with 409. Rejection released it immediately, and the open guest detail page automatically changed from Pending to Rejected without navigation or refresh. Only that verification request was rejected; other user requests were retained.
- Existing local pending requests with suitable inventory received holds during migration. The development backend was repackaged and restarted.

The Docker configuration includes public contact variables. Rebuild images after source changes. Local-only operation is retained; public deployment remains deferred.
