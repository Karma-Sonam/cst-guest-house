# Local demonstration and handover

The system remains local-only. Start Docker Desktop, then run `D:\GuestHouse\start-local.ps1` from PowerShell 7. Open http://127.0.0.1:5173/. The helper waits for PostgreSQL, backend health and the frontend. Use the website rather than the health endpoint, which intentionally displays JSON. Logs are in `.logs`.

Use college-acceptance.md to record confirmed property information, college workflow review results, defects and the acceptance decision. The template begins unapproved. `check-local-readiness.ps1` captures a read-only technical configuration snapshot; passing those checks does not imply college sign-off.

The administrator email/password are in the ignored local `.env` file under ADMIN_EMAIL and ADMIN_PASSWORD. Enter them privately; do not put passwords in screenshots, reports, Git or shared Postman environments. Updating bootstrap settings does not reset an existing account password. The browser keeps its token in memory, so refreshing or closing a page requires signing in again.

## Suggested demonstration

1. Show the public home, rooms and facilities pages. Campus photos are references, not verified room interiors.
2. Register a synthetic guest with an `example.test` email and sign in. Check availability for the current property date through tomorrow, with three guests. Submit a standard-room request; an extra bed is required, and the selected/automatically allocated room is immediately held for the requested dates. Record the reference.
3. Use a separate browser tab/session to sign in as administrator. Find the reference under Booking requests, select a suitable available standard room, approve the extra bed and the request, then mark the bed as added. Check-in is permitted only during the reserved dates.
4. Show Current guests and the booking timeline. Check out and show the completed status and room release. Do not use a real guest record for a demonstration.
5. Show an independent convention or dining request and its administrator review. Approval is subject to overlap checks; facility approval is independent of accommodation approval.
6. Show booking search, report generation, user search and the disabled self-deactivation action. An account deactivation demonstration should use only a synthetic test account. Existing bookings are retained when an account is deactivated.
7. Open Room management → Edit details. Do not add invented amenities, photos, location or special-room capacities. Active allocations block edits. Special-room bed counts and capacity remain unverified until confirmed by the property.
8. At phone width, demonstrate the navigation menu, Escape to close, form labels, and a report. Wide tables scroll within their own area.

The sequence above is a repeatable handover checklist, not a claim that each step was repeated in this latest usability pass. Earlier full lifecycle tests and browser checks are documented separately.

## Verified engineering checks

| Area | Evidence |
| --- | --- |
| Backend | Latest pending-hold change passed 25 tests: 13 unit and 12 real PostgreSQL integration tests. All seven migrations were applied. |
| Frontend | 16 tests cover booking/auth workflows, account changes, reports, room editing, explicit labels/help text, nested password controls, Escape focus return and automatic resource updates. Production build passes. |
| Responsive browser checks | Registration and generated reports fit 320, 390 and 768 pixel viewport overrides without document-level horizontal overflow. The 390 pixel admin dashboard fits after fixing table-label overflow. Both mobile menus close with Escape and restore trigger focus. |
| Browser scope | Checks used the in-app browser on this computer. This is not an independent Firefox/Safari/device test or a formal accessibility certification. |
| Containers | Both images were rebuilt after the room/admin/accessibility changes. All three services became healthy; migration V5, management API proxy checks, closed-registration default, direct SPA route refresh and CSP passed. The trial stack was stopped with its volume retained. The development preview remains available. Rebuild again after future source changes. |
| CI/CD | Workflows validated locally; no hosted GitHub run or public deployment has been performed. |
| Local backups | Custom-format archive creation and an isolated restore rehearsal passed. Hash mismatch rejection, cleanup and working-site health were checked. See local-backups.md; this does not verify off-machine or public-server recovery. |

## Property decisions and operational work

- Confirm special-room capacities, bed counts, actual room amenities/location and official guest-house contact content.
- Confirm registration policy, administrator ownership, operating hours and guest-data retention rules.
- Review the demonstration with the college and record any acceptance changes.
- Extend browser/device and assistive-technology coverage before operational handover.
- Before public use, provide hosting/domain details and verify HTTPS, off-machine backups and a restore rehearsal on that hosting setup, monitoring and request-rate limits. A local restore rehearsal has passed; database restart persistence alone is not a backup/restore test.

No payment, billing, invoicing or food-ordering workflow is included. See `delivery.md` for test commands and local container instructions, and `admin-management.md`, `booking-search.md` and `room-details.md` for administrator features.
