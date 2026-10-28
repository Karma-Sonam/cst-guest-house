# React website

## Open the site

Website: http://127.0.0.1:5173/

The API remains on port 8080. Its health JSON is a diagnostic endpoint; use port 5173 for the website.

From PowerShell 7:

```powershell
Set-Location D:\GuestHouse
.\start-local.ps1
```

This starts the project's PostgreSQL container and any missing backend/frontend processes in the background, waits for readiness, and prints the website address. Docker Desktop must be running. Logs are under the ignored `.logs/` directory. Existing healthy servers are reused.

Use the registration page to create a guest account when registration is enabled. The local administrator was provisioned from ADMIN_EMAIL/ADMIN_PASSWORD in the ignored root `.env`; no production credentials or default password are in source. Browser tests created synthetic accounts and audited test requests in the local database.

## Stack and files

React, TypeScript, Vite, Tailwind CSS, React Router, Axios and Lucide icons. Exact resolved dependencies are pinned in frontend/package.json and package-lock.json. Node 24 LTS (24.15 or newer within major 24) is required; `.nvmrc` records the verified 24.22.1 runtime. The current machine's system Node 23 remains untouched. Local scripts use the available bundled Node 24 runtime, or NODE24_HOME when set.

```text
frontend/
  public/images/              Original logo and campus references
  src/
    components/Layout.tsx     Public, guest and admin shells; protected routes
    components/ui.tsx         Forms, badges, dialogs, tables and timelines
    context/AuthContext.tsx   Memory-only session and expiry handling
    hooks/useResource.ts      Loading, errors, retries and cancelled reads
    services/api.ts           Axios authentication and API errors
    pages/PublicPages.tsx     Home, rooms, facilities, about and contact
    pages/AuthPages.tsx       Login and configurable registration
    pages/UserPages.tsx       Dashboard, availability, bookings and profile
    pages/FacilityPages.tsx   Facility requests and admin review
    pages/AdminPages.tsx      Admin overview, rooms, requests and movements
    main.tsx                  Route definitions
    styles.css                Responsive institutional design
    workflows.test.tsx        Important form and route regression tests
  vite.config.ts              Fixed preview port and /api backend proxy
  run-local.ps1               Node 24 development/build/test launcher
```

Images supplied by the original prototype show the campus and college entrance, not verified room interiors. Public accommodation cards identify these as campus references. Replace them with verified room photos when available. Contact details and special-room capacity remain explicitly unverified rather than invented.

## Screens implemented

Public: landing page with availability search, rooms overview, facilities, about, contact, sign-in, registration, 403 and 404.

Guest: dashboard with real totals/recent bookings, availability results, room details, accommodation request, confirmation/detail/timeline, status-filtered/paginated bookings, convention/dining requests, facility detail/history, profile editing and logout.

Admin: real dashboard counts, pending requests, booking review and allocation, extra-bed decisions/setup, reassignment, rejection, reopening, check-in/out, remarks, room serviceability, special-room capacity controls, convention/dining schedules and decisions, current guests, selected-date arrivals/departures and profile editing.

Changes are sent to the existing APIs, not saved as simulated browser-only bookings. Availability is rechecked on approval. Larger parties are directed to separate one-room requests. Hall requests have separate approvals and can be linked after accommodation submission.

## Session and network behavior

JWT access tokens stay in memory. Refreshing/closing the page requires sign-in again. Expired sessions return to sign-in; the API still enforces roles and ownership. Logging out revokes the server session and clears local state. This initial implementation does not have refresh tokens or password recovery email.

Vite proxies /api to 127.0.0.1:8080. FRONTEND_URL supports a comma-separated explicit origin allowlist; local configuration permits http://127.0.0.1:5173 and http://localhost:5173. Production must use the actual deployed origin. No wildcard origin or weakened authentication was introduced.

Supporting backend endpoints added for the UI:

- GET /api/public/config: registration availability.
- GET/PUT /api/users/profile: permitted profile fields; email/role remain protected.
- GET /api/bookings/summary: authenticated user's booking totals.
- GET /api/bookings/my?status=: optional own-booking status filter.
- GET /api/admin/dashboard/summary: inventory, current occupancy and request counts.
- GET /api/admin/current-guests: paginated checked-in stays.
- GET /api/admin/arrivals-departures?date=YYYY-MM-DD&arrivals=true|false: approved arrivals or checked-in departures for a date.

## Development commands

Install Node 24 LTS before using standard npm commands on another machine. Use npm 12.2.1, matching packageManager, to avoid the dependency resolver crash observed with this machine's older npm.

```powershell
Set-Location D:\GuestHouse\frontend
npm.cmd ci
.\run-local.ps1 dev
# Separate terminal, with the dev server left running:
.\run-local.ps1 test
.\run-local.ps1 build
```

The root starter expects the backend JAR to exist. If backend code changes, stop this project's backend before packaging on Windows, build with the Maven wrapper, then rerun the root starter. Database startup and backend commands are documented in backend-foundation.md.

API integration checks through the website's proxy:

```powershell
Set-Location D:\GuestHouse\backend
.\test-web-integration.ps1
```

This script uses browser-style Origin headers and checks registration, profile protection, user/admin totals, own-status filtering and arrivals/departures. It creates synthetic local test data. Do not run smoke scripts against production.

## Verification and remaining work

Verified on 27 October 2026: production frontend compilation and all seven frontend tests pass with Node 24. Backend packaging and all twelve backend unit tests pass. Proxy-level integration checks with browser Origin headers pass. Browser checks verified mobile navigation, registration/login, a three-guest request, linked convention request, admin review, extra-bed setup, check-in/out and facility rejection. Screenshots are saved under screenshots/. The mobile homepage had equal document and viewport widths, with no horizontal overflow. A registration-to-login state bug was fixed and covered by a regression test; cleared/changed dates are also covered. `git diff --check` passed. The preview was left on the public homepage with its temporary viewport override cleared.

This is the main frontend milestone, not a production deployment. Remaining project work includes broader accessibility/browser coverage, CI-ready PostgreSQL integration tests, Postman collection, optional user activation/reporting interfaces, richer administrative search, full room metadata editing, production container/proxy configuration, CI/CD and deployment. Basic hall schedules use a filtered list rather than a calendar grid. Email recovery and persistent sign-in are not presented as working features.
