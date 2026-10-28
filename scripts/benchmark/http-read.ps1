param([ValidateRange(1,100000)][int]$Requests = 100)
# Read-only, sequential HTTP benchmark against the running LOCAL demo.
$ErrorActionPreference = 'Stop'
$pair = 'customer1:DemoRide21!'
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair)) }
$rows = @()
for ($i=1; $i -le $Requests; $i++) {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $status = 0
    try { $r=Invoke-WebRequest 'http://localhost:18080/api/slots' -Headers $headers -UseBasicParsing; $status=[int]$r.StatusCode }
    catch { if ($_.Exception.Response) { $status=[int]$_.Exception.Response.StatusCode } }
    $timer.Stop()
    $rows += [pscustomobject]@{ request=$i; status=$status; latency_ms=$timer.Elapsed.TotalMilliseconds }
}
$out=Join-Path $PSScriptRoot '../../target/evaluation'
New-Item -ItemType Directory -Force $out | Out-Null
$rows | Export-Csv (Join-Path $out 'http-read.csv') -NoTypeInformation
$sorted=@($rows.latency_ms | Sort-Object)
$median=($sorted[[math]::Floor(($Requests-1)/2)]+$sorted[[math]::Floor($Requests/2)])/2
$p95=$sorted[[math]::Ceiling($Requests*0.95)-1]
[pscustomobject]@{total=$Requests;successful=@($rows | Where-Object status -eq 200).Count;failed=@($rows | Where-Object status -ne 200).Count;concurrency=1;median_ms=$median;p95_ms=$p95}
