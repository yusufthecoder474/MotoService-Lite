# Cloud/platform verification ledger

Date: 30 Oct 2026 (Asia/Kolkata). Base source: `631c5f459d62ae0b8492a0a374677bf0fb9042e1`. Feature branch: `feature/pg-cloud-platform`. Updated final source SHA and hosted results will be recorded after CI completes; this initial revision does not claim unobserved passes.

| Claim | Status | Command/platform/evidence |
|---|---|---|
| Fresh original baseline | PASS | Hosted Ubuntu, Java 21, `mvn -B verify`; 31 unit/MVC + 34 PostgreSQL IT = 65, zero failure/error/skip; 29 Oct 2026 16:44 UTC; raw `raw/baseline-hosted.log` |
| Modified unit/MVC suite and packaged JAR | PASS | Local Linux amd64, Java 21; `mvn -B package`; 36 tests; raw `raw/local-unit.log` |
| Local full PostgreSQL verification | BLOCKED | `mvn -B verify`; Docker/Testcontainers unavailable; unit tests pass, IT context setup fails; not an application pass |
| Local Docker/Compose/kind | NOT EXECUTED | No Docker daemon/client in execution workspace; hosted jobs prepared |
| Terraform fmt | PASS | Terraform 1.9.8, `fmt -check -recursive` |
| Local Terraform validate | BLOCKED | Provider plugin cannot open Unix socket in this runtime; `raw/local-terraform.log` |
| YAML/config invariants and limited credential scan | PASS | `python3 scripts/validate-platform.py`; not a comprehensive secret/security audit |
| Hosted upgraded CI | NOT EXECUTED | Pending initial feature branch publication |
| Terraform plan/apply | NOT EXECUTED | Subscription, cost and networking approval not supplied |
| Azure deployment/public endpoint | NOT EXECUTED | CLOUD DEPLOYMENT NOT EXECUTED; no cloud resources created |
| SBOM/vulnerability scan | NOT EXECUTED | No scan result claimed |

Fresh baseline run: https://github.com/yusufthecoder474/MotoService-Lite/actions/runs/40733043251 (rerun job 123590569597). Do not add earlier reported Windows tests to this count. Final tests must be counted once from final reports, not summed across reruns.

Hosted acceptance will exercise original concurrency/security suites plus new probe/cloud checks, real HTTP booking/promotion/completion and app-only restart persistence. kind's disposable database pod is not restarted; persistence through database/cluster replacement is not claimed.
