#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
	echo "Usage: ./scripts/dev-reset.sh"
	exit 0
fi

if [[ $# -gt 0 ]]; then
	echo "dev-reset.sh does not accept arguments." >&2
	exit 1
fi

"$(dirname "$0")/reset-db.sh"
./gradlew bootRun
