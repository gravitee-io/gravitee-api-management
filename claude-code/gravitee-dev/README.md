# gravitee-dev

Composable CLI agents for Gravitee development workflows, powered by the [Claude Agent SDK](https://docs.anthropic.com/en/docs/claude-code/agent-sdk).

## How It Works

`gravitee-dev` splits the development lifecycle into two layers:

```
PLANNING LAYER (Claude Code plugin — human-driven)
└── /gravitee-dev:gravitee-plan — conversational feature/fix definition
    Outputs: GitHub issue OR implementation_plan.md + .gravitee/plan.lock

           ↓ (contract: validated plan document)

IMPLEMENTATION LAYER (Claude Agent SDK — autonomous)
└── gravitee-dev implement    → TDD execution + atomic commits
└── gravitee-dev docs-sync    → update docs to match code
└── gravitee-dev create-pr    → push and open draft PR
└── gravitee-dev review-loop  → poll comments and iterate until approved
```

**Why the split?** Planning requires human judgment and iteration. Implementation requires determinism and autonomous execution. Mixing them produces worse outcomes in both.

## Prerequisites

| Dependency | Purpose |
|-----------|---------|
| **Python 3.12+** | Runtime |
| [**uv**](https://docs.astral.sh/uv/) | Package & environment manager |
| [**Claude Code**](https://docs.anthropic.com/en/docs/claude-code) >=1.0.33 | Provides `claude-agent-sdk` and the plugin system |
| [**gh CLI**](https://cli.github.com/) | GitHub issue / PR operations |

## Installation

There are two things to install: the **plugin** (planning skills for Claude Code) and the **CLI** (autonomous agents).

### 1. Install the Plugin (Planning Skills)

The plugin adds the `/gravitee-dev:gravitee-plan` skill to Claude Code for human-assisted planning conversations.

**Option A — Install from the local marketplace (recommended):**

From within a Claude Code session:

```
/plugin marketplace add git@github.com:gravitee-io/gravitee-devic-framework-samples.git
/plugin install gravitee-dev@gravitee-marketplace
```

**Option B — Install from GitHub:**

```
/plugin marketplace add gravitee-io/gravitee-devic-framework-samples#subdirectory=claude-code
/plugin install gravitee-dev@gravitee-marketplace
```

**Option C — Test locally during development:**

```bash
claude --plugin-dir /path/to/gravitee-dev
```

Verify by running `/gravitee-dev:gravitee-plan` inside Claude Code — you should see the planning conversation start.

### 2. Install the CLI (Autonomous Agents)

```bash
uv tool install "gravitee-dev @ git+ssh://git@github.com/gravitee-io/gravitee-devic-framework-samples.git#subdirectory=claude-code/gravitee-dev"

```

Or from a local checkout:

```bash
uv tool install -e ./claude-code/gravitee-dev
```

Verify:

```bash
gravitee-dev --help
```

## Walkthrough: Full Workflow Example

This example walks through the complete lifecycle — from rough idea to merged PR.

### Step 1: Plan (Human-Assisted, in Claude Code)

Open Claude Code in your project directory and start a planning conversation:

```
/gravitee-dev:gravitee-plan Add rate-limiting to the REST API
```

Claude will:
1. Read your project context (README, recent git history, open issues)
2. Ask clarifying questions iteratively (acceptance criteria, edge cases, constraints)
3. Propose a structured plan with a checklist of atomic, testable tasks
4. Ask for your explicit approval

Once you approve, Claude asks how you want to output the plan:

- **GitHub Issue** — creates a remote issue with the plan as the body
- **Local Plan File** — writes `implementation_plan.md` + `.gravitee/plan.lock` (SHA-256 checksum)

Choose the output that fits your workflow. For this example, let's say you choose **Local Plan File**.

```
Plan written:
- implementation_plan.md (project root)
- .gravitee/plan.lock (integrity checksum)
```

### Step 2: Implement (Autonomous)

Run the autonomous agent to execute the plan with TDD and atomic commits:

```bash
gravitee-dev implement
```

The agent will:
- Validate the plan integrity (SHA-256 checksum)
- Write acceptance tests first (TDD)
- Implement each checklist item
- Create one semantic commit per task
- Run the build and tests after each change

### Step 3: Open a PR

```bash
gravitee-dev create-pr
```

Pushes the branch and opens a draft PR linked to the issue.

### Step 4: Review Loop

```bash
gravitee-dev review-loop
```

Polls the PR for review comments and runs the `pr-review` agent to address them. Press `c` to mark the PR ready and continue, or `q` to quit. After the review loop exits, merge the PR manually and run `gravitee-dev cleanup --branch <branch>` to remove the worktree.

### Alternative: Full Lifecycle in One Command

If you prefer to skip the interactive planning step and run everything end-to-end:

```bash
gravitee-dev flow "Add rate-limiting to the REST API"
```

This runs a deterministic pipeline: create worktree → implement → docs-sync → create-pr → review-loop.

## Commands

### Workflow commands

| Command | Argument | Default | Description |
|---------|----------|---------|-------------|
| `commit` | Instructions | `"Commit current changes"` | Stage and commit with a semantic message |
| `implement` | Instructions | `"Continue implementation from task.md"` | Execute plan with TDD and atomic commits |
| `create-pr` | Instructions | `"Push and create draft PR"` | Push branch and open a draft Pull Request |
| `docs-sync` | Instructions | `"Sync documentation with code changes"` | Update docs to match code changes |
| `review-loop` | — | — | Poll for PR comments and address them interactively |

### Orchestrator command

| Command | Argument | Default | Description |
|---------|----------|---------|-------------|
| `flow` | Task description or issue ref | *(required)* | Run the full lifecycle end-to-end |

### Worktree commands

| Command | Description |
|---------|-------------|
| `worktrees` | List all active gravitee-managed worktrees |
| `cleanup` | Remove a git worktree and its local branch |

### Flow-specific options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `--pause-after` | `TEXT` | none | Halt after the named phase (implement, docs-sync, create-pr, review-loop) |
| `--skip-review` | flag | off | Skip the review loop phase |
| `--branch` | `TEXT` | auto | Explicit branch name for the worktree |
| `--issue` | `INT` | none | GitHub issue number to link to the worktree |
| `--review-interval` | `INT` | `30` | Seconds between review comment polls |
| `--request-reviewer` | `TEXT` | none | GitHub login to request as reviewer (repeatable) |

### Examples

```bash
# Implement with a specific model and budget cap
gravitee-dev implement --model opus --max-budget 5.0

# Push and open a draft PR
gravitee-dev create-pr

# Full lifecycle linked to an issue, pausing after PR creation
gravitee-dev flow "Add OAuth2 support" --issue 42 --pause-after create-pr

# Full lifecycle skipping the review loop
gravitee-dev flow "Add OAuth2 support" --skip-review

# List active worktrees
gravitee-dev worktrees

# Clean up a worktree after merge
gravitee-dev cleanup --branch feat/oauth2
```

## Global Options

| Option | Short | Type | Default | Description |
|--------|-------|------|---------|-------------|
| `--cwd` | `-C` | `PATH` | current dir | Working directory for the agent |
| `--model` | `-m` | `sonnet\|opus\|haiku` | per-agent | Override the model used |
| `--max-budget` | | `float` | none | Maximum spend in USD |
| `--quiet` | `-q` | flag | off | Suppress all output |

## Architecture

### Composition tree

```
PIPELINE (deterministic, not agent-based)
  flow
    ├─ [worktree init]           (Python — creates isolated git worktree)
    ├─ implement       (WORKFLOW)
    ├─ docs-sync       (WORKFLOW)
    ├─ create-pr       (WORKFLOW)
    │   └─ safety-check    (SUB)
    └─ review-loop             (Python — polls PR and calls pr-review agent)
        └─ pr-review       (WORKFLOW)
```

### Two-layer model

- **Sub-agents** perform one thing well (e.g. stage files and create a single semantic commit). They have strict tool allow-lists and low turn limits.
- **Workflow agents** orchestrate a development phase by combining read/write/git tools with sub-agent delegation. Each maps to one CLI command.
- **`flow`** is a deterministic Python pipeline — not an agent itself. It calls agents in sequence and manages worktree lifecycle. This makes flow behavior predictable and avoids LLM-driven orchestration for the sequencing logic.

### Tool presets

Permissions are composed from reusable presets defined in `tools/presets.py`:

| Preset | Includes |
|--------|----------|
| `GIT_STATUS` | `git status`, `git diff`, `git log` |
| `GIT_COMMIT` | `GIT_STATUS` + `git add`, `git commit`, `git reset HEAD` |
| `GIT_BRANCH` | `GIT_STATUS` + `git checkout`, `git branch`, `git fetch`, `git pull` |
| `GIT_PUSH` | `git push` |
| `GIT_REBASE` | `git rebase`, `git push --force-with-lease` |
| `GH_ISSUE` | `gh issue`, `gh search` |
| `GH_PR` | `gh pr`, `gh api` |
| `READ_ONLY` | `Read`, `Glob`, `Grep` |
| `FILE_WRITE` | `Write`, `Edit` |
| `TASK_TOOL` | `Task` |
| `NEVER_ALLOW` | `rm -rf`, `sudo`, `curl`, `wget`, `WebFetch`, `WebSearch` |

Agents compose presets via list unpacking:

```python
allowed_tools=[*READ_ONLY, *FILE_WRITE, *GIT_COMMIT]
```

Agents compose presets via list unpacking — see `agents/registry.py` for a full listing of each agent's permissions.

## Plugin Structure

This project doubles as a Claude Code plugin. The plugin provides the `/gravitee-dev:gravitee-plan` planning skill. It is distributed via the `gravitee-marketplace` defined in the parent `claude-code/.claude-plugin/marketplace.json`.

```
claude-code/
├── .claude-plugin/
│   └── marketplace.json       # Marketplace catalog (gravitee-marketplace)
└── gravitee-dev/
    ├── .claude-plugin/
    │   └── plugin.json        # Plugin manifest
    ├── skills/
    │   └── gravitee-plan/
    │       └── SKILL.md       # Planning skill definition
    ├── src/
    │   └── gravitee_dev/    # CLI agent source code
    ├── tests/
    ├── pyproject.toml
    └── README.md
```

## Development

```bash
# Clone and install in editable mode
git clone https://github.com/gravitee-io/gravitee-devic-framework-samples.git
cd claude-code/gravitee-dev
uv sync

# Run tests
uv run pytest tests/

# Lint and format
uv run ruff check src/ tests/
uv run ruff format --check src/ tests/

# Test the plugin locally in Claude Code
claude --plugin-dir .
```
