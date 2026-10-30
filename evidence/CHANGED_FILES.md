# Exact changed files

Compared with base `631c5f459d62ae0b8492a0a374677bf0fb9042e1`. A = added; M = modified. No deleted files.

Application changes add Actuator/security/profile guards and tests. Deployment additions cover the non-root image, isolated verification, kind, Terraform and manual Azure update. Documentation/evidence are separate from runtime claims.

| Change | Path |
|---|---|
| M | `.dockerignore` |
| M | `.github/workflows/ci.yml` |
| A | `.github/workflows/deploy-azure.yml` |
| M | `.gitignore` |
| M | `Dockerfile` |
| M | `README.md` |
| A | `VERIFY_PLATFORM.cmd` |
| A | `compose.platform.yaml` |
| A | `deploy/k8s/app.yaml` |
| A | `deploy/k8s/configmap.yaml` |
| A | `deploy/k8s/kind-config.yaml` |
| A | `deploy/k8s/namespace.yaml` |
| A | `deploy/k8s/postgres.yaml` |
| A | `deploy/k8s/secret.example.yaml` |
| A | `docs/AGILE_DELIVERY.md` |
| A | `docs/AZURE_DEPLOYMENT.md` |
| A | `docs/INTERVIEW_AND_RESUME.md` |
| A | `docs/LOW_LEVEL_DESIGN.md` |
| A | `docs/PLATFORM_GUIDE.md` |
| A | `docs/adr/0001-container-apps-and-local-kind.md` |
| M | `docs/architecture.md` |
| A | `docs/terraform.md` |
| A | `evidence/CHANGED_FILES.md` |
| A | `evidence/PG_CLOUD_UPGRADE_VERIFICATION.md` |
| A | `evidence/hosted-run.json` |
| A | `evidence/raw/baseline-hosted.log` |
| A | `evidence/raw/first-container-failure.log` |
| A | `evidence/raw/first-terraform-failure.log` |
| A | `evidence/raw/hosted-configuration.log` |
| A | `evidence/raw/hosted-container.log` |
| A | `evidence/raw/hosted-kind.log` |
| A | `evidence/raw/hosted-terraform.log` |
| A | `evidence/raw/hosted-verify.log` |
| A | `evidence/raw/local-static.log` |
| A | `evidence/raw/local-terraform.log` |
| A | `evidence/raw/local-unit.log` |
| A | `infra/terraform/.gitignore` |
| A | `infra/terraform/.terraform.lock.hcl` |
| A | `infra/terraform/backend.hcl.example` |
| A | `infra/terraform/main.tf` |
| A | `infra/terraform/outputs.tf` |
| A | `infra/terraform/providers.tf` |
| A | `infra/terraform/terraform.tfvars.example` |
| A | `infra/terraform/variables.tf` |
| A | `infra/terraform/versions.tf` |
| M | `pom.xml` |
| A | `scripts/azure-cleanup.ps1` |
| A | `scripts/azure-preflight.ps1` |
| A | `scripts/kind-down.sh` |
| A | `scripts/kind-up.sh` |
| A | `scripts/kind-verify.sh` |
| A | `scripts/kind.ps1` |
| A | `scripts/platform-smoke.py` |
| A | `scripts/platform-verify.sh` |
| A | `scripts/staging-bootstrap.sql` |
| A | `scripts/validate-platform.py` |
| A | `scripts/verify-platform.ps1` |
| A | `src/main/java/com/motoservice/config/CloudSafetyGuard.java` |
| M | `src/main/java/com/motoservice/config/SecurityConfig.java` |
| A | `src/main/java/com/motoservice/tools/PasswordHash.java` |
| A | `src/main/resources/application-cloud.yml` |
| M | `src/main/resources/application.yml` |
| A | `src/test/java/com/motoservice/CloudSafetyGuardTest.java` |
| A | `src/test/java/com/motoservice/HealthIT.java` |

Total: **64 changed paths**. Original business service/controller/repository/entity code, migrations, 65 original tests, screenshots, `RUN_ALL.cmd`, original Compose files and automation scripts are unchanged; see `raw/local-static.log`.
