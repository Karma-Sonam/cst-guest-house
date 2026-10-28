# User accounts and booking reports

Sign in as an administrator at http://127.0.0.1:5173/login. The administration sidebar now includes **User accounts** and **Reports**.

## User accounts

Search by name or email. Results are paginated and contain safe profile fields only. Activate or deactivate an account using the confirmation panel. Deactivation prevents sign-in and deletes existing sessions. Reactivation requires a fresh sign-in; previous tokens remain invalid. Existing accommodation and facility records are retained and are not automatically cancelled.

Administrators cannot deactivate their own account, and the last active administrator is protected. Activation decisions are serialized within a database transaction to protect concurrent administrator changes. Actual state changes create audit entries. This screen does not create administrators or change roles/passwords.

API: `GET /api/admin/users?search=&page=0&size=10` and `PATCH /api/admin/users/{id}/active` with `{ "active": false }`. Page size is bounded at 100; search length at 120 characters. Both routes require an active administrator session.

## Reports

Choose a start date (included) and end date (excluded), covering 1–366 days, then generate the report. Accommodation status counts include stays overlapping the range. Facility counts use event dates within the range. All status counts reflect the current status, rather than a historical status snapshot.

Reserved room nights count scheduled nights clipped to the chosen range for APPROVED, CHECKED_IN and COMPLETED accommodation requests. They are not actual occupied nights, unique guest counts or an occupancy percentage. Cancelled and rejected stays contribute no reserved nights. The report uses a consistent database snapshot.

API: `GET /api/admin/reports/summary?from=2026-10-01&to=2026-11-01`. No billing or revenue reporting is included.

## Verification

The full backend suite passed 21 tests (13 unit and 8 real PostgreSQL integration tests). The additions cover safe account listing, guest access denial, deactivation/login/session revocation, reactivation without old-token reuse, self-deactivation protection, validation, audit entries, report date bounds and checkout-exclusive night counting. Nine frontend tests passed, including confirmation before an account change and selected report date submission; the production frontend build also passed.

The running local site was checked in the browser: administrator sign-in, the new sidebar links, paginated account listing, name/email search, disabled self-deactivation and report generation all worked. The report preview is saved at `screenshots/admin-reports.png`. Existing local test records are retained. Run the Docker build again before using the separate container stack with these new features; the development backend has already been repackaged and restarted.
