"""Implement orchestrator agent — TDD workflow with test-writer and developer sub-agents."""

from claude_agent_sdk.types import HookMatcher

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.agents.developer import DeveloperAgent
from gravitee_dev.agents.safety_check import SafetyCheckAgent
from gravitee_dev.agents.test_writer import TestWriterAgent
from gravitee_dev.agents.validate_plan import ValidatePlanAgent
from gravitee_dev.hooks import (
    check_stop_hook,
    protect_files_hook,
    secret_scan_hook,
    tdd_orchestration_hook,
    validate_plan_hook,
)
from gravitee_dev.tools.presets import (
    BUILD_TOOLS,
    FILE_WRITE,
    GIT_COMMIT,
    NEVER_ALLOW,
    READ_ONLY,
    TASK_TOOL,
)

PROMPT = """\
# Implement Orchestrator Agent

You orchestrate implementation of an approved plan using strict TDD discipline.
You decompose the plan into chunks, then delegate each chunk to specialized sub-agents
in a RED→GREEN sequence.

## Steps

### 1. Context Loading

- Check if `implementation_plan.md` exists.
- **IF MISSING**: Fetch the PR body using `gh pr view` and extract the plan.
- Load `task.md`.

### 2. Clean Up

- Remove any stale `.gravitee/context/` files from a previous run.
- Create `.gravitee/context/` directory if needed.

### 3. Decomposition

Decompose the plan into implementation chunks. For each chunk, write a
`.gravitee/context/chunk-NNN.md` file containing:
- The requirement / acceptance criteria for this chunk
- Relevant source files to read or modify
- Any dependencies on previous chunks

### 4. Execution Loop

For each chunk (in order):

a. **Set chunk**: Write the chunk ID to `.gravitee/context/.current-chunk`.

b. **RED phase**: Delegate to `test-writer`.
   - Verify `.gravitee/context/chunk-NNN.tests.md` exists after completion.
   - If missing, investigate and retry.

c. **GREEN phase**: Delegate to `developer`.
   - Verify `.gravitee/context/chunk-NNN.impl.md` exists after completion.
   - If missing, investigate and retry.

d. **Verify**: Run the full test suite to confirm GREEN.
   - If tests fail, delegate back to `developer` with the failure details.

e. **Progress**: Update `task.md` to mark the chunk complete.

### 5. Final Verification

- Run the full test suite one final time.
- Report completion: "Implementation complete. All tests passing."

## Constraints

- You may NOT commit directly — commits are made by test-writer and developer.
- You may NOT push, checkout branches, rebase, or merge.
- You may only write files inside `.gravitee/context/`.
- File edits to source/test code are delegated to sub-agents.
"""


class ImplementOrchestratorAgent(GraviteeAgent):
    """Orchestrate TDD implementation with test-writer and developer sub-agents."""

    name = "implement"
    description = (
        "Orchestrate TDD implementation: decompose plan into chunks, "
        "delegate RED phase to test-writer, GREEN phase to developer."
    )
    prompt = PROMPT
    prompt_version = "2.0.0"
    layer = "workflow"
    model = "opus"
    allowed_tools = [
        *READ_ONLY,
        *FILE_WRITE,
        *TASK_TOOL,
        *BUILD_TOOLS,
    ]
    disallowed_tools = [
        *NEVER_ALLOW,
        *GIT_COMMIT,
        "Bash(git push *)",
        "Bash(git checkout *)",
        "Bash(git branch *)",
        "Bash(git rebase *)",
        "Bash(git merge *)",
    ]
    sub_agents = {
        "test-writer": TestWriterAgent(),
        "developer": DeveloperAgent(),
        "validate-plan": ValidatePlanAgent(),
        "safety-check": SafetyCheckAgent(),
    }
    hooks = {
        "PreToolUse": [
            HookMatcher(
                matcher="Edit|Write",
                hooks=[tdd_orchestration_hook, protect_files_hook, secret_scan_hook],
            )
        ],
        "UserPromptSubmit": [HookMatcher(hooks=[validate_plan_hook])],
        "Stop": [HookMatcher(hooks=[check_stop_hook])],
    }
    cli_command = "implement"


# Backward compatibility alias
ImplementAgent = ImplementOrchestratorAgent
