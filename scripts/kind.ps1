param([ValidateSet("up","verify","down")][string]$Action="verify", [switch]$ConfirmDelete)
$ErrorActionPreference="Stop"
$root = Split-Path $PSScriptRoot -Parent
$linuxRoot = (wsl wslpath -a "$root").Trim()
if ($LASTEXITCODE -ne 0) { throw "WSL2 Ubuntu is required" }
if ($Action -eq "down") {
  if (!$ConfirmDelete) { throw "Pass -ConfirmDelete to erase only motoservice-local" }
  wsl --cd "$linuxRoot" bash scripts/kind-down.sh DELETE-motoservice-local
} else { wsl --cd "$linuxRoot" bash "scripts/kind-$Action.sh" }
exit $LASTEXITCODE
