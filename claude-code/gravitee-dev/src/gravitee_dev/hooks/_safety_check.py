"""UserPromptSubmit hook — block agent start on dirty working tree.

Runs ``git status --porcelain`` before the first LLM turn.  If the tree
has uncommitted changes the hook blocks with a human-readable file list,
so the agent never wastes a turn delegating to a sub-agent for the check.

Fails closed (blocks) on subprocess errors as a safety measure.
"""

import os
import subprocess

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput


async def clean_working_tree_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    """Block the agent if the working tree has uncommitted changes.

    Args:
        hook_input: The UserPromptSubmitHookInput from the SDK.
        tool_name: Unused for UserPromptSubmit hooks.
        context: Hook context (unused).

    Returns:
        Empty dict to allow start, or ``{"decision": "block", "reason": ...}``
        if the working tree is dirty.
    """
    cwd: str = hook_input.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())

    try:
        result = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=10,
        )
        dirty = [line for line in result.stdout.splitlines() if line.strip()]
    except (subprocess.TimeoutExpired, OSError) as exc:
        return SyncHookJSONOutput(
            decision="block",
            reason=f"Failed to check working tree status: {exc}",
        )

    if not dirty:
        return SyncHookJSONOutput()

    file_list = "\n".join(f"  {line}" for line in dirty)
    return SyncHookJSONOutput(
        decision="block",
        reason=(f"Working tree is dirty. Commit or stash changes before proceeding:\n{file_list}"),
    )
