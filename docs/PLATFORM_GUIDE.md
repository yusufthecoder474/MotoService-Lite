# Platform upgrade: start here

This is the existing MotoService application, with deployment and verification additions. Business controllers, transaction logic, schema migrations, original tests, screenshots and `RUN_ALL.cmd` are preserved. Azure is **NOT DEPLOYED**. See the [verification ledger](../evidence/PG_CLOUD_UPGRADE_VERIFICATION.md) for executed results; configuration files alone are not deployment evidence.

## Windows 11 / WSL2

1. Open Docker Desktop → Settings → General → Use the WSL 2 based engine. Under Resources → WSL Integration enable Ubuntu 24.04, then Apply & restart.
2. Open Ubuntu and run `docker version`, `docker compose version`, `java -version`, `mvn -version`, `git --version`, `python3 --version`. Install Java 21, Maven, Git, Python 3 and OpenSSL in Ubuntu if missing. The Java reported by Maven must be 21.
3. Clone this repository inside Ubuntu, check out `feature/pg-cloud-platform`, and keep the working tree clean when producing SHA-tagged images. A ZIP is useful for review, but build scripts need a Git checkout to identify the revision.
4. The original Windows `RUN_ALL.cmd` still performs the original local demonstration. `VERIFY_PLATFORM.cmd` launches the new isolated WSL verifier. Alternatively run `bash scripts/platform-verify.sh` in Ubuntu. It runs `mvn -B verify`, builds the real prebuilt JAR into an image, creates a uniquely named temporary Compose project, exercises HTTP booking/promotion/completion, restarts only the app and checks persisted history. Its exit trap deletes only its own temporary project/volume. It never shares the normal demo database. Port 18082 must be free.
5. Reports are under `artifacts/platform/` and Maven `target/*-reports/`. A passing command exits zero. Existing original evidence is separate from these new measurements.

## Local kind Kubernetes

Install kind and kubectl from their official release pages for Linux in WSL (not a mix of Windows and Linux kubeconfigs). CI pins kind 0.33.0. Verify `kind version`, `kubectl version --client`, `docker info`. Allow several GiB RAM for the cluster and PostgreSQL.

```bash
mvn -B verify
bash scripts/kind-up.sh
bash scripts/kind-verify.sh
kubectl --context kind-motoservice-local -n motoservice-local get pods
kubectl --context kind-motoservice-local -n motoservice-local logs deploy/motoservice
kubectl --context kind-motoservice-local -n motoservice-local port-forward --address 127.0.0.1 service/motoservice 18081:8080
# Open http://127.0.0.1:18081 (local demo accounts only).
# Stop port-forward with Ctrl+C before running the verifier again.
bash scripts/kind-down.sh DELETE-motoservice-local
```

PowerShell equivalents: `./scripts/kind.ps1 -Action up`, then `-Action verify`; teardown is `-Action down -ConfirmDelete`. These call the same Bash scripts in Ubuntu. Teardown irreversibly deletes only the named demonstration cluster and its data.

`kind-up.sh` generates a random local Secret without printing its value, loads `motoservice-lite:<full commit SHA>`, and waits for both Deployments. No host ingress or NodePort exists. The API server and port-forward bind loopback. The verifier uses real HTTP, then performs a controlled **app-only rolling restart**, reconnects port-forward and confirms the promoted completed booking/history survived. It checks current containers are Ready with zero unexpected restarts. PostgreSQL uses `emptyDir`: deleting/replacing its pod loses data. This is not storage durability or HA evidence. App sessions are in-memory, so reauthenticate after replacement; one replica avoids cross-replica session routing concerns.

For a deliberate application failure, on this disposable cluster only:

```bash
kubectl --context kind-motoservice-local -n motoservice-local delete pod -l app=motoservice
kubectl --context kind-motoservice-local -n motoservice-local rollout status deploy/motoservice
SMOKE_PHASE=persistence BASE_URL=http://127.0.0.1:18081 SMOKE_RESULT_PATH=artifacts/platform/kind-smoke.json python3 scripts/platform-smoke.py
```

Restart port-forward first if it disconnected. Do not delete the PostgreSQL pod to test application persistence. `HealthIT` separately verifies refusing traffic returns readiness 503 while liveness stays 200. A DB outage should remove readiness without triggering a liveness restart storm; no real outage claim is made unless logs demonstrate it.

## Build, health and limitations

`mvn -B verify` must precede `docker build --build-arg VCS_REF=$(git rev-parse HEAD) -t motoservice-lite:$(git rev-parse HEAD) .`. The Dockerfile copies that verified JAR, runs as UID 10001, caps JVM heap at 65% of its container memory and exposes 8080. The base image and PostgreSQL minor tag can receive updates: SHA tags identify source, not bit-for-bit image reproducibility. Record the image digest for releases. No SBOM or vulnerability-scan pass is claimed.

Only anonymous **GET** `/actuator/health/liveness` and `/actuator/health/readiness` are permitted. Responses contain status only. All other Actuator paths are denied even to the advisor. CSRF remains enabled, including for Basic-authenticated writes. Readiness includes PostgreSQL; liveness does not. Startup permits 300 seconds in Kubernetes, Hikari acquisition is 3 seconds, and probes allow 4 seconds. Graceful shutdown waits 30 seconds; Kubernetes/isolated Compose allow 40 seconds.

Cloud needs a separate database, private staging accounts, verified JDBC TLS and `SPRING_PROFILES_ACTIVE=cloud`. The startup guard rejects published demo usernames and weak/duplicate TLS parameters. It is a deployment mistake guard, not a substitute for controlling who can change environment variables. The image itself remains usable for the local demo; a local profile must never be exposed publicly.

Next: [Terraform](terraform.md), [Azure guide](AZURE_DEPLOYMENT.md), [LLD](LOW_LEVEL_DESIGN.md), [independent Agile delivery](AGILE_DELIVERY.md).
