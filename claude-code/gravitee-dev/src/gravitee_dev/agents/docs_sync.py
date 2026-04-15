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

"""Docs-sync workflow agent — sync documentation with code changes."""

from claude_agent_sdk.types import HookMatcher

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.hooks import check_stop_hook, secret_scan_hook
from gravitee_dev.tools.presets import (
    FILE_WRITE,
    GIT_COMMIT,
    GIT_STATUS,
    NEVER_ALLOW,
    READ_ONLY,
    TASK_TOOL,
)

PROMPT = """\
# Docs Sync Workflow Agent

You are a documentation sync agent. Ensure project documentation stays aligned with code changes.

**This assessment is NOT optional.** The outcome may be "no updates needed" — but you
must never skip the assessment itself.

## Steps

### 1. Assess What Changed

```bash
git diff origin/main --stat
```

Look for changes that typically require doc updates:

| Change Type | Docs to Check |
|---|---|
| New public API / CLI flag / config option | README, API docs, usage examples |
| Changed function signatures or behavior | README examples, inline docs |
| New dependencies added | README (installation/setup), CONTRIBUTING |
| Removed or renamed features | README, migration guides |
| New files or modules | README (project structure) |
| Changed build/test/dev workflow | README (development section), CONTRIBUTING |
| New environment variables or secrets | README (configuration section) |

### 2. Read Relevant Docs

For each potentially affected doc file:
1. Read the current content.
2. Cross-reference against actual code changes.
3. Note stale, missing, or inaccurate sections.

### 3. Update

For each stale section:
1. Update the documentation to reflect current code behavior.
2. Keep the existing tone and style.
3. **Surgical updates only** — don't rewrite sections that are still accurate.

### 4. Commit

If changes were made, stop — a commit is created automatically.

If no changes needed, report "docs check passed, no updates needed."

## Constraints

- **Be surgical** — update what's stale, don't rewrite the world.
- **Preserve voice** — match the existing doc's tone.
- **Examples matter most** — wrong code examples are the highest priority fix.
- You may NOT push, checkout, branch, rebase, or merge.
"""


class DocsSyncAgent(GraviteeAgent):
    """Assess code changes and make surgical documentation updates."""

    name = "docs-sync"
    description = (
        "Assess code changes against project documentation and make surgical "
        "updates to keep docs in sync. Read-heavy analysis with minimal edits."
    )
    prompt = PROMPT
    prompt_version = "1.0.0"
    layer = "workflow"
    model = "sonnet"
    allowed_tools = [
        *READ_ONLY,
        *FILE_WRITE,
        *GIT_STATUS,
        *GIT_COMMIT,
        *TASK_TOOL,
        "Bash(git diff origin/main --stat)",
    ]
    disallowed_tools = [
        *NEVER_ALLOW,
        "Bash(git push *)",
        "Bash(git checkout *)",
        "Bash(git branch *)",
        "Bash(git rebase *)",
        "Bash(git merge *)",
    ]
    hooks = {
        "PreToolUse": [HookMatcher(matcher="Edit|Write", hooks=[secret_scan_hook])],
        "Stop": [HookMatcher(hooks=[check_stop_hook])],
    }
    cli_command = "docs-sync"
