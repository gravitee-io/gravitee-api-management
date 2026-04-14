"""PreToolUse hook — verify GitHub CLI authentication.

Runs ``gh auth status`` and hard-blocks if not authenticated or on error.
Fail-closed: any exception blocks the tool call.
"""

import subprocess

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput


async def gh_auth_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    """Block tool calls if ``gh auth status`` fails.

    Args:
        hook_input: The PreToolUseHookInput from the SDK.
        tool_name: The tool being invoked (unused).
        context: Hook context (unused).

    Returns:
        Empty dict to allow, or ``{"decision": "block", "reason": ...}`` to block.
    """
    try:
        result = subprocess.run(
            ["gh", "auth", "status"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        if result.returncode != 0:
            output = (result.stdout or "") + (result.stderr or "")
            return SyncHookJSONOutput(
                decision="block",
                reason=f"GitHub CLI not authenticated. Run `gh auth login`.\n{output.strip()}",
            )
    except (subprocess.TimeoutExpired, OSError) as exc:
        return SyncHookJSONOutput(
            decision="block",
            reason=f"Failed to verify GitHub auth: {exc}",
        )

    return SyncHookJSONOutput()
