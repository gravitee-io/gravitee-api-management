"""PreToolUse hook — composite TDD orchestration router.

Routes PreToolUse events by ``agent_type`` to enforce role-specific
file-access rules across the orchestrator and its sub-agents:

- **Orchestrator** (no agent_type): may only write to ``.gravitee/context/``.
- **test-writer**: may only write test files + context reports; requires chunk context.
- **developer**: may write source files; blocked from committed tests; requires tests context.
- **Unknown agent_type**: fail open (no restrictions from this hook).

Block messages flow to the orchestrator's message stream so it can take
corrective action.
"""

import os
from pathlib import Path

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput

from gravitee_dev.hooks._guard_tests import _is_test_file, _is_tracked

_CONTEXT_PREFIX = ".gravitee/context/"


def _is_context_path(file_path: str) -> bool:
    """Check if a file path is inside .gravitee/context/."""
    normalized = file_path.replace("\\", "/")
    return _CONTEXT_PREFIX in normalized


def _read_current_chunk(cwd: str) -> str | None:
    """Read the current chunk ID from .gravitee/context/.current-chunk."""
    chunk_file = Path(cwd) / ".gravitee" / "context" / ".current-chunk"
    try:
        return chunk_file.read_text().strip()
    except (OSError, FileNotFoundError):
        return None


def _chunk_file_exists(cwd: str, chunk_id: str, suffix: str = "") -> bool:
    """Check if a chunk file exists (e.g. chunk-001.md or chunk-001.tests.md)."""
    filename = f"chunk-{chunk_id}{suffix}.md"
    return (Path(cwd) / ".gravitee" / "context" / filename).exists()


def _route_orchestrator(file_path: str) -> SyncHookJSONOutput:
    """Orchestrator: only allow writes to .gravitee/context/."""
    if _is_context_path(file_path):
        return SyncHookJSONOutput()
    return SyncHookJSONOutput(
        decision="block",
        reason=(
            f"BLOCKED — Orchestrator may only write to .gravitee/context/ "
            f"(attempted: '{file_path}'). Delegate file edits to test-writer or developer."
        ),
    )


def _route_test_writer(file_path: str, cwd: str) -> SyncHookJSONOutput:
    """Test-writer: require chunk context, only allow test files + context reports."""
    # Always allow context writes
    if _is_context_path(file_path):
        return _check_chunk_prerequisites(cwd)

    # Check chunk prerequisites first
    prereq = _check_chunk_prerequisites(cwd)
    if prereq:
        return prereq

    # Only allow test files
    basename = os.path.basename(file_path)
    if not _is_test_file(basename):
        return SyncHookJSONOutput(
            decision="block",
            reason=(
                f"BLOCKED — test-writer may only write test files "
                f"(attempted: '{basename}'). Implementation files belong to the developer agent."
            ),
        )

    return SyncHookJSONOutput()


def _route_developer(file_path: str, cwd: str) -> SyncHookJSONOutput:
    """Developer: require tests context, block committed test files."""
    # Always allow context writes
    if _is_context_path(file_path):
        return _check_developer_prerequisites(cwd)

    # Check developer prerequisites (includes chunk + tests.md)
    prereq = _check_developer_prerequisites(cwd)
    if prereq:
        return prereq

    # Block committed test files
    basename = os.path.basename(file_path)
    if _is_test_file(basename) and _is_tracked(file_path, cwd):
        return SyncHookJSONOutput(
            decision="block",
            reason=(
                f"BLOCKED — developer may not modify committed test file '{basename}'. "
                f"Tests are constraints, not targets. Fix the implementation to satisfy the tests."
            ),
        )

    return SyncHookJSONOutput()


def _check_chunk_prerequisites(cwd: str) -> SyncHookJSONOutput:
    """Check that .current-chunk exists and the corresponding chunk-NNN.md exists."""
    chunk_id = _read_current_chunk(cwd)
    if chunk_id is None:
        return SyncHookJSONOutput(
            decision="block",
            reason=(
                "BLOCKED — .gravitee/context/.current-chunk is missing. "
                "The orchestrator must set the current chunk before delegating."
            ),
        )

    if not _chunk_file_exists(cwd, chunk_id):
        filename = f"chunk-{chunk_id}.md"
        return SyncHookJSONOutput(
            decision="block",
            reason=(
                f"BLOCKED — .gravitee/context/{filename} is missing. "
                f"The orchestrator must write the chunk requirements before delegating."
            ),
        )

    return SyncHookJSONOutput()


def _check_developer_prerequisites(cwd: str) -> SyncHookJSONOutput:
    """Check chunk prerequisites + tests.md existence."""
    prereq = _check_chunk_prerequisites(cwd)
    if prereq:
        return prereq

    chunk_id = _read_current_chunk(cwd)
    assert chunk_id is not None  # guaranteed by _check_chunk_prerequisites

    if not _chunk_file_exists(cwd, chunk_id, suffix=".tests"):
        filename = f"chunk-{chunk_id}.tests.md"
        return SyncHookJSONOutput(
            decision="block",
            reason=(
                f"BLOCKED — .gravitee/context/{filename} is missing. "
                f"The test-writer must run before the developer."
            ),
        )

    return SyncHookJSONOutput()


async def tdd_orchestration_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    """Composite PreToolUse hook routing by agent_type.

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

    agent_type: str | None = hook_input.get("agent_type")
    cwd: str = hook_input.get("cwd", ".")

    if agent_type is None:
        return _route_orchestrator(file_path)
    elif agent_type == "test-writer":
        return _route_test_writer(file_path, cwd)
    elif agent_type == "developer":
        return _route_developer(file_path, cwd)
    else:
        # Fail open for unknown agent types
        return SyncHookJSONOutput()
