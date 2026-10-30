# Independent Agile-style delivery

This is a solo student portfolio iteration, not professional Scrum-team experience. The assistant helped implement and inspect changes; no teammate review or approval is claimed. GitHub issue/PR links and actual CI results are recorded in the evidence ledger.

| Story | Acceptance criteria | Priority |
|---|---|---|
| S1: Preserve booking correctness | Fresh original baseline; unchanged business code; final PostgreSQL regression tests | P1 |
| S2: Detect safe readiness | Status-only probes, DB-aware readiness, secure cloud profile, non-root build | P1 |
| S3: Demonstrate recovery locally | Isolated HTTP booking/promotion; app restart retains completed history; kind checks | P2 |
| S4: Review infrastructure before spending | Four-resource Terraform; fmt/validate; no unapproved plan/apply | P2 |
| S5: Repeat verification in CI | Separate tests/container/config/Terraform/kind jobs and real run links | P2 |
| S6: Gate cloud release safely | Manual OIDC update, private accounts, TLS, cost/network review and confirmed cleanup | P2 |
| S7: Explain design and evidence | LLD, ADR, Windows guide, review notes, claim-by-claim ledger and interview notes | P2 |

Iteration board: S1–S5 and S7 are implemented and verified within the recorded local/hosted scope. S6's deployment path is implemented and statically checked; real Azure execution remains blocked. Runtime acceptance is tracked in the evidence ledger. Cloud provisioning remains blocked by subscription/cost/network approval, not counted as delivered deployment. Do not mark a runtime story Done merely because its YAML exists.

Definition of done: implementation reviewed for preserved behavior; meaningful tests pass in the available runtime or a precise blocker is recorded; secrets absent from public artifacts; docs match actual commands and limitations; evidence includes source SHA, date, command, platform and raw log; unexecuted work explicitly labeled. Cloud deployment additionally needs successful authentication, DB/TLS, health and synthetic workflow plus cleanup inventory.

## Recorded changes and defects

- Restored the upgrade from the authoritative Git repository when the resumed workspace lacked the newer uncommitted checkout; no business code reconstructed from memory.
- Local Terraform provider could not open its Unix socket (`operation not permitted`); static validation moved to hosted CI, rather than treating formatting as validation.
- Local Testcontainers could not find Docker; PostgreSQL acceptance moved to hosted CI. Unit-test success alone is not counted as full verification.
- Added regression checks for duplicated JDBC TLS parameters and persisted published demo accounts. A simple substring check is insufficient for TLS configuration validation.
- Added a separate disposable Compose project so the restart test cannot reset the user's normal local database.

- Hosted validation found that AzureRM represents `secret` as a set, so an indexed lifecycle exclusion was invalid. Changed to an explicit whole-secret exclusion and documented its drift/rotation tradeoff.

- The first real HTTP run found the smoke client handled only `application/json`, while Actuator serves a vendor `+json` media type. Fixed JSON media-type handling while retaining the strict status-only body assertion.

## PR review checklist

- [x] Original 65 tests remain; final test counts derived once from reports.
- [x] No modifications to booking transactions, original migrations, original Windows launcher or screenshots.
- [x] Probe access is GET-only, status-only; all other Actuator routes denied; CSRF retained.
- [x] Cloud cannot seed/use known demo identities; secret values are absent from Git/Terraform inputs.
- [x] DB verified TLS, network prerequisites and least privilege are documented.
- [x] JAR verified before image build; non-root user and SHA source tag recorded.
- [x] Kubernetes namespace/context/loopback explicit; DB persistence limitation clear.
- [x] Terraform validation and runtime CI outcomes linked; no unapproved cloud side effects.
- [x] Cleanup requires exact dedicated scope; review artifacts contain no credentials/state.

Self-review: app/controller/service/schema preservation checked with Git diff; config invariants and focused credential-pattern scan implemented; cloud threat boundaries and manual release caveats documented. This is not an independent security audit or a teammate approval. Base image digest pinning, full secret/history scanning, SBOM and vulnerability scanning remain future work.


Tracking: [issue #1](https://github.com/yusufthecoder474/MotoService-Lite/issues/1), [draft PR #2](https://github.com/yusufthecoder474/MotoService-Lite/pull/2). The checked list records self-review and observed checks, not external reviewer approval. The PR remains unmerged for review.
