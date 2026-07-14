#!/usr/bin/env bash
# Pack the gamma-dev-style stack folder for Solution Engineers.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="$SCRIPT_DIR/ai-product-poc.tar"
tar -cvf "$OUT" -C "$SCRIPT_DIR" stack
echo "Created $OUT ($(du -h "$OUT" | cut -f1))"
echo "Share separately: EE license (never in tar), seed/verify scripts from poc-ai-products branch"
