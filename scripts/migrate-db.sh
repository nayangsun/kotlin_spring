#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
	echo "Usage: ./scripts/migrate-db.sh"
	exit 0
fi

if [[ $# -gt 0 ]]; then
	echo "migrate-db.sh does not accept arguments." >&2
	exit 1
fi

"$(dirname "$0")/db.sh" migrate
