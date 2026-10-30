$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$linuxRoot = (wsl wslpath -a "$root").Trim()
if ($LASTEXITCODE -ne 0) { throw "Install WSL2 Ubuntu first" }
wsl --cd "$linuxRoot" bash scripts/platform-verify.sh
exit $LASTEXITCODE
