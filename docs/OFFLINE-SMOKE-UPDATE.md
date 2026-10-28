# Final offline HTTP smoke workflow

The previous launcher attempted to pull `python:3.12-alpine` for the HTTP smoke test, causing a Docker Hub DNS error. The new launcher runs `scripts/automation/smoke_http.ps1` with Windows PowerShell 5.1 after the existing Docker-based Maven and PostgreSQL verification and app startup. No extra Python Docker image or Python installation is needed.

Run `RUN_ALL.cmd` on Windows with Docker Desktop running. App URL: http://localhost:18080/ . PostgreSQL host port: 55432.

If the smoke test passes, results go to `artifacts/http-smoke.json`. The test uses synthetic accounts and data and consumes a free seeded capacity-2 slot, so repeat runs may require new sample slots. The launcher does NOT reset/remove existing persistent database volumes.

Evidence status: the user's Oct 28, 2026 log shows 31 unit/MVC and 34 integration tests passed, and app/db containers started. This modified PowerShell HTTP smoke client has NOT been executed here (Windows/Docker unavailable).
