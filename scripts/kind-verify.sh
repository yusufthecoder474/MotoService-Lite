#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
k=(kubectl --context kind-motoservice-local -n motoservice-local)
mkdir -p artifacts/platform
pid=''
cleanup() { if [[ -n "$pid" ]]; then kill "$pid" 2>/dev/null || true; fi; }
trap cleanup EXIT
forward() {
  "${k[@]}" port-forward --address 127.0.0.1 service/motoservice 18081:8080 > artifacts/platform/port-forward.log 2>&1 &
  pid=$!
}
"${k[@]}" rollout status deploy/postgres --timeout=240s
"${k[@]}" rollout status deploy/motoservice --timeout=360s
forward
export BASE_URL=http://127.0.0.1:18081
export SMOKE_RESULT_PATH=artifacts/platform/kind-smoke.json
python3 scripts/platform-smoke.py
"${k[@]}" rollout restart deployment/motoservice
"${k[@]}" rollout status deployment/motoservice --timeout=360s
cleanup
forward
SMOKE_PHASE=persistence python3 scripts/platform-smoke.py
"${k[@]}" get pods -o json > artifacts/platform/kind-pods.json
python3 - <<'CHECK'
import json
pods=json.load(open('artifacts/platform/kind-pods.json'))['items']
for pod in pods:
    if pod['metadata'].get('deletionTimestamp'): continue
    statuses=pod['status'].get('containerStatuses', [])
    assert statuses and all(c['ready'] and c['restartCount']==0 for c in statuses), pod['metadata']['name']
print('PASS: current app and database containers ready without restart loops')
CHECK
