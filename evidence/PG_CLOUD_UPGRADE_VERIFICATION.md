# Cloud/platform verification ledger

Verified 30 Oct 2026, 09:55 IST (04:25 UTC). **CLOUD DEPLOYMENT NOT EXECUTED.**

- Original base: `631c5f459d62ae0b8492a0a374677bf0fb9042e1`.
- Verified code/workflow SHA: `414d06c743961f943513c41633255ad0c6ea34df` on `feature/pg-cloud-platform`.
- [Final hosted run 41002535727](https://github.com/yusufthecoder474/MotoService-Lite/actions/runs/41002535727): **SUCCESS**, all five jobs; completed 2026-10-30 04:25:50 UTC. Structured result: [hosted-run.json](hosted-run.json).
- [Draft PR #2](https://github.com/yusufthecoder474/MotoService-Lite/pull/2), [tracking issue #1](https://github.com/yusufthecoder474/MotoService-Lite/issues/1). `main` remains unchanged. No external reviewer approval or merge is claimed.
- The later evidence/README-only commit uses `[skip ci]`; it changes no application, workflow, Docker, Kubernetes, Terraform or script code. The code SHA above is the exact revision exercised, not a claim that a documentation-only commit ran tests again.

## Verification matrix

Hosted platform for final jobs: GitHub Ubuntu 24.04.5 amd64; Java 21 for Maven, PostgreSQL 16 containers. Every raw log includes timestamped commands and checkout information. Local checks ran on the restricted Linux amd64 workspace. Tests are counted once, not added across retries.

| Claim / scope | Status | Command and timestamp (UTC) | Source / raw evidence |
|---|---|---|---|
| Fresh original baseline | PASS | `mvn -B verify`; 29 Oct 16:44; 31 unit/MVC + 34 PostgreSQL IT = **65**, zero failure/error/skip | Base SHA; [baseline-hosted.log](raw/baseline-hosted.log); [baseline rerun](https://github.com/yusufthecoder474/MotoService-Lite/actions/runs/40733043251), job 123590569597 |
| Final complete test suite | PASS | `mvn -B verify`; 30 Oct 04:23:30; **36 unit/MVC + 38 PostgreSQL IT = 74**, zero failure/error/skip | Verified SHA; [hosted-verify.log](raw/hosted-verify.log), job 123890008375 |
| Capacity, transactional FIFO, ownership, role and CSRF regressions | PASS | Original BookingIT 14, SecurityIT 13, ConcurrencyIT 5, EvaluationIT 2; included in total above, not extra tests | Same verify log; original test files unchanged |
| Cloud-safety and probe regressions | PASS | CloudSafetyGuardTest 5; HealthIT 4; exact anonymous status-only responses, readiness 503 while liveness 200, denied management routes, CSRF preserved | Same verify log; no real Azure/TLS-server handshake claimed |
| Prebuilt JAR → non-root SHA image | PASS | `bash scripts/platform-verify.sh --verified-jar` invokes Docker build using the JAR artifact from successful Maven job; 30 Oct 04:24 | Verified SHA; [hosted-container.log](raw/hosted-container.log), job 123890346261 |
| Isolated Compose HTTP business workflow | PASS | Same command; 04:24:20; register motorcycle, two confirmed bookings, waitlist/cancel/promotion, advisor completion/history, negative role/CSRF checks | Same container log; private loopback port 18082; synthetic disposable DB |
| Container restart and persistence | PASS | `docker compose ... restart app`; reauthenticated completed promoted booking/history check; 04:24:27 | Same container log; app-only restart, not DB disaster recovery |
| Local kind deployment/runtime | PASS | `bash scripts/kind-up.sh` then `bash scripts/kind-verify.sh`; 04:24–04:25; app+DB Ready, real HTTP workflow, controlled app rollout, persisted history, no current-container restart loops | Verified SHA; [hosted-kind.log](raw/hosted-kind.log), job 123890346314; executed locally on GitHub runner, not Azure Kubernetes |
| Disposable runtime cleanup | PASS | Compose trap removed its unique project/volume; kind-down confirmation deleted named cluster at 04:25:47 | Container/kind logs; no user demo DB or cloud resources touched |
| Terraform formatting and schema validation | PASS | Terraform 1.9.8; `fmt -check -recursive`, `init -backend=false -input=false`, `validate -no-color`; AzureRM 4.49.0; 04:22:39 | Verified SHA; [hosted-terraform.log](raw/hosted-terraform.log), job 123890008362; no Azure auth/provisioning |
| Kubernetes static schemas | PASS | kubeconform 0.6.7, strict Kubernetes 1.32 schema; **7 resources valid, zero invalid/error/skip**; 04:22:39 | [hosted-configuration.log](raw/hosted-configuration.log), job 123890008221 |
| Config/credential-pattern/shell/PowerShell syntax checks | PASS | `python3 scripts/validate-platform.py`, `bash -n`, PowerShell AST parsing; 04:22:37–39 | Same configuration log; focused checks, not complete security scanning or Windows runtime execution |
| Local JAR build/unit suite | PASS | `mvn -B package` via local Java 21/Maven proxy wrapper; 30 Oct 04:13 UTC; 36 tests | [local-unit.log](raw/local-unit.log); precommit working tree based on original base; final hosted log is authoritative for verified SHA |
| Local static checks and preserved files | PASS | Terraform fmt, config scan, Bash syntax, actionlint 1.7.7 (without shellcheck/pyflakes), Git content comparison; 30 Oct | Verified SHA; [local-static.log](raw/local-static.log); **50 original paths** unchanged, including business layers, original tests, migrations, launchers/automation and screenshots |
| Local full Maven integration attempt | BLOCKED | `mvn -B verify` attempted in the earlier working tree: unit tests passed; Testcontainers could not find Docker and integration contexts failed to start | Environment limitation, not counted as a full pass; final hosted suite supplies real PostgreSQL evidence |
| Local Docker/kind runtime | NOT EXECUTED | Docker unavailable in assistant workspace | Hosted container/kind runs above executed successfully instead |
| Local Terraform validate | BLOCKED | Provider cannot `listen unix ... socket: operation not permitted` | [local-terraform.log](raw/local-terraform.log); hosted validate resolved the acceptance gate |
| Actual Windows/WSL wrapper execution | NOT EXECUTED | PowerShell parsed successfully on hosted Ubuntu; no Windows desktop runtime available here | Follow [platform guide](../docs/PLATFORM_GUIDE.md) on the user's PC |
| Manual Azure workflow / OIDC / DB TLS connectivity | NOT EXECUTED | YAML/actionlint validation only; no tenant, subscription, identity, staging credentials or network verified | [Azure guide](../docs/AZURE_DEPLOYMENT.md); not a successful deployment claim |
| Terraform plan/apply, paid Azure resources and public endpoint | NOT EXECUTED | Explicit subscription/cost/networking approval absent | **Azure NOT DEPLOYED**, no resource IDs/live URL/cleanup claim |
| GHCR publication / SBOM / vulnerability scan | NOT EXECUTED | Local image build only; no publish or scan result claimed | Manual publishing procedure documented; source tag is not an immutable image digest |
| External code review | NOT EXECUTED | Self-review and CI only | Draft PR awaits human review; no teammate approval claimed |

## Failed attempts retained and fixed

1. Initial commit `8404997774e6a742f6e0c587399d8fdbd9780b0a`: Terraform validation failed because AzureRM's secret block is a set and cannot be indexed. [Raw failure](raw/first-terraform-failure.log). Commit `440066e3f8e708a2186a6bd5badecfa7a97f8171` corrected the lifecycle exclusion and documented that whole-secret changes/drift are ignored and need deliberate review.
2. Initial real HTTP runs reached healthy app/DB containers but the smoke client's JSON media-type handling rejected Actuator's vendor `+json` response. [Raw container failure](raw/first-container-failure.log). Commit `9ab67139fc510ade64dfbaa95d30cee8a75a7477` corrected parsing without weakening the status-only assertion. [First fully passing runtime run](https://github.com/yusufthecoder474/MotoService-Lite/actions/runs/41002344430).
3. Verified commit `414d06c743961f943513c41633255ad0c6ea34df` added PowerShell syntax validation; the final five-job run passed again. Earlier passes are not added to the 74-test count.

## What the measurements do not prove

No production uptime, cost savings, latency improvement, real customer count, HA, cloud deployment or production security certification. Local kind PostgreSQL uses emptyDir and loses data when its pod/cluster is replaced. The persistence checks restart only the application. Sessions are in-memory and clients reauthenticate after app replacement. No actual database outage/failover or verified Azure TLS handshake was run. Readiness refusal is tested through Spring's availability state, and DB-aware readiness configuration is checked in the actual healthy runtime.

## Stoplight and next actions

- **DONE:** source upgrade, 74 tests, two runtime recovery demonstrations, Terraform static checks, configuration checks, docs and PR-ready changes.
- **BLOCKED:** Azure provisioning pending explicit subscription, cost, IAM, database and networking review.
- **UNVERIFIED:** actual Windows execution, Azure OIDC/Key Vault/TLS path, image publication and external review.
- **NOT EXECUTED:** paid provisioning, public deployment, Terraform plan/apply, SBOM/vulnerability scan.

Next: review PR #2; use `VERIFY_PLATFORM.cmd` on Windows/WSL2 if a PC-specific demonstration is desired. For cloud work, first complete the read-only preflight and approve a concrete cost/network plan; never paste credentials into chat. No cloud approval is required to inspect or run the local deliverable.
