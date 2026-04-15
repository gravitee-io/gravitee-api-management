#!/bin/bash
#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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
