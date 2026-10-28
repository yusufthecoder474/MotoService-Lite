> **Supplement (one-click upgrade):** See [RELEASE-AUDIT.md](RELEASE-AUDIT.md) for the new Docker-only full verification and live HTTP smoke workflow. Historical results below still describe the *original* execution environment; they do not turn new or unexecuted tests into passes.

# Verification report — 28 October 2026

## Outcome

**Source delivered and Maven package build VERIFIED. Full PostgreSQL-backed acceptance is incomplete.** Do not describe this release as fully verified or production-ready.

| Check | Result | Evidence |
|---|---|---|
| Java 21 compile, all production/test source | VERIFIED | Successful Maven package log |
| Spring Boot executable JAR packaging | VERIFIED | Maven repackage completed; generated JAR excluded from source ZIP |
| Unit/MVC tests | VERIFIED — 31 passed, 0 assertion failures, 0 errors, 0 skipped | evidence/unit/*.txt and build-package.log |
| Actual Thymeleaf template rendering | VERIFIED with mocked service fixtures | WebRenderingTest (4 cases) |
| HTTP error/CSRF/role filter contract | VERIFIED in MVC slice | ApiContractTest (7 cases) |
| PostgreSQL smoke attempt | FAILED — 1 setup error, 0 assertion failures, 0 skipped | evidence/postgres-attempt.log and failsafe summary |
| Database migration, seeding, real login and booking | NOT EXECUTED | PostgreSQL context could not start |
| PostgreSQL FIFO, rollback, capacity/concurrency assertions | NOT EXECUTED | Source compiled; database unavailable |
| Research experiments and performance measurements | NOT EXECUTED | Runners supplied; no observational CSV generated |
| Docker image/Compose startup | NOT EXECUTED | Docker unavailable |
| Running-app screenshots | NOT EXECUTED | No simulated screenshots supplied |
| GitHub Actions run | NOT EXECUTED | Workflow supplied, no observed hosted run |
| PowerShell commands/scripts on Windows | NOT EXECUTED | Written for PowerShell, reviewed statically on Linux |

## Environment

Initially installed: OpenJDK 17.0.20; no Maven, Docker or psql. A local toolchain was downloaded for verification: **OpenJDK 21.0.12+8**, **Maven 3.9.11**, **Spring Boot 3.5.16** resolved from Maven Central. Host reports Linux 6.18.55 x86_64. No usable PostgreSQL server was available. Downloaded PostgreSQL 16.15 binaries could not initialize as root; switching to an unprivileged user was unavailable in this environment. This is an infrastructure limitation, not evidence about application database behavior. Docker/Testcontainers image target is postgres:16; its actual runtime patch version must be recorded when executed.

Spring's [3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) listed 3.5.16 under stable releases and Java 17–25 compatibility. Java 21 compilation and dependency resolution were actually completed. The requested 3.5 line is retained; no claim is made that it is the latest Spring Boot major line or suitable for unreviewed internet deployment.

## Executed commands and counts

The workspace used a local Maven/Java path and a temporary network proxy configuration. The equivalent portable commands are:

```powershell
mvn package
mvn '-Dit.test=BookingIT#successfulBookingAndHistoryPersist' verify
```

The final package run executed **31 tests, 0 failures, 0 errors, 0 skipped**, and returned BUILD SUCCESS. The database smoke invocation occurred earlier, when 24 unit/MVC tests existed; those 24 passed again, then the single selected database method produced **1 error** while loading its context: `Could not find a valid Docker environment`. No database assertion was reached. Later source additions were HTTP/MVC tests and response/view refinements; all database source compiled in the final package run. The whole `mvn verify` suite has **not** passed here.

Repeated unit passes are not added together as distinct test coverage. No test was disabled to make the package build succeed. `package` and `verify` are deliberately different Maven lifecycle stages, documented in README and CI.

## Failure found and fixed

The initial 20-case unit run had 17 passes and 3 Mockito initialization errors: the default inline mock maker could not self-attach to the restricted JVM. The tests only mock interfaces and non-final classes, so the supported `mock-maker-subclass` extension was selected in `src/test/resources/mockito-extensions/`. The same service assertions then passed. This changes the mocking mechanism, not the tested business rules. The failure excerpt is preserved in evidence/initial-unit-attempt.txt.

## Acceptance status

All requested core workflows have source implementations: login, garage, persisted availability, booking, waitlist, cancellation/promotion, advisor status and history, REST, validation and role checks. Unit/MVC evidence validates only its stated scope. Database-backed startup, migrations, seeded-user login, SQL constraints, actual lock behavior, atomic recovery and the synthetic business hypotheses remain mandatory follow-up validations before calling the project complete.

See windows-setup.md for exact commands and test-matrix.md for the case inventory. `mvn verify` on a Java 21 machine with Docker is the next decisive verification step. If it fails, fix and rerun; never replace this report's NOT EXECUTED labels with expected outcomes.
