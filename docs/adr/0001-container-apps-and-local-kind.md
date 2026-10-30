# ADR 0001: Container Apps for optional Azure hosting; kind for local Kubernetes

Date: 30 Oct 2026. Status: accepted for implementation; Azure deployment not approved/executed.

Context: one Java application with PostgreSQL, a student budget and a need to demonstrate delivery/recovery without claiming enterprise operations.

Decision: use Azure Container Apps for optional managed container hosting, and a separate local kind deployment to practice Kubernetes manifests, probes and rolling replacement. Terraform manages four app-side Azure resources. Use an existing approved PostgreSQL instance and Key Vault/identity, not an automatic paid database. Do not create AKS.

Alternatives: AKS adds cluster/network/node operations and potential cost without serving this single-service scope. A VM requires OS/runtime maintenance. Compose remains the simplest local demo, but does not teach Kubernetes readiness/rollout behavior. Container Apps is managed hosting; using it does not prove direct Kubernetes administration.

Consequences: fewer Azure resources to explain, explicit approval boundaries, independent local Kubernetes evidence. Still requires DB routing/TLS, identity, secret management, state protection and cost review. One replica and in-memory sessions limit availability; local PostgreSQL is disposable. No SLA, zero-downtime or production-readiness claim.
