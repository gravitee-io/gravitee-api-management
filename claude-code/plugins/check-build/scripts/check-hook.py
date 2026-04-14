# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "gravitee-dev @ git+https://github.com/gravitee-io/gravitee-devic-framework-samples.git#subdirectory=claude-code/gravitee-dev",
# ]
# ///
"""Claude Code Stop Hook — delegates to gravitee-dev's check_stop_hook.

Fires every time the main Claude agent finishes responding.
Reads the Stop hook JSON from stdin and delegates to
``gravitee_dev.hooks.check_stop_hook`` which handles build checking,
auto-commit on green, and error feedback on red.

Prerequisites:
  - ``uv`` must be installed (handles ``gravitee-dev`` resolution via PEP 723).
  - A ``Taskfile.yml`` must exist in the project root (or set ``CHECK_COMMAND``).
"""

import asyncio
import json
import sys

from claude_agent_sdk.types import HookContext
from gravitee_dev.hooks import check_stop_hook


def _respond(output: dict) -> None:
    """Write a hook output dict as JSON to stdout.

    Remaps ``continue_`` to ``continue`` for the wire format.

    Args:
        output: The hook output dictionary.
    """
    serializable = dict(output)
    if "continue_" in serializable:
        serializable["continue"] = serializable.pop("continue_")
    json.dump(serializable, sys.stdout)


def main() -> None:
    """Entry point for the stop hook script."""
    raw = sys.stdin.read()
    try:
        hook_input = json.loads(raw)
    except json.JSONDecodeError:
        sys.exit(0)

    result = asyncio.run(
        check_stop_hook(
            hook_input=hook_input,
            tool_name=None,
            context=HookContext(cwd=hook_input.get("cwd", ".")),
        )
    )

    _respond(dict(result))


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Hook error: {exc}", file=sys.stderr)
        sys.exit(1)
