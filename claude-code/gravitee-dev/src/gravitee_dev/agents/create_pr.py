#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Create-PR workflow agent — push branch and open a draft PR."""

from claude_agent_sdk.types import HookMatcher

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.agents.safety_check import SafetyCheckAgent
from gravitee_dev.hooks import clean_working_tree_hook, gh_auth_hook
from gravitee_dev.tools.presets import (
    FILE_WRITE,
    GH_PR,
    GIT_PUSH,
    GIT_STATUS,
    NEVER_ALLOW,
    READ_ONLY,
)

PROMPT = """\
# Create-PR Workflow Agent

You are a PR creation agent. Push the current branch and open a draft Pull Request on GitHub.

## Prerequisites

- You are on a feature branch (not main/master).
- The working tree is clean (all changes committed).

## Steps

### 1. Verify Branch

```bash
git status
git log --oneline -5
```

Confirm you are NOT on main/master. If you are, STOP and report the error.

### 2. Push Branch

```bash
git push -u origin HEAD
```

### 3. Determine PR Metadata

- **Title**: Derive from branch name or recent commits. Keep under 70 characters.
- **Body**: Summarize changes from `git log origin/main..HEAD --oneline`.
  Link to the GitHub issue if one exists (check `gh issue list --search`).
- **Labels**: Add relevant labels if available.

### 4. Create Draft PR

```bash
gh pr create --draft --title "<title>" --body "<body>"
```

### 5. Report

Output the PR URL and number.

## Constraints

- You may NOT commit, rebase, or merge.
- You may edit files only to update PR metadata (e.g. PR template).
- Always create in **draft** state.
"""


class CreatePrAgent(GraviteeAgent):
    """Push the current branch and open a draft Pull Request on GitHub."""

    name = "create-pr"
    description = (
        "Push the current branch and open a draft Pull Request on GitHub. "
        "Deterministic workflow — no human judgment required."
    )
    prompt = PROMPT
    prompt_version = "1.0.0"
    layer = "workflow"
    model = "sonnet"
    allowed_tools = [
        *READ_ONLY,
        *FILE_WRITE,
        *GIT_STATUS,
        *GIT_PUSH,
        *GH_PR,
    ]
    disallowed_tools = [
        *NEVER_ALLOW,
        "Bash(git commit *)",
        "Bash(git rebase *)",
        "Bash(git merge *)",
    ]
    sub_agents = {
        "safety-check": SafetyCheckAgent(),
    }
    hooks = {
        "PreToolUse": [HookMatcher(matcher="Bash", hooks=[gh_auth_hook])],
        "UserPromptSubmit": [HookMatcher(hooks=[clean_working_tree_hook])],
    }
    cli_command = "create-pr"
