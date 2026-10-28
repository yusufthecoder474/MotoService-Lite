param([switch]$ResetOnly)
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '../..')
try {
    if ($ResetOnly) { mvn '-Dit.test=EvaluationIT#resetOnly' verify }
    else { mvn '-Dit.test=EvaluationIT#evaluateSyntheticWorkloads' verify }
    if ($LASTEXITCODE -ne 0) { throw 'Evaluation failed. Inspect target/failsafe-reports.' }
    if (-not $ResetOnly) { Get-Content target/evaluation/results.md }
} finally { Pop-Location }
