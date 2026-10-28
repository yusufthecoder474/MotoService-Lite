# One-click Windows verification. Compatible with Windows PowerShell 5.1 (ASCII source).
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location $root
New-Item -ItemType Directory -Force -Path 'artifacts' | Out-Null
# Never let an earlier passing run be mistaken for the current run.
Remove-Item 'artifacts/LOCAL_VERIFICATION.txt', 'artifacts/http-smoke.json' -Force -ErrorAction SilentlyContinue
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$log = Join-Path $root "artifacts\full-verification-$stamp.log"
Start-Transcript -Path $log | Out-Null
function Test-DockerReady {
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'SilentlyContinue'
        & docker info --format '{{.ServerVersion}}' *> $null
        return ($LASTEXITCODE -eq 0)
    } catch { return $false }
    finally { $ErrorActionPreference = $previous }
}
function Run-Checked([string]$Label, [string[]]$Arguments) {
    Write-Host "`n=== $Label ===" -ForegroundColor Cyan
    $previous = $ErrorActionPreference
    try {
        # Docker routinely writes ordinary progress messages to stderr on PowerShell 5.
        # Check the real native-process exit code instead of treating every stderr line as failure.
        $ErrorActionPreference = 'Continue'
        & docker @Arguments
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
    if ($code -ne 0) { throw "$Label failed (Docker exit code $code). See $log" }
}
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker is not installed or is missing from PATH. Install Docker Desktop with Linux containers first.'
    }
    if (-not (Test-DockerReady)) {
        $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
        if (Test-Path $desktop) {
            Write-Host 'Starting Docker Desktop...' -ForegroundColor Yellow
            Start-Process -FilePath $desktop | Out-Null
            for ($attempt=0; $attempt -lt 45; $attempt++) {
                Start-Sleep -Seconds 2
                if (Test-DockerReady) { break }
            }
        }
    }
    if (-not (Test-DockerReady)) { throw 'Docker engine is not ready. Start Docker Desktop and enable Linux containers/WSL2.' }
    Run-Checked 'Validate Docker Compose YAML files' @('compose','-f','compose.yaml','-f','compose.verify.yaml','config','--quiet')
    Run-Checked 'Compile, unit, API, PostgreSQL, concurrency and synthetic evaluation tests' @(
        'compose','-f','compose.yaml','-f','compose.verify.yaml','run','--rm','verify')
    Write-Host 'Maven verify exited successfully; details are in target\surefire-reports and target\failsafe-reports.' -ForegroundColor Green
    if (Test-Path 'target/evaluation/results.md') {
        Copy-Item -Force 'target/evaluation/results.md' (Join-Path $root 'artifacts/evaluation-results.md')
    }
    # Confirm that Maven produced a real, executable Spring Boot ZIP/JAR archive.
    # BUILD SUCCESS alone does not guarantee that an older cached Docker layer has a valid JAR.
    Write-Host "`n=== Validate Maven-built executable JAR ===" -ForegroundColor Cyan
    $jarPath = Join-Path $root 'target/motoservice-lite-1.0.0.jar'
    if (-not (Test-Path $jarPath)) { throw "Maven verification succeeded but executable JAR is missing: $jarPath" }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jarArchive = $null
    try {
        $jarArchive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
        $names = @($jarArchive.Entries | ForEach-Object { $_.FullName })
        if (-not ($names -contains 'META-INF/MANIFEST.MF')) { throw 'JAR missing manifest' }
        if (-not ($names -contains 'org/springframework/boot/loader/launch/JarLauncher.class')) { throw 'JAR missing Spring Boot launcher; Maven package may not be executable' }
        if (-not ($names | Where-Object { $_ -like 'BOOT-INF/classes/*' } | Select-Object -First 1)) { throw 'JAR missing application classes' }
        if (-not ($names | Where-Object { $_ -like 'BOOT-INF/lib/*' } | Select-Object -First 1)) { throw 'JAR missing dependencies' }
    } finally { if ($null -ne $jarArchive) { $jarArchive.Dispose() } }
    Write-Host "Verified executable Spring Boot JAR: $([math]::Round((Get-Item $jarPath).Length / 1MB, 1)) MB" -ForegroundColor Green
    # Recreate app container so old, corrupted app.jar is not reused.
    Run-Checked 'Build/start real database and running web application' @(
        'compose','--profile','full','up','--build','--force-recreate','-d','db','app')
    # The smoke test retries HTTP startup internally, then exercises a real multi-user booking flow.
    Write-Host "`n=== HTTP end-to-end: authenticated booking, FIFO promotion, advisor completion (built-in PowerShell, no image pull) ===" -ForegroundColor Cyan
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'scripts/automation/smoke_http.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Real HTTP smoke test failed; see log for details.' }
    $result = "Verified locally on $(Get-Date -Format o) with Docker. Full Maven verify and HTTP smoke: exit code 0. Detailed reports: target/ and $log."
    Set-Content -Path 'artifacts/LOCAL_VERIFICATION.txt' -Value $result -Encoding utf8
    Write-Host "`nSUCCESS - Opening http://localhost:18080/" -ForegroundColor Green
    Start-Process 'http://localhost:18080/'
} catch {
    Write-Host "`nFAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Do not claim PostgreSQL tests or the running application passed unless logs prove it.'
    exit 1
} finally {
    Stop-Transcript | Out-Null
}
