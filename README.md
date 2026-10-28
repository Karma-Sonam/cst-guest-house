# College Guest House Booking and Management System

Development workspace for the CST guest house system.

Source repository: https://github.com/Karma-Sonam/cst-guest-house

## Current status

The implementation is on the `develop` branch. The original prototype is preserved in `CST-GuestHouse/`, and its original README is retained in `docs/original-repository-README.md`. The Spring Boot backend and React frontend are implemented for the main accommodation and facility workflows.

Open the website at **http://127.0.0.1:5173/**. Start local services with `.\start-local.ps1` from this directory. See [frontend setup and verification](docs/frontend.md).

Stage 3 foundation is implemented and verified: Spring Boot authentication, revocable JWT sessions, role protection, initial PostgreSQL migration and local database Compose configuration. See [backend setup](docs/backend-foundation.md).

Stage 4 accommodation APIs are implemented and verified: availability, requests, admin allocation, extra beds, check-in/out, status history and room controls. See [booking backend and test commands](docs/booking-backend.md).

Stage 5 facility APIs are implemented and verified: convention/dining requests, admin approval, overlap prevention, cancellation, completion and history. See [facility API and test commands](docs/facility-backend.md).

Stage 6 frontend includes public pages, login/registration, guest and admin dashboards, bookings, rooms, facilities, profile and operational screens. Production builds, fourteen focused frontend tests and live proxy/browser workflows have been verified.

Stage 7 QA adds an isolated PostgreSQL integration suite: all twenty-three backend tests pass, including concurrency and database constraint checks. Both Docker images build, and the separate three-container stack passed health, API proxy, SPA refresh, security-header and database restart persistence checks. The Postman collection contains 55 reference requests. CI and gated deployment workflows are prepared and pass local Actionlint validation; a real GitHub run has not yet been performed. See [QA and delivery instructions](docs/delivery.md).

Hosting remains **local only**, as requested. The original development preview stays on port 5173; the separate production-style container configuration defaults to loopback port 8088.

See [Windows setup notes](docs/local-setup.md). Java 21.0.2 runs successfully.

See [the implementation blueprint](docs/implementation-blueprint.md) for scope, database design, API contract, screens, and milestones.

## Planned stack

React, TypeScript, Vite, Tailwind CSS, React Router, Axios; Spring Boot, Java 21, Spring Security, JPA; PostgreSQL; Docker Compose and GitHub Actions.

No billing, payments, invoices, or food ordering.

Stage 8 adds administrator user search and activation controls with session revocation, audit history and administrator protection, plus date-range accommodation/facility reports. See [admin management instructions](docs/admin-management.md).

Stage 9 adds administrator booking search by reference, guest name or email, combined with status filtering and pagination. See [booking search](docs/booking-search.md).

Stage 10 adds audited room-detail editing with verified bed counts, room numbers, location, descriptions and amenities, plus active-allocation safeguards. See [room detail editing](docs/room-details.md).

Stage 11 improves explicit form labels and help text, confirmation dialog names, mobile menu Escape/focus handling and table overflow on narrow screens. See [local demonstration and handover](docs/local-handover.md) for verified checks, demo steps and remaining property decisions.

Stage 12 adds private local PostgreSQL backups and isolated restore verification. Archive creation, restore, checksum rejection, temporary-database cleanup and working-site health checks passed. See [local backup instructions](docs/local-backups.md).

Stage 13 refreshes the Docker images and validates the latest management features through the three-container stack. The Postman collection now has 55 reference requests. Local-only operation is retained; permanent public deployment remains deferred.

Stage 14 prepares the [college acceptance review](docs/college-acceptance.md), with property-confirmation fields, 15 review scenarios and a decision record. The read-only local readiness script passed website, access, inventory and standard-room rule checks; college approval remains pending.

Pending-room policy update (28 October 2026): submitted requests immediately hold a concrete room and block overlapping pending/approved/checked-in requests. Operational data refreshes automatically every three seconds and after writes. Public inventory/contact values come from the backend, and per-room stand-in photos have been removed. Verified with 25 backend and 16 frontend tests plus a live browser/API hold/release check. See [pending holds and live data](docs/pending-holds-and-live-data.md). The latest source is included on the develop branch.
