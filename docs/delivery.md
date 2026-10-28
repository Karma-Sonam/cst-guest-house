# QA, containers and delivery

## Automated PostgreSQL tests

From PowerShell 7 with Docker Desktop running:

```powershell
Set-Location D:\GuestHouse\backend
.\run-integration-tests.ps1
```

The script creates/reuses `guesthouse_test`, passes its connection settings only to the test process, and runs thirteen unit tests plus ten full HTTP/PostgreSQL tests. The integration class refuses database names not ending in `_test` before Spring opens the datasource. Its fixtures clear only that disposable database; never point TEST_DATABASE_URL at a working database.

Coverage includes role/ownership restrictions, logout revocation and CORS, three-guest/extra-bed lifecycle, concurrent approvals, PostgreSQL rejection of a direct overlapping write, hall overlap/adjacency, linked cancellation and dining conflicts. All twenty-three tests passed together against PostgreSQL with all five migrations applied. QA also fixed and tested rejection of login passwords exceeding BCrypt's 72-byte UTF-8 limit, including multi-byte Unicode input.

CI provides its own disposable PostgreSQL service. Normal `mvnw test` without RUN_POSTGRES_TESTS=true skips these ten database tests and still runs unit tests. This opt-in prevents unexpected local database access.

## Postman

Import `postman/guesthouse.postman_collection.json` and `postman/local.postman_environment.json`. There are 55 reference requests. The environment contains no real passwords or tokens.

Set adminEmail/adminPassword privately from your local bootstrap credentials. Select the local environment and run only the “Happy path - local test data” folder against a development database with registration enabled. It generates a synthetic guest email/password, stores tokens, selects an available room, requests an extra bed, checks in/out, and exercises hall requests and cancellation. The additional-actions folder contains state-dependent reference operations, not another sequential runner flow.

Default check-in is the property-local current date so check-in can succeed; checkout and facility usage default to tomorrow. Reset guestEmail before rerunning registration, and reset dates on a later day. Do not run fixture collections against production. Collection JSON is validated locally; an actual Postman runner execution is separate from the automated Java and PowerShell suites.

## Full container stack

`compose.yaml` remains the development database used by the existing port-5173 preview. `compose.production.yaml` is a separate three-service stack with its own database volume. Only its frontend is published, on loopback port 8088 by default. Backend and PostgreSQL have no host port bindings and share an internal network. Frontend and backend run as non-root users; both have health checks. The frontend serves built assets and proxies /api through Nginx. SPA route refreshes return index.html.

For a local production-style demonstration using the existing ignored .env:

```powershell
Set-Location D:\GuestHouse
docker compose -f compose.production.yaml build
docker compose -f compose.production.yaml up -d --wait --wait-timeout 180
# Open http://127.0.0.1:8088/
docker compose -f compose.production.yaml ps
docker compose -f compose.production.yaml logs --tail 100 backend
# Stop only this stack, preserving its database volume:
docker compose -f compose.production.yaml down
```

Public registration defaults to false in this stack, independently of development REGISTRATION_ENABLED. For a local disposable smoke test only, set `$env:PRODUCTION_REGISTRATION_ENABLED='true'` before starting it; run `backend/test-web-integration.ps1 -BaseUrl http://127.0.0.1:8088`, then remove the process variable and recreate the backend to restore the default. This stack is still local and is not a public deployment.

Do not use `down -v` unless you deliberately intend to erase the selected stack's database. No reset command is part of normal startup or verification.

Local verification on 27 October 2026: both images built successfully and all three services became healthy. The PowerShell web integration suite passed through Nginx on port 8088, covering registration, login, safe profile updates, role restrictions, booking creation/cancellation, dashboard counts and arrivals/departures queries. A direct SPA route refresh returned the application and the configured security headers. Runtime user IDs were 999 for the backend and 101 for the frontend. Guest records survived a PostgreSQL restart and backend recreation, and registration was then confirmed disabled by default. This checks restart persistence, not backup restoration. The trial stack was stopped without deleting its volume; the existing development site on port 5173 and backend health remained available. Synthetic smoke-test records remain in the separate trial database volume.

## CI

