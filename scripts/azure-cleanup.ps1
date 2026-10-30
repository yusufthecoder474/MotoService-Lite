[CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
param([Parameter(Mandatory=$true)][string]$SubscriptionId,
      [Parameter(Mandatory=$true)][string]$ResourceGroup,
      [Parameter(Mandatory=$true)][string]$Confirmation)
$ErrorActionPreference="Stop"
if ($Confirmation -cne "DELETE $SubscriptionId/$ResourceGroup") { throw "Exact subscription/resource-group confirmation required" }
$group = az group show --subscription "$SubscriptionId" --name "$ResourceGroup" -o json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Resource group could not be read" }
if ($group.tags.project -ne "motoservice-lite" -or $group.tags.purpose -ne "educational-staging") { throw "Refusing untagged or unrelated group" }
az resource list --subscription "$SubscriptionId" --resource-group "$ResourceGroup" --query "[].{name:name,type:type,id:id}" -o table
if ($LASTEXITCODE -ne 0) { throw "Could not inspect deletion scope" }
if ($PSCmdlet.ShouldProcess("$SubscriptionId/$ResourceGroup", "Delete every resource in this dedicated group")) {
  az group delete --subscription "$SubscriptionId" --name "$ResourceGroup" --yes
  if ($LASTEXITCODE -ne 0) { throw "Deletion failed; inspect resources and billing" }
}
