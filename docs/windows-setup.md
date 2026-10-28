# Windows 11 / VS Code / PowerShell

Install a Java 21 JDK, Maven 3.6.3+ (3.9.x recommended), and Docker Desktop with Linux containers/WSL2. All project dependencies are free/open-source. Docker Engine is open source; Docker Desktop has its own terms and is not required if you use a local PostgreSQL server. Open a new terminal after installation. VS Code's Java Extension Pack is optional.

Run from the extracted `motoservice-lite` folder:

```powershell
java -version
javac -version
mvn -version
docker --version
docker compose version
docker info
Copy-Item .env.example .env
docker compose up -d db
docker compose ps
mvn clean package
mvn spring-boot:run
```

`mvn clean package` compiles all tests and runs unit tests; PostgreSQL integration tests run during `verify`, not `package`. Java must report major version 21. If Maven uses another JDK, set `$env:JAVA_HOME` to your actual Java 21 installation directory and prepend `"$env:JAVA_HOME\bin;"` to `$env:Path`.

Application startup automatically runs Flyway V1 before Hibernate schema validation and demo seeding. Do not run `ddl-auto=create`. On first startup, four customer accounts, an advisor, four demo motorcycles, two centers and 84 future slots are created. Startup does not reset existing data.

In a second terminal:

```powershell
Start-Process 'http://localhost:18080'
```

Local-only credentials:

| User | Password | Role |
|---|---|---|
| customer1 through customer4 | DemoRide21! | CUSTOMER |
| advisor | DemoAdvisor21! | SERVICE_ADVISOR |

Use two customer browser profiles/private windows and one advisor session for the demo. Fill a capacity-2 slot, join it from customer3, cancel customer1's booking, refresh customer3 to see promotion, then advance its service status as advisor.

## Tests

Keep Docker Desktop running. Testcontainers creates its own disposable PostgreSQL database; the Compose demo database is separate.

```powershell
# Unit tests only
mvn test
# All unit, PostgreSQL integration, security, concurrency and evaluation tests
mvn verify
# PostgreSQL booking and API/security tests
mvn '-Dit.test=BookingIT,SecurityIT' verify
# Concurrency tests
mvn '-Dit.test=ConcurrencyIT' verify
# Synthetic research evaluation; writes target/evaluation/*.csv and results.md
.\scripts\benchmark\evaluate.ps1
# Reset synthetic data only (dedicated/disposable test database)
.\scripts\benchmark\evaluate.ps1 -ResetOnly
# Read-only HTTP latency sample against running demo app, concurrency=1
.\scripts\benchmark\http-read.ps1 -Requests 100
```

If PowerShell blocks local scripts, inspect them first and run their documented Maven equivalent, e.g. `mvn '-Dit.test=EvaluationIT#evaluateSyntheticWorkloads' verify`. No execution-policy change is required.

## Without Docker

Install PostgreSQL 16 locally. Create `motoservice` for the demo and **a separate** `motoservice_test` for tests. A password-authenticated database user must own each database and be able to create its schema. Use psql interactively (passwords are prompted):

```powershell
psql -U postgres -h localhost
```

Inside psql, for a fresh local installation:

```sql
CREATE USER motoservice WITH PASSWORD 'local-demo-only';
CREATE DATABASE motoservice OWNER motoservice;
CREATE DATABASE motoservice_test OWNER motoservice;
\q
```

Then in PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/motoservice'
$env:DB_USER = 'motoservice'
$env:DB_PASSWORD = 'local-demo-only'
mvn spring-boot:run
# In the test terminal:
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/motoservice_test'
$env:TEST_DB_USER = 'motoservice'
$env:TEST_DB_PASSWORD = 'local-demo-only'
mvn verify
```

**Test database is reset before each test. Never use a valuable database named motoservice_test.** The URL guard rejects other database names and URL query parameters. Unset test overrides to return to Testcontainers:

```powershell
Remove-Item Env:TEST_DB_URL -ErrorAction SilentlyContinue
Remove-Item Env:TEST_DB_USER -ErrorAction SilentlyContinue
Remove-Item Env:TEST_DB_PASSWORD -ErrorAction SilentlyContinue
```

## Fully containerized app option

```powershell
docker compose --profile full up --build -d
Start-Process 'http://localhost:18080'
docker compose logs app
```

## Stop

For `mvn spring-boot:run`, press **Ctrl+C** in its terminal. Then:

```powershell
docker compose --profile full down
```

This preserves the database volume. To deliberately reset only this project's demo database (all its records are lost), stop first, then `docker compose --profile full down -v` and restart. Do not use that command for the research reset: the evaluation harness has a separate test database.

## Troubleshooting

Port 55432/18080 occupied: stop the conflicting local service or change the Compose host port and matching DB_URL. A changed `.env` password does not change an already initialized PostgreSQL volume. Maven does not automatically load `.env`; the defaults match the sample, or set `$env:DB_PASSWORD` explicitly. A Docker daemon error means Testcontainers did not execute database scenarios. Do not call that a passing integration run. Demo slots age after 14 days; retain useful data, or deliberately reset the demo database to regenerate them.

## NEW: Complete, one-click verification and launch (Docker Desktop only)

The project includes `RUN_ALL.cmd`. Once Docker Desktop is installed and running,
**double-click `RUN_ALL.cmd` from inside the `motoservice-lite` folder**.
The launcher performs all of these operations automatically:

1. Checks the Docker engine and attempts to start Docker Desktop if found.
2. Starts a **separate, disposable** PostgreSQL `motoservice_test` container.
3. Runs `mvn clean verify` inside `maven:3.9.11-eclipse-temurin-21` — Java and Maven
   do **not** need to be installed on your Windows host.
4. Saves full Maven test reports and actual synthetic CSV evaluation under `target/`.
5. Builds and starts the real application and demo PostgreSQL database.
6. Uses a separate Python container to make real authenticated HTTP requests:
   customer1/customer2 booking → customer3 FIFO waitlist → cancellation → promotion
   → advisor starts and completes service → persisted event history.
7. Saves `artifacts/http-smoke.json` (only after a successful smoke run), copies
   evaluation markdown into `artifacts/`, opens `http://localhost:18080/`, and
   leaves the application running.

Every run creates `artifacts/full-verification-YYYYMMDD-HHMMSS.log`. If any step
fails, the launcher stops with an error and does not produce a fake success marker.
`artifacts/LOCAL_VERIFICATION.txt` is created only after all three stages pass.

**Important:** The smoke test uses *fictional demo customers* and consumes one
previously unused appointment. It does not reset your demo database. The Maven
verification tests use a **different database** (a PostgreSQL tmpfs instance);
they do not alter the demo database.

The full project source may have been generated on another machine, but the
one-click launcher itself was not executed in the original restricted environment.
Treat generated output as **NOT EXECUTED** until it runs successfully on your machine.

### Equivalent commands (if you prefer terminal)

```powershell
docker compose -f compose.yaml -f compose.verify.yaml run --rm verify
docker compose --profile full up --build -d db app
docker compose -f compose.yaml -f compose.verify.yaml run --rm --no-deps smoke
```