Latest local container verification after the management and usability additions: both images rebuilt successfully, all three services were healthy and Flyway reached V5. `backend/test-management-web.ps1 -BaseUrl http://127.0.0.1:8088` passed user search, self-deactivation protection, reports, extended room fields, reference search and anonymous-access rejection through Nginx. Registration remained disabled by default; direct `/admin/reports` refresh and CSP checks passed. The trial stack was then stopped without deleting its data volume, and port-5173 preview/backend health were checked successfully. This script signs in and revokes its test session; it does not change user activation or room details.

`.github/workflows/ci.yml` triggers on main/develop pushes, pull requests and manual dispatch. Backend and frontend jobs run independently. Backend runs unit and real PostgreSQL integration tests; frontend uses the pinned Node/npm versions, npm ci, tests, production compilation and a high-severity dependency audit. The Docker job depends on both passing jobs, so failed tests/builds/scans prevent image builds. Test reports are retained as a GitHub artifact.

`.github/dependabot.yml` schedules npm, Maven, Docker and GitHub Actions dependency updates. Workflow action versions were checked against their official release metadata when prepared. Local workflow validation is distinct from a real hosted Actions run; no hosted success is claimed until these files are pushed and GitHub executes them.

## Production server preparation

Actual hosting, DNS and TLS credentials have not been supplied. On a user-selected Linux server, install Docker/Compose, provision an application directory, and clone this repository using its verified origin URL. Copy `.env.production.example` to an ignored `.env.production` on that server and replace the example secrets and domain. Set both ADMIN_EMAIL and ADMIN_PASSWORD for first startup; after the administrator exists, clear both and recreate the backend. Existing admin passwords are not reset by bootstrap.

Use a strong random database password and at least 32 random bytes for JWT_SECRET. Keep PRODUCTION_REGISTRATION_ENABLED=false until the college approves a registration policy. Verify hall operating hours and special-room capacities. Keep the database and API private. Put a host reverse proxy with valid HTTPS certificates in front of loopback port 8088; `deployment/nginx-https.conf.example` is a domain/certificate template, not an issued certificate or verified server configuration.

On Linux, after configuration:

```bash
docker compose --env-file .env.production -f compose.production.yaml up -d --build --wait --wait-timeout 180
curl --fail http://127.0.0.1:8088/api/public/config
```

The HTTPS domain and external access must then be verified on the actual server. Configure backups and restore procedures, host monitoring and appropriate request-rate limits before real operational use. The application does not send recovery email or provide persistent browser sign-in.

## Gated CD workflow

`.github/workflows/deploy.yml` is a manually dispatched deployment workflow. It is intentionally not automatic publication. It requires a full commit SHA that belongs to main and has a successful CI push run. The deployment job uses the GitHub environment named `production`.

Configure environment secrets DEPLOY_HOST, DEPLOY_USER, DEPLOY_SSH_KEY and pinned DEPLOY_KNOWN_HOSTS, plus the environment variable DEPLOY_PATH. The current workflow supports a DNS name/IPv4 host and the default SSH port. The SSH user must already have access to the provisioned checkout and Docker. Verify the SSH host fingerprint independently; the workflow never disables host-key checking or runs ssh-keyscan to establish trust.

The server checkout must have origin `https://github.com/<this repository>.git`, a clean tracked working tree, and its ignored .env.production. Deployment fetches the verified revision, checks it out, rebuilds the production stack, waits for health and checks the API through the frontend. It does not upload environment secrets, purchase hosting, configure DNS/certificates or erase volumes. Configure GitHub environment approvals according to the team's release policy.

Back up PostgreSQL before schema-changing releases. Code rollback uses a previously verified main revision and its corresponding images/configuration; database schema rollback requires an explicit migration/restore plan and is not automated by checkout. A server build or migration failure must be investigated; this student-project setup does not promise zero-downtime deployment.

## Remaining work

The user selected local-only operation for this stage. No public server, DNS, certificate or remote deployment is being configured. Public deployment later requires a separately supplied hosting target, domain and credentials. User activation and basic reports are now implemented; see admin-management.md. Administrator booking search is also implemented; see booking-search.md. Audited room-detail editing is implemented; see room-details.md. The local usability pass and demonstration guide are documented in local-handover.md. Property-confirmed content, college acceptance and broader device/assistive-technology coverage remain. Do not describe the project as deployed merely because local Docker and workflow checks pass.
