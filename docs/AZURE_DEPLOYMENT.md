# Azure deployment runbook — NOT DEPLOYED

No subscription, tenant permissions, budget, cloud database or endpoint has been verified. No Azure resource was created. The steps below are a gated procedure, not execution evidence. Region-specific cost estimate: **unavailable**; obtain a current official calculator estimate before approval. Budget alerts are **not a hard spending cap**.

## 1. Read-only preflight and explicit approval

Sign in yourself with `az login` (never paste passwords/tokens into chat), then run:

```powershell
./scripts/azure-preflight.ps1 -SubscriptionId YOUR_ID -Region APPROVED_REGION
```

The script reads subscription state, regions, provider registration and resource groups. It does not log in, register providers or create resources. Portal navigation: search **Subscriptions** → select intended subscription → Overview (confirm subscription/tenant) → **Access control (IAM)** → View my access. Verify a narrowly scoped deployment identity rather than assuming Owner. Search **Cost Management + Billing** → select subscription scope → **Budgets** → Add → monthly budget amount/date range → configure actual and forecast alerts, e.g. 50/80/100%, and your email. Verify the alert destination and current spend. Obtain the owner's approval of subscription, region, app/log/database/network/storage estimates and teardown date before continuing.

Prepare a network drawing and price estimate at [Azure pricing calculator](https://azure.microsoft.com/pricing/calculator/). Include PostgreSQL compute/storage/backups, retained ACA replica, Log Analytics, Key Vault, optional private networking/NAT and Terraform state storage. Free allocations/credits are not assumed. Database stopping may not eliminate storage charges or remain stopped indefinitely.

## 2. Approved dependencies (these actions can incur charges)

In the subscription → **Resource providers**, register `Microsoft.App` and `Microsoft.OperationalInsights`; register `Microsoft.KeyVault`, `Microsoft.ManagedIdentity`, `Microsoft.Storage` and `Microsoft.DBforPostgreSQL` only as needed for the approved dependencies. Or use `az provider register --namespace PROVIDER --subscription ID` after approval.

Portal → **Resource groups** → Create → choose approved subscription/region → dedicated dependency group. The Terraform module creates its own dedicated app group; do not create the same app resources manually and then apply Terraform over them. Portal guidance below can be used to inspect the resulting resources or as a separately approved manual alternative; Terraform ownership then requires deliberate import.

Portal → **Azure Database for PostgreSQL flexible servers** → Create → confirm subscription/group/region → Development/Burstable sizing after checking availability and price → no HA for this educational scope unless specifically approved → choose authentication → set a strong private admin secret using a password manager. Do not use a demo password. Under Networking choose the reviewed private/VNet topology or a strictly limited approved public firewall. Private DNS, routing and ACA integration must match. Do not select broad Azure-service access. Review compute, storage, backup retention and total cost before Create.

From an authorized client/network, create a separate `motoservice` database and a non-superuser staging login restricted to it; enter credentials interactively. Grant schema creation/migration rights only within that database. Never point integration tests at staging: the tests truncate a dedicated `motoservice_test` database. Validate TLS hostname and the server's current trusted root chain with the chosen Java image. Do not disable verification to get connectivity working.

Portal → **Managed identities** → Create a user-assigned identity in the approved scope. Portal → **Key vaults** → select/create approved vault → Secrets → Generate/Import → `db-password`, enter privately. Use RBAC; IAM → Add role assignment → **Key Vault Secrets User** → select the app's managed identity, narrowed to the required secret where supported. Vault networking must permit the app's approved route. Save the secret URI and identity resource ID, never the secret value, into Terraform inputs. Secret rotation can require app revision restart; verify behavior rather than assuming immediate uptake.

## 3. Build and publish the verified image

Build only a clean commit that passed the CI workflow. The image job tests locally; it intentionally does not publish automatically. On an authorized workstation, sign in to GHCR using your credential manager or `--password-stdin`, never a token in command history. Example after verifying the SHA:

```bash
mvn -B verify
sha=$(git rev-parse HEAD)
docker build --build-arg VCS_REF="$sha" -t "ghcr.io/yusufthecoder474/motoservice-lite:$sha" .
docker push "ghcr.io/yusufthecoder474/motoservice-lite:$sha"
```

GitHub profile → Packages → package settings → confirm intended visibility and repository association. This Terraform example expects a **publicly pullable** image; do not silently add registry passwords. Record the pushed digest and source SHA, and verify anonymous pull before deploying. Image publication has not been performed as part of this upgrade. No SBOM/vulnerability scan result is claimed.

## 4. Provision after reviewed Terraform plan

Follow [terraform.md](terraform.md) for protected backend, exact inputs, plan and apply approval. Portal → **Container Apps Environments** → inspect the new environment → Networking (approved subnet, if used) and Logs (workspace). Portal → **Container Apps** → app → Identity (assigned identity) → Secrets (Key Vault reference) → Containers (SHA image, cloud profile, DB URL/user, secret reference) → Ingress (8080, HTTPS only, internal by default). Public demonstration requires separate authorization and restricted IP allowlisting; published demo accounts must never work.

For a manual Portal alternative, Create Container App → Basics → approved group/name/environment → Container → approved image, target port 8080, 0.5 CPU/1Gi, cloud profile and DB env references → Scaling → one replica → Health probes → startup/liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness`, port 8080. Assign the identity and Key Vault secret first. Review every resource and cost before creation. No successful deployment can be claimed if PostgreSQL or the image is unreachable.

## 5. Private staging authentication and data

The cloud profile starts without demo seeding. A fresh database can migrate and become healthy with no users; a successful probe is therefore **not** a successful application demonstration. Provision four private synthetic accounts and slots through the dedicated database using `scripts/staging-bootstrap.sql`. It inserts data transactionally and refuses published demo usernames, never truncates data, and fails on duplicate account names rather than overwriting passwords. Inspect the database target before running it. Do not use the bootstrap against real customer data.

Generate one distinct strong password per account using a password manager. The offline helper asks invisibly in a terminal and prints only its BCrypt hash:

```bash
docker run --rm -it --entrypoint java IMAGE   -Dloader.main=com.motoservice.tools.PasswordHash -cp /app/app.jar   org.springframework.boot.loader.launch.PropertiesLauncher
# Then, from an approved DB client/network, with password entered interactively:
PSQL_HISTORY=/dev/null psql "host=APPROVED_HOST dbname=motoservice user=APPROVED_ROLE sslmode=verify-full" -W -f scripts/staging-bootstrap.sql
```

Use current PostgreSQL trusted CA configuration for psql. Keep hashes/passwords out of logs and commits. The bootstrap creates `staging-c1`, `staging-c2`, `staging-c3`, `staging-advisor`, motorcycles and ten future capacity-two slots. Keep the passwords in protected GitHub environment secrets for synthetic verification only. Each smoke consumes one unused slot; replenish approved synthetic slots deliberately when exhausted. No production identity lifecycle, MFA or user administration is implemented.

## 6. Manual OIDC deployment workflow

In GitHub → repository Settings → Environments → New environment `azure-staging` → required reviewers/deployment branches. Confirm these protections are available and enabled for the repository/plan; do not treat the YAML environment name as proof of protection. Set environment variable `AZURE_COST_APPROVED=true` only after approval. Add variables `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, `AZURE_APP_NAME`, `STAGING_BASE_URL` (HTTPS). Add the eight `SMOKE_*_USER/PASSWORD` secrets named in `deploy-azure.yml`.

Entra admin center → App registrations → selected deployment application → Certificates & secrets → Federated credentials → Add credential → GitHub Actions → organization `yusufthecoder474`, repo `MotoService-Lite`, entity **Environment**, value `azure-staging`. Subject must be `repo:yusufthecoder474/MotoService-Lite:environment:azure-staging`, audience `api://AzureADTokenExchange`. Grant only the app-update/read rights needed on the dedicated app scope; keep role-assignment/admin rights outside the workflow. Review wildcard roles carefully: some Container Apps roles can retrieve secrets. No client secret is needed. The app managed identity for Key Vault is separate from the deployment identity.

Workflow dispatch becomes available normally after the workflow is on the default branch; merging requires review and is not performed by this task. Actions → **Approved Azure staging update** → Run workflow → full successful CI SHA and `APPROVE-STAGING-COSTS`. It checks CI success, uses OIDC, updates the existing app image only, checks both probes, then runs an authenticated synthetic booking/promotion/completion flow. It never provisions infrastructure. The runner must reach the approved endpoint: internal-only ingress needs a suitably isolated authorized runner/network; public CIDR ingress must explicitly allow the chosen runner's stable egress. GitHub-hosted egress is not implicitly allowlisted. Do not widen the firewall to make CI pass.

If verification fails, retain logs and mark deployment FAIL. Use the last proven image SHA in the same approved manual workflow to roll back the app, then reverify. Single-revision mode simplifies this portfolio but may interrupt sessions. Schema rollback needs its own backup/compatibility plan; never assume changing the image reverses Flyway.

## 7. Evidence, logs and cleanup

Portal → app → Monitoring → Metrics (requests/replicas/CPU/memory) and Log stream; Environment/Log Analytics → Logs for retained logs. Do not print environment secret values or use `listSecrets` in evidence. Record UTC timestamp, app resource ID, source SHA/image digest, ingress policy, health and authenticated synthetic outcome, actual URL only if publication was approved, and sanitized screenshots with no personal/billing info. An assigned FQDN alone is not live evidence.

After the demo, inventory dedicated resources and preserve any requested data/backups. Prefer approved `terraform destroy` for Terraform-owned resources. Alternative:

```powershell
./scripts/azure-cleanup.ps1 -SubscriptionId ID -ResourceGroup rg-motoservice-staging -Confirmation "DELETE ID/rg-motoservice-staging" -WhatIf
# Inspect the list. Then repeat without -WhatIf and answer the deletion confirmation.
```

The script checks dedicated project tags and exact scope confirmation. It deletes **all resources** in that group only after confirmation. It does not delete the separate database, identity, vault, backend, backups or registry. Inspect those separately and seek approval before deletion; inspect Cost Management afterward for residual charges. Reconcile Terraform state after manual deletion. Never report cleanup complete until the actual resource inventory confirms it.

## Official references (checked 30 Oct 2026)

- [Container Apps deployment](https://learn.microsoft.com/en-us/azure/container-apps/containerapp-up): `up` can create additional resources, so this runbook uses reviewed Terraform and image-only updates.
- [Secrets and Key Vault](https://learn.microsoft.com/en-us/azure/container-apps/manage-secrets)
- [GitHub OIDC authentication](https://learn.microsoft.com/en-us/azure/developer/github/connect-from-azure)
- [PostgreSQL overview](https://learn.microsoft.com/en-us/azure/postgresql/overview)
- [PostgreSQL TLS](https://learn.microsoft.com/en-us/azure/postgresql/security/security-tls)
- [kind quick start](https://kind.sigs.k8s.io/docs/user/quick-start)
- [Spring Boot 3.5 endpoints](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html)
