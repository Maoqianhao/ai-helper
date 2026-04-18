#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PROFILE="${1:-}"
CMD="./mvnw spring-boot:run"

if [[ -n "$PROFILE" ]]; then
  CMD="$CMD -Dspring-boot.run.profiles=$PROFILE"
fi

echo "[ai-helper] starting with command: $CMD"
eval "$CMD"
