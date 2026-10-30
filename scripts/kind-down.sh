#!/usr/bin/env bash
set -euo pipefail
[[ "${1:-}" == DELETE-motoservice-local ]] || { echo 'Pass DELETE-motoservice-local to delete this disposable cluster and its data'; exit 1; }
kind delete cluster --name motoservice-local
