# Verification v6 — HTTP smoke array parsing fix

The Windows log for 2026-10-28 recorded **31 passing unit/MVC tests**, **34 passing PostgreSQL integration/security/concurrency/evaluation tests**, a valid **57.6 MB executable Spring Boot JAR**, and an HTTP 200 response from `/login`. The smoke test then failed while casting a PowerShell `Object[]` to `Int64` at the selected slot ID.

This release fixes the **PowerShell 5.1 HTTP JSON array conversion and slot selection**, preserving the Java backend and database. It does not assert that the new smoke test has passed; rerun `RUN_ALL.cmd` and check `artifacts/http-smoke.json` for a fresh PASS.

Docker application port: `http://127.0.0.1:18080` (host). Database host port: `55432`.

If no free 2-capacity slot is present, the smoke test stops without deleting or resetting existing demo data.
