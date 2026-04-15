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

"""PreToolUse hook — TDD test immutability guard.

Once a test file has been committed to git it becomes a constraint.
The implementation must satisfy the tests, not the other way around.
This hook blocks Edit/Write on *tracked* test files so the agent must
escalate to the developer before modifying acceptance tests.

Untracked test files are always allowed — that's the RED phase.
"""

import fnmatch
import subprocess

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput

TEST_PATTERNS: list[str] = [
    # JS/TS
    "*.test.*",
    "*.spec.*",
    # Python
    "test_*",
    "*_test.*",
    # Java/Kotlin
    "*Test.java",
    "*Tests.java",
    "*Spec.java",
    "*IT.java",
    "*Test.kt",
    "*Tests.kt",
    "*Spec.kt",
    "*IT.kt",
]

_BLOCK_REASON = """\
BLOCKED — TDD guardrail: '{basename}' is a committed test file.

Per our TDD discipline, acceptance tests are near-immutable once committed.
A test may only be modified if:
  1. The test itself contains a factual error
  2. Requirements have explicitly changed

Do NOT silently adjust tests to make the implementation pass.
Instead, fix the implementation to satisfy the existing tests.

If you believe the test is genuinely wrong, explain why to the developer
and wait for explicit approval before modifying it."""


def _is_test_file(basename: str) -> bool:
    """Check if a filename matches any known test file pattern."""
    return any(fnmatch.fnmatch(basename, pattern) for pattern in TEST_PATTERNS)


def _is_tracked(file_path: str, cwd: str) -> bool:
    """Check if a file is tracked by git."""
    try:
        result = subprocess.run(
            ["git", "ls-files", "--", file_path],
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=10,
        )
        return bool(result.stdout.strip())
    except (subprocess.TimeoutExpired, OSError):
        return False


async def guard_tests_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    """Block edits to tracked test files (TDD immutability guard).

    Args:
        hook_input: The PreToolUseHookInput from the SDK.
        tool_name: The tool being invoked (unused).
        context: Hook context (unused).

    Returns:
        Empty dict to allow, or ``{"decision": "block", "reason": ...}`` to block.
    """
    file_path: str = hook_input.get("tool_input", {}).get("file_path", "")
    if not file_path:
        return SyncHookJSONOutput()

    import os

    basename = os.path.basename(file_path)

    if not _is_test_file(basename):
        return SyncHookJSONOutput()

    cwd: str = hook_input.get("cwd", ".")

    if not _is_tracked(file_path, cwd):
        return SyncHookJSONOutput()

    return SyncHookJSONOutput(
        decision="block",
        reason=_BLOCK_REASON.format(basename=basename),
    )
