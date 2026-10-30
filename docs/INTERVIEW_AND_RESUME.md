# Explain the project honestly

## Resume bullets (Google XYZ style)

Use only the verification ledger's current measurements; do not describe Azure as deployed while it is pending.

- Preserved booking correctness across 65 original regression tests by extending a Java 21/Spring Boot/PostgreSQL service with guarded cloud configuration and status-only health probes; final automated pass count: **[FILL IN from final ledger]**.
- Made application recovery repeatable, measured by **[FILL IN verified restart/persistence runs]**, by adding isolated Docker Compose and local kind workflows that verify booking, FIFO promotion and persisted history after app replacement.
- Made cloud changes reviewable through a four-resource Terraform configuration and gated GitHub OIDC workflow, measured by **[FILL IN successful static/hosted checks]**, while documenting cost approval, Key Vault secrets, TLS, rollback and cleanup.

Availability (user-provided): **1 Nov 2026–May 2027; open to relocation**. These dates are availability, not employment history. Replace placeholders only with actual logs; no claimed uptime, cost savings or production users.

## 60-second demo

0–10s: Explain the two-capacity workshop slot and why cancellation should free capacity fairly. Show the existing customer screen.

10–25s: Show two confirmed bookings, a third customer waiting, then cancellation promoting the earliest eligible customer. Show advisor completion/history. Use the actual local demo, not a fabricated video.

25–40s: Open the new CI run and its separate test, container, kind and Terraform results. Show a status-only readiness response and the app restart/persistence receipt if passed.

40–50s: Show the small Terraform topology and manual deployment gate. Say plainly: “Azure deployment is pending approval” unless real cloud evidence has replaced that status.

50–60s: Explain one limitation: local PostgreSQL is disposable, sessions are in-memory, and this is an independent educational project. Point to the LLD/ADR and evidence ledger.

## Ten interview questions

1. **Why Container Apps rather than AKS?** One app needs managed container hosting, not a cluster to operate. kind demonstrates Kubernetes locally; neither implies production AKS experience.
2. **How do you prevent overbooking?** A PostgreSQL pessimistic slot lock serializes occupancy checks and mutations in a transaction; constraints additionally protect duplicates. Concurrency tests use real PostgreSQL.
3. **How is FIFO promotion atomic?** Cancellation and selection/promotion of the oldest eligible queue entry occur under the same slot lock and transaction, with booking/event writes committed together.
4. **Readiness versus liveness?** Readiness includes DB reachability and traffic acceptance. Liveness measures process state so a DB outage should not restart every app. Probes reveal only status.
5. **What does GitHub Actions prove?** Only what the linked jobs actually ran: tests, image build, HTTP restart/persistence, kind behavior and static configuration validation. A green build is not a cloud deployment.
6. **What is sensitive about Terraform state?** State can hold secret values even when marked sensitive. This module uses Key Vault references and a protected Entra-authenticated backend; state/plan files never belong in Git.
7. **How does Azure authentication work?** GitHub exchanges a narrowly scoped OIDC identity for Azure access, tied to the protected environment. No stored client secret. App identity for Key Vault is a separate least-privilege identity.
8. **What code review occurred?** Self-review assisted by tooling and CI. There is no teammate approval to claim. The PR checklist identifies what an independent reviewer should inspect.
9. **What is in the low-level design?** Controller/service/repository interfaces, route permissions, transactional cancellation/promotion flow, relational constraints and deployment boundaries. It reflects the actual classes.
10. **How did you use Agile?** A small independent backlog of seven user stories with acceptance criteria, a delivery board, defects and definition of done. It is Agile-style personal planning, not professional team Scrum experience.
