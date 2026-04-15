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

"""Stop hook — build checker and auto-commit.

When the agent stops and files have changed:
1. Run a configurable build/check command (default: ``task check``).
2. If the build passes, auto-commit via a sub-agent.
3. If the build fails, block the stop and feed errors back.

On re-entry (``stop_hook_active=True``), the hook still runs the full
check-and-commit flow but never blocks — this ensures fixes from the
previous cycle are committed while preventing infinite block loops.
"""

import os
import shlex
import subprocess
import sys
from pathlib import Path

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput

from gravitee_dev.agents.commit import CommitAgent
from gravitee_dev.types import RunConfig

DEFAULT_CHECK_COMMAND = "task check"
DEFAULT_MAX_OUTPUT_LINES = 100


def _run(cmd: str, cwd: str, timeout: int = 120) -> tuple[str, int]:
    """Run a command and return ``(combined_output, exit_code)``.

    Never raises on non-zero exit.
    """
    try:
        args = shlex.split(cmd)
        result = subprocess.run(
            args,
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        output = (result.stdout or "") + (result.stderr or "")
        return output, result.returncode
    except subprocess.TimeoutExpired:
        return f"Command timed out after {timeout}s: {cmd}", 1
    except Exception as exc:
        return f"Failed to run command: {exc}", 1


def _tail(text: str, max_lines: int = DEFAULT_MAX_OUTPUT_LINES) -> str:
    """Return the last *max_lines* lines of *text*, with a truncation notice."""
    lines = text.splitlines()
    if len(lines) <= max_lines:
        return text
    return f"... (truncated {len(lines) - max_lines} lines) ...\n" + "\n".join(lines[-max_lines:])


async def check_stop_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
    *,
    check_command: str | None = None,
    max_output_lines: int | None = None,
) -> SyncHookJSONOutput:
    """Run a build check on stop and auto-commit if green.

    When ``stop_hook_active`` is ``True`` (the SDK's re-entry guard), the hook
    still runs the check-and-commit flow but **never blocks**.  This ensures
    changes made to fix a previous build failure are still verified and
    committed, while preventing infinite block loops.

    Args:
        hook_input: The StopHookInput from the SDK.
        tool_name: Unused for Stop hooks.
        context: Hook context (unused).
        check_command: Override the build check command.
            Falls back to ``CHECK_COMMAND`` env var, then ``"task check"``.
        max_output_lines: Max lines of build output to include in error feedback.

    Returns:
        Empty dict to allow stop, or ``{"decision": "block", "reason": ...}``
        to block and feed errors back.
    """
    is_reentry = hook_input.get("stop_hook_active", False)

    cwd: str = hook_input.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
    cmd = check_command or os.environ.get("CHECK_COMMAND", DEFAULT_CHECK_COMMAND)
    max_lines = max_output_lines or int(
        os.environ.get("OUTPUT_MAX_LINES", DEFAULT_MAX_OUTPUT_LINES)
    )

    # Check for changed files
    git_output, _ = _run("git status --porcelain", cwd)
    changed = [line.strip() for line in git_output.splitlines() if line.strip()]

    if not changed:
        return SyncHookJSONOutput()

    # Run build check
    task_output, exit_code = _run(cmd, cwd)

    if exit_code == 0:
        agent = CommitAgent()
        result = await agent.run(
            "Create exactly ONE semantic commit for the current changes.",
            RunConfig(cwd=Path(cwd), quiet=True),
        )
        if result.is_error:
            print(f"[check-hook] commit agent failed: {result.result_text}", file=sys.stderr)
        return SyncHookJSONOutput()

    # Build failed on re-entry — allow stop to prevent infinite loops
    if is_reentry:
        print(
            f"[check-hook] build still failing on re-entry, allowing stop: "
            f"{_tail(task_output, 10)}",
            file=sys.stderr,
        )
        return SyncHookJSONOutput()

    # Build failed — block and feed errors
    trimmed = _tail(task_output, max_lines)
    error_message = (
        "Please fix the following errors.\n"
        f"The following is a report from {cmd}. Don't do any other investigation\n"
        "<errors-to-fix>\n"
        f"{trimmed}\n"
        "</errors-to-fix>"
    )

    return SyncHookJSONOutput(
        decision="block",
        reason=error_message,
    )
