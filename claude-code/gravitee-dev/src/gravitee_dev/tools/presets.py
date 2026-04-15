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

"""Tool permission presets — composable allow/disallow lists for agents.

Each preset is a list of tool permission strings compatible with
``ClaudeAgentOptions.allowed_tools`` / ``disallowed_tools``.
Compose them with unpacking: ``[*GIT_STATUS, *READ_ONLY]``.
"""

# --- Git operations (incremental scope) ---

GIT_STATUS: list[str] = [
    "Bash(git status *)",
    "Bash(git diff *)",
    "Bash(git log *)",
]

GIT_COMMIT: list[str] = [
    *GIT_STATUS,
    "Bash(git add *)",
    "Bash(git commit *)",
    "Bash(git reset HEAD *)",
]

GIT_BRANCH: list[str] = [
    *GIT_STATUS,
    "Bash(git checkout *)",
    "Bash(git branch *)",
    "Bash(git fetch *)",
    "Bash(git pull *)",
]

GIT_PUSH: list[str] = [
    "Bash(git push *)",
]

GIT_REBASE: list[str] = [
    "Bash(git rebase *)",
    "Bash(git push origin HEAD --force-with-lease)",
]

# --- GitHub CLI operations ---

GH_ISSUE: list[str] = [
    "Bash(gh issue *)",
    "Bash(gh search *)",
]

GH_PR: list[str] = [
    "Bash(gh pr *)",
    "Bash(gh api *)",
    "Bash(gh auth status)",
]

# --- File operations ---

READ_ONLY: list[str] = [
    "Read",
    "Glob",
    "Grep",
]

FILE_WRITE: list[str] = [
    "Write",
    "Edit",
]

# --- Task delegation ---

TASK_TOOL: list[str] = [
    "Task",
]

# --- Build tools ---

BUILD_TOOLS: list[str] = [
    "Bash(uv run *)",
    "Bash(uv sync *)",
    "Bash(uv build *)",
    "Bash(pnpm run *)",
    "Bash(pnpm test *)",
    "Bash(pnpm build *)",
    "Bash(npm run *)",
    "Bash(npm test *)",
    "Bash(npm build *)",
    "Bash(mvn *)",
    "Bash(task *)",
    "Bash(cargo build *)",
    "Bash(cargo test *)",
    "Bash(make *)",
    "Bash(ruff check *)",
    "Bash(ruff format *)",
    "Bash(pytest *)",
]

# --- Never-allow list (defense-in-depth) ---

NEVER_ALLOW: list[str] = [
    "Bash(rm -rf *)",
    "Bash(sudo *)",
    "Bash(curl *)",
    "Bash(wget *)",
    "WebFetch",
    "WebSearch",
    "Bash(python -c *)",
    "Bash(python3 -c *)",
    "Bash(node -e *)",
    "Bash(bash -c *)",
    "Bash(sh -c *)",
    "Bash(eval *)",
    "Bash(chmod +x *)",
    "Bash(pip install *)",
    "Bash(pip3 install *)",
    "Bash(npm install -g *)",
    "Bash(git filter-branch *)",
    "Bash(git push --force *)",
    "Bash(env *)",
    "Bash(export *)",
]
