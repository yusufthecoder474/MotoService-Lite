# Terraform: static checks before any paid deployment

Terraform 1.9.8 is used by CI; configuration requires >=1.9,<2 and pins AzureRM 4.49.0, with the dependency lock file committed. Four resources are defined: dedicated resource group, Log Analytics workspace, Container Apps environment and Container App. There is **no AKS, database, registry, NAT gateway, Key Vault, identity, role grant or state account creation** in this configuration. Those dependencies must already exist or be separately reviewed and approved.

```bash
terraform -chdir=infra/terraform fmt -check -recursive
terraform -chdir=infra/terraform init -backend=false -input=false
terraform -chdir=infra/terraform validate -no-color
```

These need internet access for provider installation and permission to start the provider plugin, but do not require an Azure account. `validate` checks configuration/schema, not region capacity, credentials, billing, networking, image accessibility or database availability.

## Approval boundary

**No plan/apply has been authorized or executed.** The `cost_approved=false` precondition blocks plan/apply by default. This is a workflow safeguard, not an IAM control; do not change it until the owner approves the actual subscription, region, price estimate, dedicated scope and networking plan. Provider auto-registration is disabled. Public ingress defaults false; enabling it additionally requires restricted IPv4 client CIDRs (never 0.0.0.0/0).

Only after approval:

1. Copy `terraform.tfvars.example` to ignored `terraform.tfvars`, supply non-secret identifiers, reachable JDBC hostname and public SHA-tagged image. Keep the password only in Key Vault. An existing user-assigned identity needs narrowly scoped Key Vault Secrets User access, and network access to that vault. No `data` source retrieves its plaintext value.
2. Use an existing dedicated Azure Storage backend with private access/RBAC, encryption, versioning and restricted state readers. Copy `backend.hcl.example` to ignored `backend.hcl`. Grant Storage Blob Data Contributor only to the required state container; prefer Entra authentication/OIDC over storage keys. Never upload state or saved plan files.
3. Run `terraform -chdir=infra/terraform init -reconfigure -backend-config=backend.hcl`. This touches the approved backend and needs authentication; it is distinct from static init.
4. Run `terraform -chdir=infra/terraform plan -out=reviewed.tfplan` and privately inspect the plan. It must contain only the reviewed four resources, correct IDs, one app replica, no unexpected replacement/deletion, restricted ingress and correct Key Vault reference. The plan may contain sensitive data even when output hides it.
5. After approval of this exact plan, run `terraform -chdir=infra/terraform apply reviewed.tfplan`. Do not use `-auto-approve`. Validate the running application separately.

The workspace daily log quota is not a spending cap. One app replica is deliberately retained for a simple session-based demo and can incur compute charges. No minimum-cost promise is made. `ignore_changes` on the secret value prevents the provider from treating a Key Vault-managed value as a desired plaintext secret.

## Connectivity and migrations

Default internal app ingress does not give the app a private route to PostgreSQL. For private PostgreSQL, supply an approved existing ACA integration subnet, routing and DNS resolution for the database endpoint. A plain public PostgreSQL firewall needs approved stable egress; Container Apps outbound addresses can change. Do not enable broad 'all Azure services' access or an open 0.0.0.0/0 firewall as a shortcut. If a VNet/NAT/private endpoint is needed, stop and review its additional cost and topology first; this module deliberately does not create it.

The JDBC URL is credential-free and requires `sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory`, using trusted CA roots in the maintained JVM image. Test the actual server chain/hostname from the deployment network. A TLS-enabled port alone is not verification. Flyway validates and migrates at startup; clean is disabled and Hibernate only validates. Use a database-specific non-superuser owner/migrator for this small staging scope; production would separate migration and runtime identities. Back up before schema changes; application rollback cannot undo an incompatible migration.

Prefer reviewed `terraform destroy` after approval for resources managed here. The separately confirmed PowerShell resource-group cleanup is an emergency alternative; afterward reconcile Terraform state. Preserve externally managed DB/backups/state/Key Vault unless separately authorized for deletion.

References checked 30 Oct 2026: [Container App](https://registry.terraform.io/providers/hashicorp/azurerm/4.49.0/docs/resources/container_app), [Environment](https://registry.terraform.io/providers/hashicorp/azurerm/4.49.0/docs/resources/container_app_environment), [provider source documentation](https://github.com/hashicorp/terraform-provider-azurerm/blob/v4.49.0/website/docs/r/container_app.html.markdown).
