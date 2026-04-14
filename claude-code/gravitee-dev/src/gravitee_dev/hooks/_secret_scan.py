"""PreToolUse hook — scan for secrets before tool execution.

Attempts gitleaks first, falls back to built-in regex patterns for
common secret types. Fail-closed on timeout. Bypass with
``SKIP_SECRET_SCAN=1`` environment variable.
"""

import os
import re
import subprocess

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput

_SECRET_PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("AWS Access Key", re.compile(r"AKIA[0-9A-Z]{16}")),
    ("AWS Secret Key", re.compile(r"(?i)aws_secret_access_key\s*=\s*\S+")),
    ("GitHub Token", re.compile(r"gh[pousr]_[A-Za-z0-9_]{36,255}")),
    ("Private Key Header", re.compile(r"-----BEGIN\s+(RSA|EC|DSA|OPENSSH)?\s*PRIVATE KEY-----")),
    ("Generic Secret", re.compile(r"(?i)(password|secret|token)\s*[:=]\s*['\"][^'\"]{8,}['\"]")),
]

_GITLEAKS_TIMEOUT = 30


def _run_gitleaks(cwd: str) -> tuple[bool, str]:
    """Run gitleaks on staged changes.

    Returns:
        (secrets_found, output) — True if secrets were detected.
    """
    try:
        result = subprocess.run(
            ["gitleaks", "protect", "--staged", "--no-banner"],
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=_GITLEAKS_TIMEOUT,
        )
        if result.returncode != 0:
            output = (result.stdout or "") + (result.stderr or "")
            return True, output.strip()
        return False, ""
    except FileNotFoundError:
        return False, "gitleaks-not-found"
    except subprocess.TimeoutExpired:
        return True, "gitleaks timed out"


def _regex_scan(content: str) -> list[str]:
    """Scan text for secret patterns using built-in regexes.

    Args:
        content: Text to scan.

    Returns:
        List of matched pattern names.
    """
    matches: list[str] = []
    for name, pattern in _SECRET_PATTERNS:
        if pattern.search(content):
            matches.append(name)
    return matches


async def secret_scan_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    """Scan for secrets before allowing tool execution.

    Args:
        hook_input: The PreToolUseHookInput from the SDK.
        tool_name: The tool being invoked (unused).
        context: Hook context (unused).

    Returns:
        Empty dict to allow, or ``{"decision": "block", "reason": ...}`` to block.
    """
    if os.environ.get("SKIP_SECRET_SCAN") == "1":
        return SyncHookJSONOutput()

    cwd: str = hook_input.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())

    # Try gitleaks first
    secrets_found, output = _run_gitleaks(cwd)
    if secrets_found and output != "gitleaks-not-found":
        return SyncHookJSONOutput(
            decision="block",
            reason=f"Secret scan detected potential secrets:\n{output}",
        )

    # Built-in regex fallback: scan tool input content
    tool_input = hook_input.get("tool_input", {})
    content_to_scan = ""
    for key in ("content", "new_string", "command"):
        val = tool_input.get(key, "")
        if val:
            content_to_scan += str(val) + "\n"

    if content_to_scan.strip():
        matches = _regex_scan(content_to_scan)
        if matches:
            return SyncHookJSONOutput(
                decision="block",
                reason=(
                    f"Secret scan detected potential secrets: {', '.join(matches)}. "
                    "Remove secrets before proceeding."
                ),
            )

    return SyncHookJSONOutput()
