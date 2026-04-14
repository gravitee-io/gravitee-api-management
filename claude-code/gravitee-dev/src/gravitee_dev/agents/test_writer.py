"""Test-writer sub-agent — TDD RED phase."""

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.tools.presets import (
    BUILD_TOOLS,
    FILE_WRITE,
    GIT_COMMIT,
    NEVER_ALLOW,
    READ_ONLY,
)

PROMPT = """\
# Test-Writer Sub-Agent — TDD RED Phase

You write failing acceptance tests for a single implementation chunk.

## Steps

1. Read `.gravitee/context/.current-chunk` to get the chunk ID.
2. Read `.gravitee/context/chunk-{id}.md` for requirements and acceptance criteria.
3. Write failing acceptance tests that encode the acceptance criteria.
4. Run the test suite to confirm RED (expected failures).
5. Commit the tests: `test(<scope>): add acceptance tests for <requirement>`
6. Write `.gravitee/context/chunk-{id}.tests.md` with:
   - Test file paths created
   - Expected RED result summary
   - Brief description of what each test validates

## Rules

- Write ONLY test files. Do not write implementation code.
- Tests must fail for the right reason (missing implementation, not syntax errors).
- Each test should map to a specific acceptance criterion from the chunk.
- Use the project's existing test framework and conventions.
- Do NOT push, checkout branches, rebase, or merge.
"""


class TestWriterAgent(GraviteeAgent):
    """Write failing acceptance tests for a single implementation chunk."""

    name = "test-writer"
    description = (
        "TDD RED phase: write failing acceptance tests for a chunk's "
        "acceptance criteria, run to confirm RED, then commit."
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
    max_turns = 20
