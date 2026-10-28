# End-to-end real HTTP smoke, using Windows PowerShell 5.1 only.
# No Python installation, Docker Hub Python image, or external module required.
$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:18080'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "Smoke assertion failed: $Message" }
}
function New-User([string]$Name, [string]$Password) {
    $raw = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${Name}:${Password}"))
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $auth = @{ Authorization = "Basic $raw" }
    $csrf = Invoke-RestMethod -Method Get -Uri "$base/api/csrf" -WebSession $session -Headers $auth -TimeoutSec 20
    Assert-True ([bool]$csrf.token) "Missing CSRF token for $Name"
    $auth[$csrf.headerName] = [string]$csrf.token
    $bikes = Invoke-RestMethod -Method Get -Uri "$base/api/motorcycles" -WebSession $session -Headers $auth -TimeoutSec 20
    $flatBikes = New-Object 'System.Collections.Generic.List[object]'
    foreach ($outer in @($bikes)) {
        foreach ($bike in @($outer)) {
            if ($null -ne $bike -and $null -ne $bike.id) { $flatBikes.Add($bike) }
        }
    }
    return @{ name=$Name; webSession=$session; headers=$auth; bikes=$flatBikes.ToArray() }
}
function Call-Api($User, [string]$Method, [string]$Path, $Payload=$null) {
    $parameters = @{ Method=$Method; Uri="$base$Path"; WebSession=$User.webSession; Headers=$User.headers; TimeoutSec=30 }
    if ($null -ne $Payload) {
        $parameters['Body'] = ConvertTo-Json -InputObject $Payload -Depth 8 -Compress
        $parameters['ContentType'] = 'application/json'
    }
    try {
        # Parse the HTTP JSON body explicitly. Windows PowerShell 5.1 can
        # preserve a JSON array as one nested Object[] across function calls.
        $parameters['UseBasicParsing'] = $true
        $response = Invoke-WebRequest @parameters
        if ([string]::IsNullOrWhiteSpace($response.Content)) { return $null }
        return (ConvertFrom-Json -InputObject $response.Content)
    }
    catch {
        $detail = $_.Exception.Message
        if ($_.Exception.Response) { $detail = "$detail (HTTP $([int]$_.Exception.Response.StatusCode))" }
        throw "$Method $Path failed: $detail"
    }
}
Write-Host 'Waiting for Spring Boot HTTP server on 127.0.0.1:18080 ...' -ForegroundColor Cyan
$up = $false
for ($i=0; $i -lt 60; $i++) {
    try {
        $page = Invoke-WebRequest -Uri "$base/login" -UseBasicParsing -TimeoutSec 3
        if ($page.StatusCode -eq 200) { $up = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}
Assert-True $up 'Spring Boot login page did not become ready'
Write-Host 'Live login page: HTTP 200' -ForegroundColor Green
$c1 = New-User 'customer1' 'DemoRide21!'
$c2 = New-User 'customer2' 'DemoRide21!'
$c3 = New-User 'customer3' 'DemoRide21!'
$advisor = New-User 'advisor' 'DemoAdvisor21!'
Assert-True ($c2.bikes.Count -gt 0 -and $c3.bikes.Count -gt 0) 'Seeded customers 2 and 3 need motorcycles'
$registration = 'SMK' + [Guid]::NewGuid().ToString('N').Substring(0,9).ToUpperInvariant()
$newBike = Call-Api $c1 'POST' '/api/motorcycles' @{registration=$registration; model='Fictional Roadster'; note='Synthetic real HTTP smoke'}
Assert-True ($newBike.registration -eq $registration) 'Motorcycle registration mismatch'
# Convert a possible nested PowerShell Object[] into individual slot records.
$rawSlots = Call-Api $c1 'GET' '/api/slots'
$slots = New-Object 'System.Collections.Generic.List[object]'
foreach ($outer in @($rawSlots)) {
    foreach ($slot in @($outer)) {
        if ($null -ne $slot -and $null -ne $slot.id) { $slots.Add($slot) }
    }
}
Write-Host "Service slots returned: $($slots.Count)" -ForegroundColor Cyan
# Avoid reserved slots: require BOTH spaces free, or use a fresh MotoService demo data volume.
$matching = @($slots.ToArray() | Where-Object { $_.capacity -eq 2 -and $_.available -eq 2 })
Assert-True ($matching.Count -gt 0) 'No empty 2-capacity slot. Demo data may be used up; do not delete project volumes automatically.'
$selected = $matching[0]
$slotId = [long]($selected.id)
Write-Host "Selected free slot ID: $slotId" -ForegroundColor Cyan
function BookingPayload($BikeId) { return @{motorcycleId=[long]$BikeId; slotId=$slotId; serviceType='REGULAR_SERVICE'; note='Automated synthetic smoke'} }
$a = Call-Api $c1 'POST' '/api/bookings' (BookingPayload $newBike.id)
$b = Call-Api $c2 'POST' '/api/bookings' (BookingPayload $c2.bikes[0].id)
Assert-True ($a.status -eq 'CONFIRMED' -and $b.status -eq 'CONFIRMED') 'Two reservations must be confirmed'
$w = Call-Api $c3 'POST' '/api/waitlist' (BookingPayload $c3.bikes[0].id)
Assert-True ($w.status -eq 'WAITING') 'Third customer should be waitlisted'
$full = Call-Api $c1 'GET' "/api/slots/$slotId"
Assert-True ($full.available -eq 0) 'Slot must be fully booked'
$cancelled = Call-Api $c1 'POST' "/api/bookings/$($a.id)/cancel"
$wait = @(Call-Api $c3 'GET' '/api/waitlist')
$promoted = $wait | Where-Object { $_.id -eq $w.id } | Select-Object -First 1
Assert-True ($null -ne $promoted -and $promoted.status -eq 'PROMOTED' -and $null -ne $promoted.bookingId) 'FIFO waitlist promotion missing'
$promotedId = [long]$promoted.bookingId
$inService = Call-Api $advisor 'PATCH' "/api/advisor/bookings/$promotedId/status" @{status='IN_SERVICE'}
Assert-True ($inService.status -eq 'IN_SERVICE') 'Advisor cannot start service'
$completed = Call-Api $advisor 'PATCH' "/api/advisor/bookings/$promotedId/status" @{status='COMPLETED'}
Assert-True ($completed.status -eq 'COMPLETED' -and @($completed.history).Count -eq 3) 'Service completion/history mismatch'
$history = @(Call-Api $c3 'GET' '/api/bookings')
$actual = $history | Where-Object { $_.id -eq $promotedId } | Select-Object -First 1
Assert-True ($null -ne $actual -and $actual.status -eq 'COMPLETED') 'Customer history missing completed promoted booking'
$occupancy = Call-Api $c2 'GET' "/api/slots/$slotId"
Assert-True ($occupancy.available -eq 0) 'Promotion must fill cancelled capacity'
$result = [ordered]@{status='PASS';kind='REAL_HTTP_POSTGRESQL_SMOKE';registered_motorcycle=$registration;slot=$slotId;capacity=2;confirmed=2;waitlist_promoted_to=$promotedId;advisor_status='COMPLETED';service_history_events=@($completed.history).Count;test_client='Windows PowerShell 5.1'}
$out = Join-Path $root 'artifacts/http-smoke.json'
$result | ConvertTo-Json -Depth 6 | Set-Content -Path $out -Encoding UTF8
Write-Host 'PASS: Real HTTP booking, cancellation, FIFO promotion, and advisor completion.' -ForegroundColor Green
Write-Host "Evidence: $out"
