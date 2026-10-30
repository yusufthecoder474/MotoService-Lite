param([Parameter(Mandatory=$true)][string]$SubscriptionId,
      [Parameter(Mandatory=$true)][string]$Region)
$ErrorActionPreference="Stop"
Get-Command az -ErrorAction Stop | Out-Null
# Read only: no sign-in, provider registration, resource creation or subscription switch.
$account = az account show --subscription "$SubscriptionId" -o json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Sign in yourself with az login, then rerun preflight" }
if ($account.id -ne $SubscriptionId -or $account.state -ne "Enabled") { throw "Subscription mismatch or disabled" }
$locations = az account list-locations --subscription "$SubscriptionId" --query "[].name" -o tsv
if ($LASTEXITCODE -ne 0 -or $locations -notcontains $Region) { throw "Region unavailable" }
foreach ($provider in @("Microsoft.App", "Microsoft.OperationalInsights", "Microsoft.KeyVault", "Microsoft.DBforPostgreSQL")) {
  az provider show --namespace "$provider" --subscription "$SubscriptionId" --query "{provider:namespace,state:registrationState}" -o table
  if ($LASTEXITCODE -ne 0) { throw "Cannot inspect provider" }
}
az group list --subscription "$SubscriptionId" --query "[].{name:name,location:location}" -o table
if ($LASTEXITCODE -ne 0) { throw "Cannot inspect resource groups" }
Write-Host "Read-only preflight complete. IAM, budget, database routing, TLS, Key Vault access, image and explicit cost approval still required."
