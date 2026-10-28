# Release audit — full-implementation verification gap

## Original issue

The earlier archive implemented core workflows but reported only 31 executed
unit/MVC tests. Its Testcontainers run failed **before any database assertion**
because Docker was not accessible. Therefore the implementation was not fully
verified, even though the PostgreSQL integration and concurrency test source
was present. This distinction matters: compiling and testing mocks cannot prove
transaction boundaries, row locks, Flyway migration compatibility, real login,
waitlist ordering, or HTTP behavior with PostgreSQL.

## Added in this release

- `compose.verify.yaml` — a Docker-only verification stack. A Maven container
  compiles and runs the **entire** Maven `verify` lifecycle against its own
  disposable PostgreSQL test database, using the guarded `TEST_DB_URL` variable.
- `RUN_ALL.cmd` and `scripts/automation/RUN_ALL.ps1` — one-click Windows launcher
  that checks Docker, runs Maven verification, starts the production-shaped
  local demo, performs the real-network smoke test, and opens the browser.
- `scripts/automation/smoke_http.py` — stdlib-only HTTP end-to-end flow covering
  four demo roles/users, two bookings, a full slot, FIFO waitlisting, cancellation,
  automatic promotion, advisor status changes and persisted service-event history.
- Automatic output locations: `target/surefire-reports`,
  `target/failsafe-reports`, `target/evaluation/*.csv`,
  `artifacts/full-verification-*.log`, and (only after successful smoke)
  `artifacts/http-smoke.json` and `artifacts/LOCAL_VERIFICATION.txt`.

## Verification status at packaging time

| Item | Status | Basis |
|---|---|---|
| Historical Java 21 package | PREVIOUSLY VERIFIED | Original bundled build logs |
| Historical 31 unit/MVC tests | PREVIOUSLY VERIFIED | Original bundled Surefire logs |
| Java 21 on current container | VERIFIED | `java -version` |
| Python HTTP smoke script syntax | VERIFIED | `python -m py_compile` |
| New Docker Compose YAML structure | STATIC VALIDATION | Python YAML parser |
| New PowerShell launcher | NOT EXECUTED | PowerShell unavailable in current container |
| Maven `verify` after enhancements | NOT EXECUTED | No Maven/dependency cache and no network access |
| PostgreSQL Flyway/real endpoints | NOT EXECUTED | No PostgreSQL/Docker engine |
| Concurrency and research experiments | NOT EXECUTED | Requires PostgreSQL |
| Real HTTP smoke script against Spring Boot | NOT EXECUTED | Requires running Spring Boot + PostgreSQL |
| Running-app screenshots | NOT EXECUTED | Requires running application/browser |
| Hosted GitHub Actions | NOT EXECUTED | No repository workflow run observed |

**No 90+ ATS score, production-ready status, cloud deployment, or measured
business benefit is claimed here.** A full successful local run is needed before
using database or concurrency results as resume evidence.

## Acceptance decision

Source features: implemented in Java. Executed production-shaped system
acceptance: **PENDING**. Report the exact failed command/log if any stage of
`RUN_ALL.cmd` fails and fix the actual runtime issue before claiming completion.
