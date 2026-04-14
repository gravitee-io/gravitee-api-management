# Dev Agent Framework — Architecture Document (v3)

> **Changelog from v2**: This revision addresses 18 security, safety, and design risks identified in the v2 review. All changes are marked with `[v3]`. See Section 15 for the updated risk register and a full change summary.

## Table of Contents

1. [Vision & Decisions](#1-vision--decisions)
2. [Two-Layer System](#2-two-layer-system)
3. [Architecture](#3-architecture)
4. [Claude Code Skills & Plugins (Planning Layer)](#4-claude-code-skills--plugins-planning-layer)
5. [Agent Catalogue (Implementation Layer)](#5-agent-catalogue-implementation-layer)
6. [Hook System](#6-hook-system)
7. [Tool Permission System](#7-tool-permission-system)
8. [Runner & SDK Integration](#8-runner--sdk-integration)
9. [CLI Interface](#9-cli-interface)
10. [Workflow](#10-workflow)
11. [Data Schemas](#11-data-schemas)
12. [Code Patterns](#12-code-patterns)
13. [Testing](#13-testing)
14. [Roadmap](#14-roadmap)
15. [Risks & Open Questions](#15-risks--open-questions)

---

## 1. Vision & Decisions

### The Core Split

The framework is built on a **clear separation of concerns** between two layers:

| Layer | Technology | Who drives it | Purpose |
|---|---|---|---|
| **Planning** | Claude Code skills & plugins | Human | Conversational feature definition, issue creation, plan writing |
| **Implementation** | Claude Agent SDK | Autonomous agents | TDD execution, commits, PR lifecycle |

This separation is deliberate. Planning requires human judgment, context, and back-and-forth refinement. Implementation requires determinism, tool safety, and autonomous execution. Mixing the two in a single agent produces worse outcomes in both.

### Decisions Locked In

- **Planning is conversational, not a CLI command** — the human drives the plan via a chat session with Claude Code skills. No autonomous agent defines what to build.
- **Skills & plugins for planning outputs** — GitHub issue creation and `implementation_plan.md` writing are handled by Claude Code skills callable from the conversation, not by local agent code.
- **Claude Agent SDK for autonomous execution** — all implementation agents run via `ClaudeSDKClient` with `bypassPermissions` mode. **[v3]** Because `bypassPermissions` removes OS-level safeguards, all protection relies on hook correctness and allow/disallow lists. These must be treated as the security perimeter and reviewed with the same rigour as access-control code.
- **Local CLI tools over MCP (implementation layer)** — git, gh CLI, test runners, linters invoked via scoped `Bash` permissions.
- **Declarative agent specs** — each agent is a frozen `AgentSpec` dataclass; no class hierarchies.
- **Composable tool permissions** — preset lists (`GIT_COMMIT`, `READ_ONLY`, etc.) composed via unpacking.
- **Programmatic hooks** — deterministic guardrails built as async Python functions, not shell scripts.
- **Three-layer agent model** — sub-agents (atomic ops), workflow agents (dev phases), orchestrator (end-to-end).
- **Typer CLI entry point** — `gravitee-dev <command>` exposes each agent as a standalone command.
- **Human gates at plan, mid-implementation, and review** — human approves the plan (before implementation), can insert mid-flow checkpoints via `--pause-after`, and must approve the final PR before merge. **[v3]** The `--pause-after implement` checkpoint is strongly recommended for large or high-risk plans.
- **TDD enforcement** — test files become immutable once committed; implementation must satisfy tests, not the other way around.
- **[v3] Plan document integrity** — `implementation_plan.md` is schema-validated and SHA-256 checksummed before the `implement` agent reads it. The checksum is written to `.gravitee/plan.lock` at approval time and re-verified at execution time.
- **[v3] Secret scanning is mandatory** — a `secret_scan_hook` runs as a `PreToolUse` hook on all commit operations and as a standalone pre-push check. It is not optional.
- **[v3] Concurrency lock** — a `.gravitee/gravitee.lock` file is acquired at `flow` start and released on exit, preventing two concurrent flow runs against the same working directory.
- **[v3] Structured audit log** — every agent run appends a structured JSON record to `.gravitee/audit.jsonl`, capturing agent name, prompt version, tools called, cost, and outcome.
- **[v3] `disallowed_tools` propagation is a test-enforced invariant** — a dedicated test asserts that every hook that spawns a sub-agent explicitly passes `disallowed_tools`. Omitting it is a test failure, not just a documentation note.

### Core Principles

| Principle | Description |
|---|---|
| **Human-driven planning** | Features are defined by humans in conversation, not inferred by autonomous agents. |
| **Declarative over Imperative** | Implementation agents are data (`AgentSpec`), not behavior classes. The runner interprets specs uniformly. |
| **Least-Privilege Tools** | Each agent gets only the tools it needs. The commit agent can't push; the ship agent can't write files. |
| **Deterministic Guardrails** | Hooks enforce invariants (file protection, TDD immutability, build verification) without relying on prompt compliance. |
| **CLI-First, No Daemons** | All external actions use tools already in the developer's environment. No background services or OAuth setup required. |
| **Fail Loud, Fail Safe** | Hooks block on violations and feed errors back. Re-entry guards prevent infinite loops. **[v3]** Safety-critical hooks (secret scanning, plan integrity, working tree checks) fail **closed** — a subprocess error is treated as a violation, not a green light. |
| **[v3] Defense in Depth** | No single hook is the last line of defence. Secret scanning, file protection, and permission lists all independently guard the same boundary. |
| **[v3] Audit Everything** | Every agent action is logged to an append-only structured file. Post-incident debugging must not depend solely on git history. |

---

## 2. Two-Layer System

```
┌──────────────────────────────────────────────────────────────┐
│                   PLANNING LAYER                             │
│            (Claude Code Skills & Plugins)                    │
│                                                              │
│  Human ←──────────────────────────────── Claude             │
│    │   back-and-forth chat conversation    │                 │
│    │                                       │                 │
│    └──── approves plan ────────────────────┘                 │
│                    │                                         │
│                    ▼                                         │
│         [plan-skill] invoked                                 │
│              │              │                                │
│         GitHub Issue    implementation_plan.md               │
│         (gh plugin)     (filesystem plugin)                  │
│                    │                                         │
│         [v3] SHA-256 checksum written to                     │
│              .gravitee/plan.lock                             │
└──────────────────────────────────────────────────────────────┘
                    │
                    │  contract: structured + validated plan
                    ▼
┌──────────────────────────────────────────────────────────────┐
│                 IMPLEMENTATION LAYER                         │
│               (Claude Agent SDK — CLI)                       │
│                                                              │
│  gravitee-dev implement                                    │
│       [v3] validate plan schema + verify checksum            │
│       reads: implementation_plan.md  OR  gh issue body       │
│                                                              │
│  → TDD loop → commits → docs-sync → PR → review → ship      │
│                                                              │
│  [v3] .gravitee/gravitee.lock  (concurrency guard)           │
│  [v3] .gravitee/audit.jsonl    (append-only audit log)       │
└──────────────────────────────────────────────────────────────┘
```

The **contract between the two layers** is the plan document. It can live as a GitHub issue body, a local `implementation_plan.md`, or both. The implementation layer consumes it but never modifies it — the plan is the source of truth, not the agent's interpretation of the task. **[v3]** The plan is schema-validated against a known structure before execution, and its checksum is verified to detect tampering between approval and execution.

---

## 3. Architecture

### System Overview

```
PLANNING LAYER
  Claude Code conversation
        │
        │  [plan-skill] triggered by user at end of conversation
        ▼
  ┌─────────────────────────┐     ┌──────────────────────────┐
  │   GitHub plugin          │     │   Filesystem plugin       │
  │   gh issue create        │     │   write implementation_   │
  │                          │     │   plan.md                 │
  └─────────────────────────┘     └──────────────────────────┘
        │                                     │
        │  [v3] SHA-256 of plan content ───────┘
        │       written to .gravitee/plan.lock

IMPLEMENTATION LAYER
  gravitee-dev CLI (Typer)
        │
        │  gravitee-dev <command> <prompt> [--cwd] [--model] [--max-budget]
        ▼
  ┌─────────────────────────┐
  │  [v3] pre-flight checks  │   Concurrency lock · Plan schema validation · Checksum verify
  └─────────────────────────┘
        │
        ▼
  ┌─────────────────────────┐
  │     run_agent()          │   Resolves sub-agents, composes options, invokes SDK
  │     (runner.py)          │
  └─────────────────────────┘
        │
        ▼
  ┌─────────────────────────┐
  │   ClaudeSDKClient        │   claude_agent_sdk — model, tools, hooks, agents
  │   (claude-agent-sdk)     │
  └─────────────────────────┘
        │
        ├─── Hooks (programmatic, per-agent)
        │     ├── PreToolUse: protect_files, guard_tests
        │     ├── PreToolUse: gh_auth (agents using GH_PR / GH_ISSUE)
        │     ├── PreToolUse: [v3] secret_scan (all agents with FILE_WRITE or GIT_COMMIT)
        │     ├── Stop: check_stop (build + auto-commit)
        │     └── UserPromptSubmit: clean_working_tree [v3: fails closed]
        │
        ├─── Sub-agents (flat dict, recursively resolved)
        │     ├── commit (haiku, max 10 turns)
        │     ├── safety-check (haiku, max 5 turns)
        │     └── [v3] validate-plan (haiku, max 3 turns)
        │
        ├─── Tool permissions (allow/disallow lists from presets.py)
        │
        └─── [v3] Audit log (append-only .gravitee/audit.jsonl per run)
```

### Three-Layer Agent Model (Implementation Layer)

```
ORCHESTRATOR
  └── flow (sonnet)
        ├── implement       (WORKFLOW, opus)
        │     ├── validate-plan   (SUB, haiku — pre-flight)   [v3]
        │     ├── safety-check    (SUB, haiku — pre-flight)
        │     └── commit          (SUB, haiku — via Stop hook)
        ├── docs-sync       (WORKFLOW, sonnet)
        │     └── commit          (SUB, haiku — via Stop hook)
        ├── copilot-review  (WORKFLOW, sonnet)
        │     └── commit          (SUB, haiku — via Stop hook)
        ├── pr-review       (WORKFLOW, sonnet)
        │     └── commit          (SUB, haiku — via Stop hook)
        └── ship            (WORKFLOW, sonnet)
              └── [v3] rollback   (SUB, haiku — on failure)
```

### Human / Autonomous Boundary

| Phase | Layer | Who | What |
|---|---|---|---|
| Planning | Claude Code skill | Human + AI | Conversational feature definition. Skill outputs GitHub issue and/or `implementation_plan.md`. Human approves before anything is written. **[v3]** Plan checksum written to `.gravitee/plan.lock`. |
| Branch creation | Implementation (manual or skill) | Human | `git checkout -b feature/...` — triggered by the user or by the plan skill optionally. |
| Plan validation | Implementation (Agent SDK) | Autonomous | **[v3]** `validate-plan` sub-agent verifies schema and checksum match before any code is written. |
| Implementation | Implementation (Agent SDK) | Autonomous | Implement agent executes plan with TDD. Stop hook auto-commits on green. **[v3]** `--pause-after implement` recommended for large tasks. |
| Docs sync | Implementation (Agent SDK) | Autonomous | Docs-sync agent assesses and updates documentation. |
| Create PR | Planning (Claude Code skill) | Human | `/gh-create-pr` skill pushes branch and opens draft PR. |
| Copilot review | Implementation (Agent SDK) | Autonomous | Copilot-review agent iterates on Copilot feedback, marks PR ready. **[v3]** Timeout failure halts the flow with explicit error; does not silently proceed. |
| Final review | Implementation (Agent SDK) | Human + AI | Human reviews the PR. |
| Ship | Implementation (Agent SDK) | Autonomous | Ship agent rebases, merges, and cleans up. **[v3]** Compensating rollback sub-agent runs on failure. |

---

## 4. Claude Code Skills & Plugins (Planning Layer)

### Overview

The planning layer uses Claude Code's skill and plugin system. It is not a CLI command and does not use the Agent SDK. It is a conversational interface where the human drives requirements definition.

### `plan` Skill — Feature Planning Conversation

**Invoked from**: Claude Code chat session  
**Trigger**: User describes a feature, bug, or task in natural language

**Conversation flow**:

```
① User provides rough brief
   │  "I want to add OAuth2 login support"
   │
   ▼
② Claude asks clarifying questions iteratively
   │  - What problem does this solve?
   │  - What are the acceptance criteria?
   │  - Edge cases? Constraints?
   │  - Any dependencies on existing code or services?
   │  - What does done look like?
   │
   ▼
③ Claude proposes a structured plan
   │  User refines, pushes back, adds context
   │  Continues until user approves
   │
   ▼
④ User triggers output (explicit approval)
   │
   ├─── Output: GitHub Issue
   │      [gh-plugin] invoked
   │      Creates issue with structured plan as body
   │      Returns issue URL + number
   │
   ├─── Output: Local file
   │      [filesystem-plugin] invoked
   │      Writes implementation_plan.md to project cwd
   │
   └─── Output: Both (recommended)
          Creates GitHub issue first
          Writes implementation_plan.md
          Embeds issue number in file header: `<!-- Closes #42 -->`
          [v3] Writes SHA-256 checksum of plan content to .gravitee/plan.lock
               Format: { "sha256": "<hash>", "issued_at": "<iso8601>", "issue": 42 }
```

**Plan document structure** (output of the skill):

```markdown
<!-- Closes #42 -->
<!-- plan-schema-version: 1 -->

## [Feature/Fix title]

### Problem

Clear description of what the issue is or what capability is missing.
Written from the user's perspective — no implementation details yet.

### Approach

How we intend to solve it. Architecture decisions, trade-offs considered,
any constraints that shaped the approach.

### Checklist
- [ ] task_1: ...
- [ ] task_2: ...
```

**[v3]** The `<!-- plan-schema-version: 1 -->` marker is required. The `validate-plan` sub-agent will reject any plan file that is missing this marker, lacks a `### Checklist` section, or contains no `- [ ]` items. This prevents the agent from silently acting on a malformed or injected plan.

The checklist is the single source of truth for both task tracking and acceptance criteria. When all boxes are checked, the feature is complete. The implement agent reads this checklist to drive the TDD loop and marks items complete as it goes.

### Plugins Required

| Plugin | Purpose | Auth required |
|---|---|---|
| `gh-plugin` | Create/update GitHub issues | GitHub token |
| `filesystem-plugin` | Write `implementation_plan.md` to project directory | Local fs access |
| **[v3]** `checksum-plugin` | Write SHA-256 of plan to `.gravitee/plan.lock` | Local fs access |

### What the Skill Does NOT Do

- Does not create a git branch (user does this, or optionally the skill can)
- Does not read the codebase autonomously
- Does not make architectural decisions unilaterally
- Does not invoke the Agent SDK

---

## 5. Agent Catalogue (Implementation Layer)

### Sub-Agents (narrow, atomic operations)

#### `commit` — Semantic Commit Helper

- **Model**: haiku | **Max turns**: 10
- **Role**: Creates exactly ONE semantic commit. Stages files individually (never `git add .`), writes a user-facing changelog-style message, runs `git diff --cached` to verify only expected files are staged, and never pushes.
- **Allowed**: `GIT_COMMIT`, `Read`
- **Disallowed**: `FILE_WRITE`, `GIT_PUSH`, `TASK_TOOL`, `NEVER_ALLOW`, checkout/branch/rebase/merge
- **Invoked by**: `check_stop_hook`

> **[v3] Note**: When spawned inside `check_stop_hook` as an `AgentDefinition`, `disallowed_tools` must be explicitly passed through in the hook's `ClaudeAgentOptions`. The SDK does not propagate disallowed_tools to sub-agent definitions automatically. This is now **enforced by a dedicated test** (`test_hooks.py::test_check_stop_hook_passes_disallowed_tools`) — a hook that spawns a sub-agent without explicit `disallowed_tools` will fail the test suite.

#### `safety-check` — Pre-flight Git State Verifier

- **Model**: haiku | **Max turns**: 5
- **Role**: Verifies working tree is clean. If dirty, reports and aborts. If clean, checks out `main` and pulls latest.
- **Allowed**: `GIT_STATUS`, `git checkout`, `git pull`
- **Disallowed**: `FILE_WRITE`, `GIT_PUSH`, `TASK_TOOL`, `NEVER_ALLOW`, commit/rebase/merge
- **[v3] Note**: This agent performs `git checkout main`. It must only be invoked when the working tree is confirmed clean. If `clean_working_tree_hook` fails or errors, `safety-check` must not run — the `implement` agent prompt explicitly guards this sequencing.

#### `validate-plan` — Plan Integrity Verifier **[v3]**

- **Model**: haiku | **Max turns**: 3
- **Role**: Reads `implementation_plan.md` (or fetches from `gh issue view #N`). Verifies:
  1. `plan-schema-version` marker is present and supported.
  2. Required sections (`### Problem`, `### Approach`, `### Checklist`) exist.
  3. At least one `- [ ]` item is present.
  4. SHA-256 of the file content matches `.gravitee/plan.lock`.
  5. No obvious prompt-injection patterns (e.g., lines starting with `IGNORE ABOVE`, `---NEW INSTRUCTIONS---`, or `<system>`).
- **Allowed**: `Read`, `GIT_STATUS`
- **Disallowed**: `FILE_WRITE`, all git write operations, `NEVER_ALLOW`
- **Invoked by**: `implement` (before `safety-check`)
- **On failure**: Halts immediately with a clear error. Does not proceed to implementation.

#### `rollback` — Compensating Action Handler **[v3]**

- **Model**: haiku | **Max turns**: 8
- **Role**: Invoked by `ship` on failure after a force-push has already occurred. Attempts to restore the remote branch to its pre-rebase state using `git push origin HEAD --force-with-lease` with the pre-rebase ref. If branch deletion has already occurred, logs the last known SHA and instructs the human on manual recovery. Never merges, never modifies main.
- **Allowed**: `Read`, `GIT_STATUS`, `GIT_PUSH`
- **Disallowed**: `FILE_WRITE`, `GIT_COMMIT`, `TASK_TOOL`, `NEVER_ALLOW`, checkout/rebase/merge
- **Invoked by**: `ship` (on failure only)

### Workflow Agents (multi-step development phases)

#### `implement` — TDD Implementation Loop

- **Model**: opus | **CLI**: `gravitee-dev implement`
- **Role**: Loads plan from `implementation_plan.md` (or issue body via `gh issue view #N`). Runs `validate-plan` sub-agent first, then `safety-check`. Iterates through the plan checklist: implement → verify (build/test/lint) → commit (auto via hook) → mark item complete.
- **TDD enforcement**: Writes failing tests before implementation. The `implement` agent reads the checklist directly from `implementation_plan.md` or the GitHub issue body. There is no separate `task.md` — the plan document is the single artifact that tracks both intent and progress.
- **[v3] Test quality gate**: The `check_stop_hook` verifies that test coverage has not decreased from the pre-task baseline (using `--cov-fail-under` with the project's configured threshold). A commit that reduces overall coverage is blocked.
- **Allowed**: `READ_ONLY`, `FILE_WRITE`, `GIT_COMMIT`, `TASK_TOOL`, build tools (`uv`, `pnpm`, `npm`, `mvn`, `task`, `cargo`, `make`, `ruff`)
- **Disallowed**: `NEVER_ALLOW`, push/checkout/branch/rebase/merge
- **Sub-agents**: `validate-plan` [v3], `safety-check`, `commit`
- **Hooks**: `PreToolUse(Edit|Write)` → `protect_files_hook`, `guard_tests_hook`, `secret_scan_hook` [v3] | `Stop` → `check_stop_hook`

**Plan source resolution**:

```
gravitee-dev implement                    → reads implementation_plan.md from cwd
gravitee-dev implement --issue 42         → gh issue view 42 (fetches plan from body)
```

#### `docs-sync` — Documentation Synchronization

- **Model**: sonnet | **CLI**: `gravitee-dev docs-sync`
- **Role**: Assesses code changes against docs (`git diff origin/main --stat`). Makes surgical updates only. Mandatory in the full flow — outcome may be "no updates needed" but the assessment is never skipped.
- **Allowed**: `READ_ONLY`, `FILE_WRITE`, `GIT_STATUS`, `GIT_COMMIT`, `TASK_TOOL`, `git diff origin/main --stat`
- **Disallowed**: `NEVER_ALLOW`, push/checkout/branch/rebase/merge
- **Hooks**: `Stop` → `check_stop_hook` | `PreToolUse(Edit|Write)` → `secret_scan_hook` [v3]

#### `pr-review` — PR Review Comment Handler

- **Model**: sonnet | **CLI**: `gravitee-dev pr-review`
- **Role**: Fetches GitHub PR review comments. For each: assess → fix or explain → reply to thread → push. Re-fetches to verify all addressed. Posts summary comment.
- **Allowed**: `READ_ONLY`, `FILE_WRITE`, `GIT_COMMIT`, `GIT_PUSH`, `GH_PR`, `TASK_TOOL`
- **Disallowed**: `NEVER_ALLOW`, checkout/branch/rebase/merge
- **Hooks**: `Stop` → `check_stop_hook` | `PreToolUse` → `gh_auth_hook` | `PreToolUse(Edit|Write)` → `secret_scan_hook` [v3]

#### `copilot-review` — Copilot Review Loop

- **Model**: sonnet | **CLI**: `gravitee-dev copilot-review`
- **Role**: Requests Copilot review on PR. Iterates on feedback: assess → fix or reject → push → re-request → poll (configurable intervals, configurable max timeout). Verifies feedback was actually retrieved before declaring success. When done, updates `walkthrough.md` and marks PR ready (`gh pr ready`).
- **[v3] Timeout failure behavior**: If `COPILOT_TIMEOUT` is reached and no feedback has been retrieved, the agent **halts with a non-zero exit code** and reports `copilot_review: TIMED_OUT` in the audit log. The flow does not proceed to human review. The user must re-run `gravitee-dev copilot-review` manually or skip it with `--skip-copilot`.
- **Allowed**: `READ_ONLY`, `FILE_WRITE`, `GIT_COMMIT`, `GIT_PUSH`, `GH_PR`, `TASK_TOOL`, `sleep`
- **Disallowed**: `NEVER_ALLOW`, checkout/branch/rebase/merge
- **Hooks**: `Stop` → `check_stop_hook` | `PreToolUse` → `gh_auth_hook` | `PreToolUse(Edit|Write)` → `secret_scan_hook` [v3]
- **Config**: `COPILOT_POLL_INTERVAL` (default: 30s), `COPILOT_TIMEOUT` (default: 300s) — both overridable via env vars

#### `ship` — Merge & Cleanup

- **Model**: sonnet | **CLI**: `gravitee-dev ship`
- **Role**: Verifies PR readiness (approvals, CI). Rebases onto main. Force-pushes with `--force-with-lease`. Merges via `gh pr merge --rebase`. Cleans up branch (checkout main, pull, delete remote branch).
- **[v3] Failure recovery**: The ship agent executes in three checkpointed stages. If merge fails after force-push, the `rollback` sub-agent runs to restore the remote branch. If branch deletion fails after a successful merge, the failure is logged to the audit log and the user is notified — the merge is not rolled back.
- **Allowed**: `READ_ONLY`, `GIT_STATUS`, `GIT_BRANCH`, `GIT_PUSH`, `GIT_REBASE`, `GH_PR`, `gh pr merge`, `git push origin --delete`
- **Disallowed**: `NEVER_ALLOW`, `Write`, `Edit`, `Task`, commit
- **Sub-agents**: `rollback` [v3] (on failure only)
- **Hooks**: `PreToolUse` → `gh_auth_hook`

### Orchestrator

#### `flow` — End-to-End Implementation Lifecycle

- **Model**: sonnet | **CLI**: `gravitee-dev flow`
- **Role**: Chains all implementation agents sequentially: implement → docs-sync → copilot-review → (human review gate) → ship. Assumes `/gh-init` has run (branch, issue, plan) and `/gh-create-pr` will be invoked between docs-sync and copilot-review via the skill layer. Verifies success before proceeding. Tracks cumulative cost across agents and halts if a budget threshold is exceeded. Reports per-phase errors and stops on failure.
- **[v3] Concurrency lock**: Acquires `.gravitee/gravitee.lock` on start. If the lock file exists, flow exits immediately with an error listing the PID and start time from the lock file. Lock is released (deleted) on exit, including on unhandled exceptions.
- **[v3] Intra-agent budget monitoring**: The `flow` orchestrator passes a per-phase budget cap derived from `max_budget_usd / num_phases` to each agent. An agent that exceeds its phase budget is interrupted before the next phase boundary, not after.
- **Sub-agents**: `implement`, `docs-sync`, `copilot-review`, `pr-review`, `ship`
- **Human gate**: Final review before merge
- **Allowed**: Broadest tool set — all presets plus build tools
- **Disallowed**: `NEVER_ALLOW`

> **Note**: `flow` begins at `implement`, not planning. Planning is always done in the Claude Code conversation layer before `flow` is invoked.

---

## 6. Hook System

Seven programmatic hooks compose into agent specs via `HookMatcher`.

### `protect_files_hook` — PreToolUse

- **Triggers on**: `Edit` / `Write` tool calls
- **Function**: Blocks edits to files matching protected patterns
- **Default patterns**: `.env`, `.pem`, `.key`, `credentials.json`, lock files, `.git/`, build artifacts, IDE config
- **Extensible**: Pattern list configurable per-project via `.gravitee/policy.yaml` (key: `protected_paths`). Falls back to defaults if no policy file found.
- **Used by**: `implement`

### `guard_tests_hook` — PreToolUse

- **Triggers on**: `Edit` / `Write` tool calls
- **Function**: Blocks edits to **committed** test files. Untracked test files (RED phase) are allowed.
- **Test patterns**: `*.test.*`, `*.spec.*`, `test_*`, `*_test.*`, `*Test.java`, `*Tests.java`, `*Spec.java`, `*IT.java` and Kotlin equivalents
- **Detection**: Uses `git ls-files` to check if the file is tracked
- **Used by**: `implement`

### `check_stop_hook` — Stop

- **Triggers on**: Agent stop
- **Function**:
  1. Checks `git status --porcelain` for changed files
  2. If no changes → allows stop
  3. Runs configurable build check (default: `task check`, override via `CHECK_COMMAND` env var)
  4. **[v3]** Verifies test coverage has not dropped below project baseline (`COV_FAIL_UNDER` env var, default: matches `pyproject.toml` setting)
  5. If build passes and coverage holds → spawns commit sub-agent → allows stop
  6. If build fails on first entry → blocks stop, feeds errors back
  7. If build fails on re-entry (`stop_hook_active=True`) → allows stop (prevents infinite loops)
- **Commit sub-agent spawning**: Spawned via `query()` using `ClaudeAgentOptions`. **[v3] `disallowed_tools` pass-through is now test-enforced** — see `test_hooks.py::test_check_stop_hook_passes_disallowed_tools`.
- **Used by**: `implement`, `docs-sync`, `pr-review`, `copilot-review`

### `clean_working_tree_hook` — UserPromptSubmit

- **Triggers on**: Agent start (before first LLM turn)
- **Function**: Blocks if working tree has uncommitted changes. Lists dirty files.
- **[v3] Fail-closed**: On subprocess errors (timeout, OS error), **blocks start** and reports the error. Previously failed open — this was unsafe because `create-pr` can push whatever is on disk. A push with unknown working tree state is worse than a failed start.
- **Used by**: `create-pr`

### `gh_auth_hook` — PreToolUse

- **Triggers on**: First tool call that invokes `gh` CLI
- **Function**: Runs `gh auth status` to verify GitHub CLI is authenticated. Blocks with a clear message if not, listing the required setup steps.
- **Fail behavior**: Hard block — does not fail open. An unauthenticated `gh` CLI makes the agent useless, so failing loudly is better than failing mid-flow after side effects have occurred.
- **Used by**: `create-pr`, `pr-review`, `copilot-review`, `ship`

### `secret_scan_hook` — PreToolUse **[v3]**

- **Triggers on**: `Edit` / `Write` tool calls and `Bash(git add *)` / `Bash(git commit *)` calls
- **Function**: Scans the content being written (or the staged diff) for secret patterns. Uses [gitleaks](https://github.com/gitleaks/gitleaks) if available; falls back to a built-in regex set covering common patterns (AWS keys, GH tokens, private key headers, generic high-entropy strings above threshold).
- **Fail behavior**: Hard block on detection. Reports the matched pattern type (not the value) and the file path. Does not expose the secret in the hook output.
- **Fail-closed on tool error**: If gitleaks is unavailable and the built-in scanner errors, blocks and reports. Install gitleaks or set `SKIP_SECRET_SCAN=1` (requires explicit opt-out).
- **Used by**: `implement`, `docs-sync`, `pr-review`, `copilot-review`

### `validate_plan_hook` — UserPromptSubmit **[v3]**

- **Triggers on**: Agent start of `implement`
- **Function**: Delegates to the `validate-plan` sub-agent (schema check + checksum verify). Blocks start if validation fails.
- **Used by**: `implement`

---

## 7. Tool Permission System

### Composable Presets (`tools/presets.py`)

```python
GIT_STATUS  = ["Bash(git status *)", "Bash(git diff *)", "Bash(git log *)"]
GIT_COMMIT  = [*GIT_STATUS, "Bash(git add *)", "Bash(git commit *)", "Bash(git reset HEAD *)"]
GIT_BRANCH  = [*GIT_STATUS, "Bash(git checkout *)", "Bash(git branch *)", "Bash(git fetch *)", "Bash(git pull *)"]
GIT_PUSH    = ["Bash(git push *)"]
GIT_REBASE  = ["Bash(git rebase *)", "Bash(git push origin HEAD --force-with-lease)"]

GH_ISSUE    = ["Bash(gh issue *)", "Bash(gh search *)"]
GH_PR       = ["Bash(gh pr *)", "Bash(gh api *)", "Bash(gh auth status)"]

READ_ONLY   = ["Read", "Glob", "Grep"]
FILE_WRITE  = ["Write", "Edit"]
TASK_TOOL   = ["Task"]

# [v3] NEVER_ALLOW expanded to cover build-tool-mediated code execution,
# privilege escalation, and filesystem destruction.
NEVER_ALLOW = [
    "Bash(rm -rf *)",
    "Bash(sudo *)",
    "Bash(curl *)",
    "Bash(wget *)",
    "WebFetch",
    "WebSearch",
    # [v3] additions:
    "Bash(python -c *)",          # inline script execution
    "Bash(python3 -c *)",
    "Bash(node -e *)",            # inline JS execution
    "Bash(bash -c *)",            # shell injection via bash -c
    "Bash(sh -c *)",
    "Bash(eval *)",
    "Bash(chmod +x *)",           # making files executable
    "Bash(pip install *)",        # arbitrary package installation
    "Bash(pip3 install *)",
    "Bash(npm install -g *)",     # global npm package installation
    "Bash(git filter-branch *)",  # history rewriting
    "Bash(git push --force *)",   # force push without lease
    "Bash(env *)",                # env var injection
    "Bash(export *)",             # shell env mutation
]

# [v3] BUILD_TOOLS: explicitly enumerated safe build invocations.
# Prefer this over open-ended Bash(*) access for build commands.
BUILD_TOOLS = [
    "Bash(uv run *)", "Bash(uv sync)", "Bash(uv build)",
    "Bash(pnpm run *)", "Bash(pnpm test *)", "Bash(pnpm build *)",
    "Bash(npm run *)", "Bash(npm test)", "Bash(npm build)",
    "Bash(mvn *)", "Bash(task *)", "Bash(cargo build *)",
    "Bash(cargo test *)", "Bash(make *)", "Bash(ruff check *)",
    "Bash(ruff format *)", "Bash(pytest *)",
]
```

> **[v3] Note on build tool scope**: The `implement` agent previously had unrestricted build tool access defined as open `Bash(uv *)` patterns. This has been replaced with `BUILD_TOOLS`, an explicit allowlist of safe invocations. This prevents an agent from executing `uv add malicious-package` or `npm install <injected>` under the guise of a build step.

Note: `gh auth status` is included in `GH_PR` so the `gh_auth_hook` can run the check without requiring a separate permission.

### Permission Matrix

| Agent | Git R | Git W | Git Push | Git Rebase | GH Issue | GH PR | Read | Write | Task | Build Tools |
|---|---|---|---|---|---|---|---|---|---|---|
| commit | `GIT_COMMIT` | — | — | — | — | — | Read | — | — | — |
| safety-check | `GIT_STATUS` | checkout, pull | — | — | — | — | — | — | — | — |
| validate-plan [v3] | `GIT_STATUS` | — | — | — | — | — | Read | — | — | — |
| rollback [v3] | `GIT_STATUS` | — | `--force-with-lease` only | — | — | — | Read | — | — | — |
| implement | `GIT_COMMIT` | — | — | — | — | — | yes | yes | yes | `BUILD_TOOLS` [v3] |
| docs-sync | `GIT_STATUS`, `GIT_COMMIT` | — | — | — | — | — | yes | yes | yes | — |
| create-pr | `GIT_STATUS` | — | yes | — | — | yes | yes | yes | — | — |
| pr-review | `GIT_COMMIT` | — | yes | — | — | yes | yes | yes | yes | — |
| copilot-review | `GIT_COMMIT` | — | yes | — | — | yes | yes | yes | yes | — |
| ship | `GIT_STATUS`, `GIT_BRANCH` | — | yes | yes | — | yes | yes | — | — | — |
| flow | `GIT_BRANCH`, `GIT_COMMIT` | — | yes | yes | — | yes | yes | yes | yes | `BUILD_TOOLS` [v3] |

All agents deny `NEVER_ALLOW`. Agents using GH_PR have `gh_auth_hook` as a guard. Agents with `FILE_WRITE` or `GIT_COMMIT` have `secret_scan_hook` [v3].

---

## 8. Runner & SDK Integration

### `run_agent()` — Core Execution Function

1. **[v3] Concurrency lock**: Acquires `.gravitee/gravitee.lock` before resolving sub-agents. Releases on all exit paths.
2. **Sub-agent resolution**: Recursively flattens nested sub-agents. Prevents cycles via a `_seen` set.
3. **Options composition**: Builds `ClaudeAgentOptions` with `permission_mode="bypassPermissions"`.
4. **SDK invocation**: Creates `ClaudeSDKClient`, streams responses via `client.receive_response()`.
5. **Output rendering**: Rich console — markdown for text, styled tool use/results, thinking blocks in italic blue.
6. **Result**: Returns `AgentResult` including `cost_usd` and `prompt_version`.
7. **[v3] Audit log**: Appends a structured record to `.gravitee/audit.jsonl` on every run completion (success or failure). See schema in Section 11.

### Flow Cost Tracking

The `flow` orchestrator accumulates `AgentResult.cost_usd` across all agent calls. If the running total exceeds `max_budget_usd` (set on the flow command or via `--max-budget`), the flow halts before invoking the next agent and reports the total spend.

**[v3]** Additionally, each agent is given a per-phase budget cap (`max_budget_usd / num_phases`). If an individual agent exceeds its cap, flow halts at the next phase boundary. This prevents a single runaway agent (e.g., `implement` on a large Opus run) from consuming the entire budget before other phases execute.

### `AgentSpec` → `AgentDefinition` Mapping

```python
AgentDefinition(
    description=spec.description,
    prompt=spec.prompt,
    model=spec.model if spec.model != "inherit" else None,
    tools=spec.allowed_tools if spec.allowed_tools else None,
    # [v3] REQUIRED — validated by test_hooks.py::test_check_stop_hook_passes_disallowed_tools
    disallowed_tools=spec.disallowed_tools,
)
```

---

## 9. CLI Interface

The CLI covers the **implementation layer only**. Planning happens in Claude Code chat.

| Command | Agent | Default Prompt | Required Args |
|---|---|---|---|
| `gravitee-dev implement [PROMPT]` | implement | "Continue implementation from task.md" | — |
| `gravitee-dev implement --issue N` | implement | — | GitHub issue number |
| `gravitee-dev create-pr [PROMPT]` | create-pr | "Push and create draft PR" | — |
| `gravitee-dev docs-sync [PROMPT]` | docs-sync | "Sync documentation with code changes" | — |
| `gravitee-dev pr-review [PROMPT]` | pr-review | "Address all open PR review comments" | — |
| `gravitee-dev copilot-review [PROMPT]` | copilot-review | "Request and iterate on Copilot review" | — |
| `gravitee-dev ship [PROMPT]` | ship | "Verify, rebase, merge, and cleanup" | — |
| `gravitee-dev flow TASK` | flow | — | task description |
| `gravitee-dev commit [PROMPT]` | commit | "Commit current changes" | — |
| `gravitee-dev list` | — | — | — |

### Global Options

All commands accept: `--cwd / -C`, `--model / -m` (sonnet/opus/haiku), `--max-budget`, `--quiet / -q`, `--pause-after <phase>`.

The `--pause-after` flag on `flow` allows the user to insert a human checkpoint after any named phase (e.g. `--pause-after implement`) without modifying agent specs. **[v3]** This flag is strongly recommended for any plan with more than 5 checklist items or that touches security-sensitive code.

**[v3]** `--skip-copilot` flag on `flow` allows the Copilot review phase to be bypassed if it has timed out or is unavailable, proceeding directly to human review. The skip is recorded in the audit log.

---

## 10. Workflow

### Path 1 — Planning (Claude Code Layer)

```
① User opens Claude Code conversation
   │  Describes the feature in natural language
   │
   ▼
② Back-and-forth refinement
   │  Claude asks clarifying questions about requirements,
   │  edge cases, constraints, and acceptance criteria
   │  User refines until the plan is solid
   │
   ▼
③ User approves the plan
   │  [plan-skill] is triggered
   │
   ├─── GitHub Issue  →  [gh-plugin] creates issue, returns #N
   │
   ├─── Local file    →  [filesystem-plugin] writes implementation_plan.md
   │
   └─── Both (recommended)
          Issue created first, number embedded in plan file header
          [v3] SHA-256 checksum of plan written to .gravitee/plan.lock
   │
   ▼
④ User creates feature branch (manual or skill-assisted)
      git checkout -b feature/...
```

### Path 2 — Implementation (Agent SDK Layer)

```
gravitee-dev implement  (or  gravitee-dev flow)
        │
        ├─ Source A: reads implementation_plan.md from cwd
        └─ Source B: gh issue view #N  (--issue N flag)
        │
        ▼
① [v3] validate-plan sub-agent
   │  Schema check: required sections + checklist items present
   │  Checksum verify: matches .gravitee/plan.lock
   │  Injection check: no known prompt-injection patterns
   │
   ▼
② safety-check sub-agent
   │  Verifies clean working tree
   │  git checkout main, git pull
   │
   ▼
③ TDD loop — iterates through plan task checklist
   │
   │  For each task:
   │  ┌──────────────────────────────────────────────────────┐
   │  │  a. Write failing test (RED)                         │
   │  │     guard_tests_hook: allows (file untracked)        │
   │  │     secret_scan_hook: scans new test file [v3]       │
   │  │                                                      │
   │  │  b. Write implementation (GREEN)                     │
   │  │     protect_files_hook: blocks sensitive paths       │
   │  │     guard_tests_hook: blocks edits to committed      │
   │  │     test files                                       │
   │  │     secret_scan_hook: scans implementation [v3]      │
   │  │                                                      │
   │  │  c. Verify: task check (ruff + pytest)               │
   │  │     [v3] coverage check: must not drop below         │
   │  │     COV_FAIL_UNDER baseline                          │
   │  └──────────────────────────────────────────────────────┘
   │
   ▼
④ Stop hook
   │  No changes → allow stop
   │  Changes → run task check + coverage check [v3]
   │    PASS → spawn commit sub-agent (haiku, with explicit disallowed_tools)
   │              secret_scan_hook on staged diff [v3]
   │              git diff --cached to verify staged files
   │              semantic commit message
   │            → allow stop
   │    FAIL (first entry) → block, feed errors back
   │    FAIL (re-entry)    → allow stop (prevent infinite loop)
   │
   ▼
⑤ Task marked complete, loop continues
```

### Full Feature Lifecycle (`gravitee-dev flow`)

```
[PLANNING — Claude Code conversation]
  User ←──────────────── Claude
  Approves plan
  [plan-skill] → GitHub Issue #N + implementation_plan.md
  [v3] → SHA-256 checksum in .gravitee/plan.lock
        │
        ▼
[IMPLEMENTATION — gravitee-dev flow]
[v3] .gravitee/gravitee.lock acquired

① Validate Plan                                          [Autonomous] [v3]
   schema check → checksum verify → injection check
        │
        ▼
② Implement                                              [Autonomous]
   safety-check → TDD loop → coverage gate → auto-commits
        │  [v3] --pause-after implement recommended for large tasks
        ▼
③ Docs Sync                                              [Autonomous]
   git diff origin/main --stat → surgical doc updates
        │
        ▼
④ Create PR                                              [Autonomous]
   gh_auth_hook → clean tree check (fail-closed) → push → draft PR
   Links Closes #N in PR body
        │
        ▼
⑤ Copilot Review                                         [Autonomous]
   Request → poll (configurable interval/timeout)
   Verify feedback retrieved → fix/reject → re-request
   Mark PR ready
   [v3] On timeout → HALT with error (not silent pass-through)
        │  (or --skip-copilot to bypass with audit log entry)
        ▼
⑥ Human Review                                           [Human gate]
   PR presented for approval
        │
        ▼
⑦ Ship                                                   [Autonomous]
   gh_auth_hook → verify approvals + CI
   rebase → force-push → merge → branch cleanup
   [v3] On failure after force-push → rollback sub-agent runs
   [v3] All steps recorded in audit log

[v3] .gravitee/gravitee.lock released
```

### Shared Artifacts

| Artifact | Created by | Used by | Purpose |
|---|---|---|---|
| `implementation_plan.md` | [plan-skill] | implement, create-pr | Problem, approach, and unified checklist |
| GitHub issue body | [gh-plugin] | implement (--issue N) | Same structure — problem, approach, checklist |
| `walkthrough.md` | create-pr | copilot-review | Change documentation |
| **[v3]** `.gravitee/plan.lock` | [checksum-plugin] | validate-plan | SHA-256 integrity seal for plan document |
| **[v3]** `.gravitee/gravitee.lock` | runner.py | flow | Concurrency guard — prevents parallel flow runs |
| **[v3]** `.gravitee/audit.jsonl` | runner.py | humans, tooling | Append-only structured log of all agent actions |

---

## 11. Data Schemas

### `AgentSpec` — Agent Definition

```python
@dataclass(frozen=True)
class AgentSpec:
    name: str
    description: str
    prompt: str
    prompt_version: str                                     # e.g. "1.0.0" — for traceability
    layer: Literal["sub", "workflow", "orchestrator"]
    model: Literal["sonnet", "opus", "haiku", "inherit"] = "sonnet"
    allowed_tools: list[str] = field(default_factory=list)
    disallowed_tools: list[str] = field(default_factory=list)
    sub_agents: list[str] = field(default_factory=list)
    cli_command: str | None = None
    max_turns: int | None = None
    max_budget_usd: float | None = None
    hooks: dict[HookEvent, list[HookMatcher]] = field(default_factory=dict)
```

Note: `field(default_factory=list)` replaces bare `= []` to prevent cross-agent mutation bugs.

### `RunConfig` — Runtime Configuration

```python
@dataclass(frozen=True)
class RunConfig:
    cwd: Path | None = None
    model: Literal["sonnet", "opus", "haiku"] | None = None
    max_turns: int | None = None
    max_budget_usd: float | None = None
    quiet: bool = False
    pause_after: str | None = None                          # Phase name to pause after
    skip_copilot: bool = False                              # [v3] Bypass copilot-review phase
```

### `AgentResult` — Run Output

```python
@dataclass(frozen=True)
class AgentResult:
    agent_name: str
    result_text: str
    is_error: bool = False
    cost_usd: float | None = None
    duration_ms: float | None = None
    prompt_version: str | None = None                       # Prompt version used
    metadata: dict[str, str] = field(default_factory=dict)
```

### `AuditRecord` — Audit Log Entry **[v3]**

Appended to `.gravitee/audit.jsonl` (one JSON object per line) on every agent run completion.

```python
@dataclass(frozen=True)
class AuditRecord:
    timestamp: str                  # ISO 8601
    agent_name: str
    prompt_version: str | None
    model: str
    cwd: str
    is_error: bool
    cost_usd: float | None
    duration_ms: float | None
    tool_calls: list[str]           # list of tool names invoked (not arguments)
    hooks_triggered: list[str]      # list of hook names that fired a block
    outcome: str                    # "success" | "error" | "blocked" | "budget_exceeded" | "timed_out"
    metadata: dict[str, str]
```

### `PlanLock` — Checksum Record **[v3]**

Written to `.gravitee/plan.lock` by the planning skill on plan approval.

```json
{
  "schema_version": 1,
  "sha256": "<hex digest of implementation_plan.md content>",
  "issued_at": "2025-01-01T12:00:00Z",
  "issue": 42
}
```

---

## 12. Code Patterns

### Agent Definition Pattern

```python
from gravitee_dev.agents._base import AgentSpec
from gravitee_dev.tools.presets import READ_ONLY, FILE_WRITE, GIT_COMMIT, NEVER_ALLOW, BUILD_TOOLS
from dataclasses import field

PROMPT_VERSION = "1.0.0"

PROMPT = """\
# Agent Name
...
"""

SPEC = AgentSpec(
    name="agent-name",
    description="Short description for CLI help.",
    prompt=PROMPT,
    prompt_version=PROMPT_VERSION,
    layer="workflow",
    model="sonnet",
    allowed_tools=[*READ_ONLY, *FILE_WRITE, *GIT_COMMIT, *BUILD_TOOLS],  # [v3] BUILD_TOOLS replaces open Bash(*)
    disallowed_tools=[*NEVER_ALLOW],
    hooks={"Stop": [HookMatcher(hooks=[check_stop_hook])]},
    cli_command="agent-name",
)
```

### Hook: `gh_auth_hook`

```python
async def gh_auth_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    if tool_name and "gh" not in (tool_name or ""):
        return SyncHookJSONOutput()  # only check on gh tool calls
    try:
        result = subprocess.run(
            ["gh", "auth", "status"],
            capture_output=True, timeout=10
        )
        if result.returncode != 0:
            return SyncHookJSONOutput(
                decision="block",
                reason="GitHub CLI is not authenticated. Run `gh auth login` first."
            )
    except Exception:
        return SyncHookJSONOutput(
            decision="block",
            reason="Could not verify GitHub CLI authentication. Ensure `gh` is installed."
        )
    return SyncHookJSONOutput()
```

### Hook: `secret_scan_hook` **[v3]**

```python
async def secret_scan_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    content = hook_input.tool_input.get("content") or hook_input.tool_input.get("new_content", "")
    if not content:
        return SyncHookJSONOutput()

    # Try gitleaks first
    try:
        result = subprocess.run(
            ["gitleaks", "detect", "--no-git", "--pipe"],
            input=content.encode(),
            capture_output=True,
            timeout=15,
        )
        if result.returncode != 0:
            return SyncHookJSONOutput(
                decision="block",
                reason="Potential secret detected by gitleaks. Review the content before proceeding.",
            )
    except FileNotFoundError:
        # gitleaks not installed — fall back to built-in scanner
        if _builtin_secret_scan(content):
            return SyncHookJSONOutput(
                decision="block",
                reason="Potential secret pattern detected. Install gitleaks for full scanning.",
            )
    except subprocess.TimeoutExpired:
        # [v3] Fail closed — scanner timeout is treated as a block
        return SyncHookJSONOutput(
            decision="block",
            reason="Secret scanner timed out. Set SKIP_SECRET_SCAN=1 to bypass (requires explicit opt-out).",
        )

    return SyncHookJSONOutput()
```

### Sub-Agent Spawning with Explicit `disallowed_tools`

```python
# Inside check_stop_hook — must pass disallowed_tools explicitly
# [v3] This is now enforced by test_hooks.py::test_check_stop_hook_passes_disallowed_tools
options = ClaudeAgentOptions(
    model="sonnet",
    permission_mode="bypassPermissions",
    agents={"commit": commit_agent},
    allowed_tools=commit_spec.allowed_tools,
    disallowed_tools=commit_spec.disallowed_tools,   # REQUIRED — not auto-propagated
    cwd=cwd,
)
```

### Concurrency Lock Pattern **[v3]**

```python
import os, json
from pathlib import Path
from contextlib import contextmanager

@contextmanager
def flow_lock(cwd: Path):
    lock_path = cwd / ".gravitee" / "gravitee.lock"
    lock_path.parent.mkdir(exist_ok=True)
    if lock_path.exists():
        info = json.loads(lock_path.read_text())
        raise RuntimeError(
            f"Another flow is running (PID {info['pid']}, started {info['started_at']}). "
            f"If this is stale, delete {lock_path} manually."
        )
    lock_path.write_text(json.dumps({"pid": os.getpid(), "started_at": datetime.utcnow().isoformat()}))
    try:
        yield
    finally:
        lock_path.unlink(missing_ok=True)
```

---

## 13. Testing

| Test File | Coverage |
|---|---|
| `test_cli.py` | CLI smoke tests: no-args help, `list` command, all commands have help |
| `test_runner.py` | Sub-agent resolution: no subs, single, nested, cycles, unknown, deduplication |
| `test_hooks.py` | All hooks including `gh_auth_hook`, `secret_scan_hook`, `validate_plan_hook`; **[v3]** `test_check_stop_hook_passes_disallowed_tools` asserts explicit disallowed_tools in every hook that spawns a sub-agent |
| `test_base.py` | AgentSpec frozen, field defaults (no mutable defaults), prompt_version present |
| `test_presets.py` | Tool permission preset composition; **[v3]** asserts `NEVER_ALLOW` covers all expanded patterns |
| `test_flow_budget.py` | Flow halts when cumulative cost exceeds max_budget_usd; **[v3]** per-phase budget cap enforcement |
| **[v3]** `test_plan_validation.py` | validate-plan sub-agent: valid plan passes, missing sections fail, bad checksum fails, injection patterns fail |
| **[v3]** `test_secret_scan.py` | secret_scan_hook: known patterns blocked, clean content passes, gitleaks fallback, timeout fails closed |
| **[v3]** `test_concurrency_lock.py` | Lock acquired on flow start, released on exit, raises on concurrent run, stale lock detection |
| **[v3]** `test_audit_log.py` | AuditRecord written on success, error, and block; append-only; fields populated correctly |
| **[v3]** `test_clean_working_tree.py` | Fails closed on subprocess error (not open as in v2) |

---

## 14. Roadmap

### Phase 1 — Complete

- All implementation agents with scoped permissions
- 4 programmatic hooks with test coverage
- Composable tool presets
- Runner with sub-agent resolution and Rich output
- Typer CLI for every implementation agent
- Full test suite

### Phase 2 — In Progress (this revision)

- [x] Two-layer architecture clearly defined
- [ ] `plan` skill (Claude Code) with gh and filesystem plugins
- [ ] **[v3]** `checksum-plugin` writes `.gravitee/plan.lock` on plan approval
- [ ] `gh_auth_hook` on all GH-touching agents
- [ ] `disallowed_tools` pass-through in `check_stop_hook` **(now test-enforced)**
- [ ] `git diff --cached` verification step in commit sub-agent
- [ ] `prompt_version` field in `AgentSpec` and `AgentResult`
- [ ] `field(default_factory=list)` on all mutable AgentSpec defaults
- [ ] Cumulative cost circuit-breaker in `flow`
- [ ] **[v3]** Per-phase budget cap in `flow`
- [ ] `--pause-after` flag on `flow`
- [ ] Configurable `protect_files_hook` patterns via `.gravitee/policy.yaml`
- [ ] `COPILOT_POLL_INTERVAL` / `COPILOT_TIMEOUT` env vars on copilot-review
- [ ] **[v3]** `secret_scan_hook` (gitleaks + built-in fallback) on all write/commit agents
- [ ] **[v3]** `validate-plan` sub-agent (schema + checksum + injection check)
- [ ] **[v3]** `validate_plan_hook` on `implement` start
- [ ] **[v3]** `rollback` sub-agent in `ship`
- [ ] **[v3]** `clean_working_tree_hook` changed to fail closed
- [ ] **[v3]** Copilot review timeout halts flow (not silent pass-through)
- [ ] **[v3]** `BUILD_TOOLS` preset replaces open `Bash(*)` in `implement` and `flow`
- [ ] **[v3]** `NEVER_ALLOW` expanded to cover inline execution and package installation
- [ ] **[v3]** `.gravitee/gravitee.lock` concurrency guard in `flow`
- [ ] **[v3]** `.gravitee/audit.jsonl` append-only audit log
- [ ] **[v3]** `--skip-copilot` flag on `flow`
- [ ] **[v3]** Test coverage gate in `check_stop_hook` (`COV_FAIL_UNDER`)

### Open Directions

| Area | Notes |
|---|---|
| Structured state file | `state.json` for crash recovery and multi-session resumption. The plan checklist is the current approximation — re-running `implement` skips completed items. |
| Flow rollback (full) | The `rollback` sub-agent handles the `ship` phase. Broader compensating actions per phase (e.g. closing a draft PR on `create-pr` failure) are still all-or-nothing. |
| Spec phase | Currently planning is open-ended conversation. A structured spec interview template in the skill could improve plan consistency and reduce injection surface area. |
| Multi-user support | The concurrency lock prevents parallel runs from the same working directory. Distributed team scenarios (multiple developers, same repo) are not yet handled. |

---

## 15. Risks & Open Questions

### v3 Change Summary

The following risks from v2 have been fully or partially mitigated in this revision:

| Risk ID | Description | v2 Status | v3 Status |
|---|---|---|---|
| R1 | `bypassPermissions` single point of failure | Documented | Documented + defence-in-depth principle added; fail-closed posture on all safety hooks |
| R2 | Prompt injection via plan document | **Not in register** | Mitigated: `validate-plan` schema check + checksum + injection pattern detection |
| R3 | `disallowed_tools` propagation | Mitigated (docs) | Mitigated: test-enforced invariant |
| R4 | Secret scanning absent | Open Direction | **Mitigated**: `secret_scan_hook` promoted to Phase 2, mandatory |
| R5 | `NEVER_ALLOW` too narrow | **Not in register** | Mitigated: expanded to cover inline execution, package install, force push |
| R6 | No crash recovery / rollback | Open | Partially mitigated: `rollback` sub-agent for `ship` phase; other phases still all-or-nothing |
| R7 | TDD quality gap (weak tests) | **Not in register** | Mitigated: coverage gate in `check_stop_hook` |
| R8 | `ship` force-push inconsistent state | **Not in register** | Mitigated: `rollback` sub-agent on failure |
| R9 | Context exhaustion | MED (open) | Unchanged; `--pause-after implement` recommended |
| R10 | `flow` broadest permissions, least scrutiny | **Not in register** | Mitigated: `BUILD_TOOLS` replaces open Bash access; `NEVER_ALLOW` expanded |
| R11 | Budget circuit breaker only at phase boundary | **Not in register** | Mitigated: per-phase budget cap |
| R12 | `clean_working_tree_hook` fails open | **Not in register** | Mitigated: now fails closed |
| R13 | Copilot timeout ambiguous state | MED (open) | Mitigated: explicit halt + `--skip-copilot` opt-out |
| R14 | `safety-check` can change branches on dirty tree | **Not in register** | Mitigated: `validate_plan_hook` and `clean_working_tree_hook` (fail-closed) gate before `safety-check` |
| R15 | No audit log | **Not in register** | Mitigated: `.gravitee/audit.jsonl` append-only log |
| R16 | Single human gate | Documented | Partially mitigated: `--pause-after implement` strongly recommended; gate remains optional |
| R17 | Plan documents unversioned / no schema | **Not in register** | Mitigated: `plan-schema-version` marker, `validate-plan` sub-agent |
| R18 | No concurrency protection | **Not in register** | Mitigated: `.gravitee/gravitee.lock` |

### Updated Risk Register

| Severity | Risk | Mitigation |
|---|---|---|
| **HIGH** | `gh` CLI not authenticated mid-flow | `gh_auth_hook` blocks before first GH tool call on all relevant agents. |
| **HIGH** | `bypassPermissions` removes OS-level safety net | Defence-in-depth: all safety hooks fail closed; `NEVER_ALLOW` expanded; `BUILD_TOOLS` scoped allowlist. No single hook failure grants full access. |
| **MED** | Plan document tampered between approval and execution | `validate-plan` sub-agent verifies SHA-256 checksum against `.gravitee/plan.lock` before any code is written. |
| **MED** | Prompt injection via plan document | `validate-plan` checks for known injection patterns. Does not guarantee detection of novel patterns — human review of plan content before approval remains important. |
| **MED** | Build check loops infinitely | `check_stop_hook` re-entry guard allows stop on second failure. |
| **MED** | Context exhaustion on large tasks | No checkpoint/resume yet. `--pause-after implement` flag allows mid-flow human checkpoint. Checklist checkboxes allow manual restart. |
| **MED** | Copilot review timeout exits without processing feedback | Timeout now causes explicit flow halt. `--skip-copilot` allows bypass with audit log entry. |
| **MED** | Budget overrun across a full flow | Cumulative cost tracking in `flow` with configurable `--max-budget` halt. Per-phase caps added. |
| **LOW** | `ship` inconsistent state after failed force-push | `rollback` sub-agent attempts to restore remote branch. If rollback also fails, audit log records last known SHA. |
| **LOW** | Concurrent flow runs against same working directory | `.gravitee/gravitee.lock` prevents parallel flows. |
| **LOW** | Hook subprocess timeout | All subprocess calls have timeouts. Safety-critical hooks (secret scan, clean tree) fail closed. Non-critical hooks log and continue. |
| **LOW** | Weak tests committed (low coverage) | `check_stop_hook` coverage gate blocks commits that drop coverage below `COV_FAIL_UNDER` baseline. |
| **OPEN** | No full flow rollback beyond `ship` phase | Compensating actions for `create-pr`, `docs-sync`, and `copilot-review` failures are not yet implemented. |
| **OPEN** | Multi-user / distributed team concurrency | The lock file is local. Two developers on different machines can still run flows against the same remote simultaneously. |

### Open Questions

| # | Question | Current State |
|---|---|---|
| Q1 | Should the `plan` skill also create the feature branch? | Currently left to the user. Could be optional skill behavior. |
| Q2 | Should `implement --issue N` also update the issue on completion? | Not implemented. Would close the loop between planning and implementation layers. |
| Q3 | Should cost be reported per-phase in the flow summary? | `AgentResult.cost_usd` exists per-run. Flow only enforces total budget and per-phase caps, doesn't yet report a per-phase breakdown in the summary output. |
| Q4 | Should the flow support partial resumption? | Currently all-or-nothing. The checklist in `implementation_plan.md` (or the issue body) allows manual restart by re-running `implement` — it will skip already-checked items. `--pause-after` helps with control but not crash recovery. |
| Q5 | **[v3]** Should the injection check in `validate-plan` be configurable? | Currently uses a hardcoded pattern list. A project-level allowlist in `.gravitee/policy.yaml` could reduce false positives for plans that legitimately reference system-level concepts. |
| Q6 | **[v3]** Should `.gravitee/audit.jsonl` be committed to the repo? | Currently written to the working directory but not staged. Committing it would create a permanent record but could expose cost data in public repos. |

---

## Dependencies

### Planning Layer

| Component | Purpose |
|---|---|
| Claude Code | Conversation runtime and skill host |
| `gh-plugin` | GitHub issue creation from conversation |
| `filesystem-plugin` | Local file write from conversation |
| **[v3]** `checksum-plugin` | SHA-256 plan integrity seal |

### Implementation Layer

| Package | Version | Purpose |
|---|---|---|
| `claude-agent-sdk` | latest | Core SDK — `ClaudeSDKClient`, `ClaudeAgentOptions`, `AgentDefinition`, hooks |
| `rich` | >=14.3.2 | Terminal output |
| `typer` | >=0.15 | CLI framework |
| `pytest` | >=9.0.2 | Test framework (dev) |
| `pytest-asyncio` | >=0.25 | Async test support (dev) |
| `pytest-cov` | >=6.0 | **[v3]** Coverage enforcement in `check_stop_hook` |

**External tools**: Python 3.12+, `uv`, `gh` CLI (authenticated), `task` (Taskfile runner), `ruff`, **[v3]** `gitleaks` (recommended; built-in fallback available).