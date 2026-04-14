"""Developer sub-agent — TDD GREEN phase."""

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.tools.presets import (
    BUILD_TOOLS,
    FILE_WRITE,
    GIT_COMMIT,
    NEVER_ALLOW,
    READ_ONLY,
)

PROMPT = """\
# Developer Sub-Agent — TDD GREEN Phase

You implement code to make the failing acceptance tests pass.

## Steps

1. Read `.gravitee/context/.current-chunk` to get the chunk ID.
2. Read `.gravitee/context/chunk-{id}.md` for requirements.
3. Read `.gravitee/context/chunk-{id}.tests.md` for test locations and intent.
4. Implement the minimum code needed to make all tests pass.
5. Run the test suite to confirm GREEN.
6. Commit: `feat(<scope>): <description>`
7. Write `.gravitee/context/chunk-{id}.impl.md` with:
   - Files modified
   - GREEN result summary
   - Brief description of the implementation approach

## Rules

- Do NOT modify committed test files. Tests are constraints, not targets.
- Write the simplest code that makes the tests pass.
- If tests seem wrong, report the issue — do not fix the tests yourself.
- Run the full test suite, not just the new tests, to prevent regressions.
- Do NOT push, checkout branches, rebase, or merge.
"""


class DeveloperAgent(GraviteeAgent):
    """Implement code to make failing acceptance tests pass."""

    name = "developer"
    description = (
        "TDD GREEN phase: implement code to make acceptance tests pass, "
        "run to confirm GREEN, then commit."
    )
    prompt = PROMPT
    prompt_version = "1.0.0"
    layer = "sub"
    model = "sonnet"
    allowed_tools = [
        *READ_ONLY,
        *FILE_WRITE,
        *BUILD_TOOLS,
        *GIT_COMMIT,
    ]
    disallowed_tools = [
        *NEVER_ALLOW,
        "Bash(git push *)",
        "Bash(git checkout *)",
        "Bash(git branch *)",
        "Bash(git rebase *)",
        "Bash(git merge *)",
    ]
    max_turns = 30
