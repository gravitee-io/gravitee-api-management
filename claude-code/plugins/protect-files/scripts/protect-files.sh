#!/bin/bash
# PreToolUse hook: blocks Edit/Write on files matching protected patterns.
# Reads the hook payload (JSON) from stdin, extracts tool_input.file_path,
# and checks it against patterns in protected-patterns.conf.
#
# Exit codes:
#   0 — allow (no match or graceful degradation)
#   2 — block (match found; stderr message shown to the agent)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PATTERNS_FILE="$SCRIPT_DIR/protected-patterns.conf"

# ── Dependency check ──────────────────────────────────────────────────
if ! command -v jq &>/dev/null; then
  echo "WARNING: jq not found — protect-files hook cannot run. Allowing edit." >&2
  exit 0
fi

# ── Read payload from stdin ───────────────────────────────────────────
payload="$(cat)"
file_path="$(echo "$payload" | jq -r '.tool_input.file_path // empty')"

if [ -z "$file_path" ]; then
  # No file_path in payload — nothing to protect against
  exit 0
fi

# ── Load patterns ─────────────────────────────────────────────────────
if [ ! -f "$PATTERNS_FILE" ]; then
  echo "WARNING: $PATTERNS_FILE not found — skipping protection check." >&2
  exit 0
fi

while IFS= read -r line; do
  # Skip blank lines and comments
  pattern="$(echo "$line" | sed 's/#.*//' | xargs)"
  [ -z "$pattern" ] && continue

  if [[ "$file_path" == *"$pattern"* ]]; then
    echo "Blocked: '$file_path' matches protected pattern '$pattern'" >&2
    exit 2
  fi
done < "$PATTERNS_FILE"

# No pattern matched — allow
exit 0
