"""PR-review workflow agent — address GitHub PR review comments."""

from claude_agent_sdk.types import HookMatcher

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.hooks import check_stop_hook, gh_auth_hook, secret_scan_hook
from gravitee_dev.tools.presets import (
    FILE_WRITE,
    GH_PR,
    GIT_COMMIT,
    GIT_PUSH,
    NEVER_ALLOW,
    READ_ONLY,
    TASK_TOOL,
)

PROMPT = """\
# PR Review Workflow Agent

You are a PR review handler. Iterate on feedback from GitHub PR review comments until
all are resolved or explained.

## Prerequisites

- A PR must already exist (draft or open).
- The PR must have pending review comments.

## Steps

### 1. Gather Context

a. **Identify the PR**:
```bash
gh pr view --json number,title,url,state,isDraft
```

b. **Fetch review comments**:
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments --paginate
```

c. **Fetch review summaries**:
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/reviews --paginate
```

d. **Build comment inventory**: List all unresolved comments grouped by file and reviewer.

### 2. Review Feedback Loop

For each open review comment:

a. **Assess**: Consider correctness, necessity, trade-offs, and scope.

b. **Decide — Fix or Explain**:
   - **FIX**: Implement the requested change.
   - **EXPLAIN**: Reply with technical rationale for keeping current implementation.

c. **Reply to thread**:
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments/{comment_id}/replies \\
  -f body="<response>"
```

d. **Push**: Push your changes:
```bash
git push origin HEAD
```
(A commit is created automatically when you stop.)

e. **Continue** to the next comment.

### 3. Finalization

When all comments are addressed:
- Re-fetch comments to verify all threads have replies.
- Post a summary comment on the PR.
- Report total comments addressed, fixes implemented, and comments explained.

## Constraints

- Do NOT create implementation plans for each cycle.
- Proceed directly with fixes or explanations.
- You may NOT checkout, branch, rebase, or merge.
- You may be called multiple times in a loop. Address all currently-unresolved
  comments and stop. Do not poll for new comments.
"""


class PrReviewAgent(GraviteeAgent):
    """Iterate on GitHub PR review comments until all are resolved."""

    name = "pr-review"
    description = (
        "Iterate on GitHub PR review comments until all are resolved or explained. "
        "Fetches review threads, assesses each comment, fixes or explains, and pushes."
    )
    prompt = PROMPT
    prompt_version = "1.0.0"
    layer = "workflow"
    model = "sonnet"
    allowed_tools = [
        *READ_ONLY,
        *FILE_WRITE,
        *GIT_COMMIT,
        *GIT_PUSH,
        *GH_PR,
        *TASK_TOOL,
    ]
    disallowed_tools = [
        *NEVER_ALLOW,
        "Bash(git checkout *)",
        "Bash(git branch *)",
        "Bash(git rebase *)",
        "Bash(git merge *)",
    ]
    hooks = {
        "PreToolUse": [
            HookMatcher(matcher="Bash", hooks=[gh_auth_hook]),
            HookMatcher(matcher="Edit|Write", hooks=[secret_scan_hook]),
        ],
        "Stop": [HookMatcher(hooks=[check_stop_hook])],
    }
    cli_command = "pr-review"
