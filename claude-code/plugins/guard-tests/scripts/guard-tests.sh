#!/bin/bash
# PreToolUse hook: enforces TDD immutability for committed test files.
#
# Once a test file has been committed to git, it becomes a constraint —
# the implementation must satisfy the tests, not the other way around.
# This hook blocks Edit/Write on tracked test files so Claude must
# escalate to the developer before modifying acceptance tests.
#
# New (untracked) test files are always allowed — that's the RED phase.
#
# Test file patterns matched:
#   *.test.*, *.spec.*, *_test.*, test_*, *Test.java, *Tests.java,
#   *Spec.java, *IT.java (integration tests)
#
# Exit codes:
#   0 — allow (not a test file, or test file is untracked)
#   2 — block (tracked test file — escalate to developer)

set -euo pipefail

# ── Dependency check ──────────────────────────────────────────────────
if ! command -v jq &>/dev/null; then
  echo "WARNING: jq not found — guard-tests hook cannot run. Allowing edit." >&2
  exit 0
fi

if ! command -v git &>/dev/null; then
  echo "WARNING: git not found — guard-tests hook cannot run. Allowing edit." >&2
  exit 0
fi

# ── Read payload from stdin ───────────────────────────────────────────
payload="$(cat)"
file_path="$(echo "$payload" | jq -r '.tool_input.file_path // empty')"
cwd="$(echo "$payload" | jq -r '.cwd // empty')"

if [ -z "$file_path" ]; then
  exit 0
fi

# ── Check if this is a test file ─────────────────────────────────────
basename="$(basename "$file_path")"

is_test_file=false

case "$basename" in
  # JS/TS/Python: *.test.* or *.spec.*
  *.test.* | *.spec.*)       is_test_file=true ;;
  # Python: test_* or *_test.*
  test_* | *_test.*)          is_test_file=true ;;
  # Java/Kotlin: *Test.java, *Tests.java, *Spec.java, *IT.java
  *Test.java | *Tests.java)   is_test_file=true ;;
  *Spec.java | *IT.java)      is_test_file=true ;;
  *Test.kt   | *Tests.kt)     is_test_file=true ;;
  *Spec.kt   | *IT.kt)        is_test_file=true ;;
esac

if [ "$is_test_file" = false ]; then
  exit 0
fi

# ── Check if the test file is tracked by git ─────────────────────────
git_dir="${cwd:-.}"

# git ls-files returns the file if it's tracked, empty otherwise
tracked="$(git -C "$git_dir" ls-files -- "$file_path" 2>/dev/null)"

if [ -z "$tracked" ]; then
  # Untracked test file — RED phase, allow creation
  exit 0
fi

# ── Blocked: committed test file ─────────────────────────────────────
cat >&2 <<EOF
BLOCKED — TDD guardrail: '$basename' is a committed test file.

Per our TDD discipline, acceptance tests are near-immutable once committed.
A test may only be modified if:
  1. The test itself contains a factual error
  2. Requirements have explicitly changed

Do NOT silently adjust tests to make the implementation pass.
Instead, fix the implementation to satisfy the existing tests.

If you believe the test is genuinely wrong, explain why to the developer
and wait for explicit approval before modifying it.
EOF

exit 2
