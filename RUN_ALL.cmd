@echo off
setlocal
cd /d "%~dp0"
echo MotoService Lite - HTTP Smoke Array Fix (v6)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\automation\RUN_ALL.ps1"
set "RESULT=%ERRORLEVEL%"
echo.
if not "%RESULT%"=="0" (
 echo VERIFICATION/STARTUP DID NOT COMPLETE. Check artifacts\ log and docker compose logs --tail=100 app.
) else (
 echo Automated verification and HTTP smoke completed successfully.
)
echo.
pause
exit /b %RESULT%
