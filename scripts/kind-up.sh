#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for tool in docker kind kubectl openssl git; do command -v "$tool" >/dev/null; done
test -f target/motoservice-lite-1.0.0.jar || { echo 'First run mvn -B verify'; exit 1; }
image="motoservice-lite:$(git rev-parse HEAD)"
if ! kind get clusters | grep -qx motoservice-local; then
  kind create cluster --name motoservice-local --config deploy/k8s/kind-config.yaml
fi
k=(kubectl --context kind-motoservice-local -n motoservice-local)
docker build --build-arg "VCS_REF=$(git rev-parse HEAD)" -t "$image" .
kind load docker-image "$image" --name motoservice-local
"${k[@]}" apply -f deploy/k8s/namespace.yaml
if ! "${k[@]}" get secret motoservice-db >/dev/null 2>&1; then
  secretfile=$(mktemp); chmod 600 "$secretfile"
  trap 'rm -f "$secretfile"' EXIT
  openssl rand -hex 32 | tr -d '\n' > "$secretfile"
  "${k[@]}" create secret generic motoservice-db --from-file="DB_PASSWORD=$secretfile"
fi
"${k[@]}" apply -f deploy/k8s/configmap.yaml -f deploy/k8s/postgres.yaml
"${k[@]}" rollout status deploy/postgres --timeout=240s
sed "s|motoservice-lite:local-build|$image|" deploy/k8s/app.yaml | "${k[@]}" apply -f -
"${k[@]}" rollout status deploy/motoservice --timeout=360s
