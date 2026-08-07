#!/usr/bin/env bash
# Build Go tools into scripts/bin/
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BIN_DIR="${SCRIPT_DIR}/bin"

mkdir -p "$BIN_DIR"

cd "${PROJECT_ROOT}/tools"

echo "Building Go tools..."
go build -o "${BIN_DIR}/" ./cmd/...

echo "Built:"
ls -la "${BIN_DIR}/"
