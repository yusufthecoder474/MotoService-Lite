@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\verify-platform.ps1"
exit /b %ERRORLEVEL%
