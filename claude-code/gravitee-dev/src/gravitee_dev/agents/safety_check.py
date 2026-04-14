"""Safety-check sub-agent — verifies clean git state before operations."""

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.tools.presets import (
    FILE_WRITE,
    GIT_PUSH,
    GIT_STATUS,
    NEVER_ALLOW,
    TASK_TOOL,
)

PROMPT = """\
# Safety Check Sub-Agent

You are a pre-flight safety checker. Verify the working tree is clean and ready for the
next operation.

## Steps

1. Run `git status` to check the working tree state.
2. **IF dirty** (uncommitted changes, untracked files that matter):
   - Report the dirty state clearly — list the files.
   - Respond with: `SAFETY_CHECK: DIRTY — <reason>`
   - Do NOT attempt to fix it.
3. **IF clean**:
   - Switch to the default branch: `git checkout main`
   - Pull latest: `git pull origin main`
   - Respond with: `SAFETY_CHECK: CLEAN`

## Constraints

- You may ONLY read git status, diff, log, checkout, and pull.
- You may NOT commit, write files, push, rebase, or merge.
- If the tree is dirty, do NOT try to clean it — just report.
"""


class SafetyCheckAgent(GraviteeAgent):
    """Verify the working tree is clean and ready for the next operation."""

    name = "safety-check"
    description = (
        "Verify the working tree is clean and ready for the next operation. "
        "Checks git status, verifies no uncommitted changes, and optionally "
        "switches to the default branch and pulls latest."
    )
    prompt = PROMPT
    prompt_version = "1.0.0"
    layer = "sub"
    model = "haiku"
    allowed_tools = [*GIT_STATUS, "Bash(git checkout *)", "Bash(git pull *)"]
    disallowed_tools = [
        *FILE_WRITE,
        *GIT_PUSH,
        *TASK_TOOL,
        *NEVER_ALLOW,
        "Bash(git commit *)",
        "Bash(git rebase *)",
        "Bash(git merge *)",
    ]
    max_turns = 5
