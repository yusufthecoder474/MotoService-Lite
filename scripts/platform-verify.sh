#!/usr/bin/env bash
# Creates a NEW disposable project on each invocation; cleanup touches only that project.
set -euo pipefail
cd "$(dirname "$0")/.."
for tool in docker python3 openssl git; do command -v "$tool" >/dev/null; done
mkdir -p artifacts/platform
export IMAGE_TAG="$(git rev-parse HEAD)"
export PLATFORM_DB_PASSWORD="$(openssl rand -hex 32)"
project="motoservice-check-$(date +%s)-$$"
dc=(docker compose -p "$project" -f compose.platform.yaml)
cleanup() { "${dc[@]}" logs --no-color > artifacts/platform/compose.log 2>&1; "${dc[@]}" down -v --remove-orphans; }
trap cleanup EXIT
if [[ "${1:-}" != --verified-jar ]]; then
  command -v mvn >/dev/null
  mvn -B verify
fi
test -f target/motoservice-lite-1.0.0.jar
"${dc[@]}" up -d --build --wait --wait-timeout 300
export BASE_URL=http://127.0.0.1:18082
export SMOKE_RESULT_PATH=artifacts/platform/compose-smoke.json
python3 scripts/platform-smoke.py
"${dc[@]}" restart app
"${dc[@]}" up -d --wait --wait-timeout 180
SMOKE_PHASE=persistence python3 scripts/platform-smoke.py
"${dc[@]}" ps
