# Claude Code Resources

A curated collection of resources for **[Claude Code](https://claude.com/code)**, the agentic coding tool from Anthropic.

This directory mirrors the `~/.claude/` layout — `rules/` and `skills/` at the top level — so it's immediately recognizable and nearly drop-in. Domain-specific skill bundles and guardrail hooks are distributed as **plugins** via the built-in marketplace.

> **Warning** — This configuration is **opinionated** and tailored for the author's personal workflow. The install script symlinks everything into `~/.claude/` (user scope), meaning it will apply to **every project** you open with Claude Code. If you prefer narrower impact, consider copying the files you need into your project's `.claude/` directory instead, where they will only apply to that specific repository.

```
claude-code/
├── .claude-plugin/            # marketplace manifest
│   └── marketplace.json
├── gravitee-dev/            # composable CLI agents (programmatic hooks + SDK)
├── plugins/                   # plugin bundles (skills + guardrail hooks)
│   ├── gh-flow/               # workflow: issue → ship lifecycle
│   ├── gravitee-java/         # scaffolding: Gravitee Java projects
│   ├── protect-files/         # guardrail: block edits to sensitive files
│   ├── guard-tests/           # guardrail: TDD test immutability
│   └── check-build/           # guardrail: build check + auto-commit
├── rules/                     # domain-specific rule files
├── skills/                    # standalone skills (not bundled in a plugin)
```

---

## Plugin Marketplace

Five plugins are available — two skill bundles and three guardrail hooks — for centralized discovery and installation.

### Adding the marketplace

```bash
/plugin marketplace add git@github.com:gravitee-io/gravitee-devic-framework-samples.git
```

### Installing a plugin

```bash
/plugin install gh-flow@gravitee-marketplace
/plugin install gravitee-java@gravitee-marketplace
/plugin install protect-files@gravitee-marketplace
/plugin install guard-tests@gravitee-marketplace
/plugin install check-build@gravitee-marketplace
```

### Available plugins

| Plugin | Category | Type | Description |
| --- | --- | --- | --- |
| [`gh-flow`](plugins/gh-flow/) | workflow | skills (10) | Agentic GitHub Flow: orchestrated dev lifecycle from issue to ship |
| [`gravitee-java`](plugins/gravitee-java/) | scaffolding | skills (9) | Gravitee Java project scaffolding: Maven, CircleCI, plugin structure, Kafka policy |
| [`protect-files`](plugins/protect-files/) | guardrail | hook | Blocks Edit/Write on sensitive files (secrets, lock files, build artifacts) |
| [`guard-tests`](plugins/guard-tests/) | guardrail | hook | TDD guardrail: blocks edits to committed test files |
| [`check-build`](plugins/check-build/) | guardrail | hook | Stop hook: build check + auto-commit on green, error feedback on red |

### gh-flow

End-to-end development lifecycle — from issue to shipping — with multi-model review cycles.

| Skill | Description |
| --- | --- |
| [`gh-super-flow`](plugins/gh-flow/skills/gh-super-flow/) | Orchestrated end-to-end dev flow with configurable AI model reviews |
| [`gh-init`](plugins/gh-flow/skills/gh-init/) | Initialize work on a new feature or bug fix |
| [`gh-implement`](plugins/gh-flow/skills/gh-implement/) | Execute implementation plan with TDD and atomic commits |
| [`gh-create-pr`](plugins/gh-flow/skills/gh-create-pr/) | Push local work and create draft PR |
| [`gh-ship`](plugins/gh-flow/skills/gh-ship/) | Verify completion, rebase, merge, and cleanup |
| [`gh-docs-sync`](plugins/gh-flow/skills/gh-docs-sync/) | Sync project documentation with code changes |
| [`gh-issue-triage`](plugins/gh-flow/skills/gh-issue-triage/) | Triage, prioritize, and organize repository issues |
| [`gh-pr-review-cycle`](plugins/gh-flow/skills/gh-pr-review-cycle/) | Automated PR review cycle with feedback integration |
| [`gh-retro`](plugins/gh-flow/skills/gh-retro/) | Retrospective, next work selection, and context handoff |
| [`gh-setup-ci`](plugins/gh-flow/skills/gh-setup-ci/) | Create GitHub Actions CI workflow (build, lint, test) |

### gravitee-java

| Skill | Description |
| --- | --- |
| [`gravitee-init-project`](plugins/gravitee-java/skills/gravitee-init-project/) | Initialize a new Gravitee Java project with standard tooling |
| [`gravitee-create-maven-base`](plugins/gravitee-java/skills/gravitee-create-maven-base/) | Scaffold Maven project with Gravitee standards |
| [`gravitee-add-circleci`](plugins/gravitee-java/skills/gravitee-add-circleci/) | Add standard Gravitee CircleCI configuration |
| [`gravitee-add-github-files`](plugins/gravitee-java/skills/gravitee-add-github-files/) | Add Gravitee GitHub templates (PR, Renovate, CODEOWNERS) |
| [`gravitee-add-gitignore`](plugins/gravitee-java/skills/gravitee-add-gitignore/) | Add standard Gravitee .gitignore for Java Maven |
| [`gravitee-add-license`](plugins/gravitee-java/skills/gravitee-add-license/) | Add Apache 2.0 LICENSE for Gravitee projects |
| [`gravitee-add-plugin-structure`](plugins/gravitee-java/skills/gravitee-add-plugin-structure/) | Add Gravitee plugin files (properties, assembly, schemas) |
| [`gravitee-add-prettier`](plugins/gravitee-java/skills/gravitee-add-prettier/) | Add standard Gravitee .prettierrc for Java formatting |
| [`gravitee-kafka-native-policy`](plugins/gravitee-java/skills/gravitee-kafka-native-policy/) | Scaffold a Gravitee Kafka native policy |

---

## Quick Install

```bash
# From the repository root — symlinks rules/ and skills/ to ~/.claude/
./install-claude.sh
```

This will:
1. Back up your existing `~/.claude/` configuration
2. Symlink every rule from `rules/` into `~/.claude/rules/`
3. Symlink every skill from `skills/` into `~/.claude/skills/`

Guardrail hooks are now installed as plugins via the marketplace (see above).

---

## Rules

Modular coding standards and conventions, each loaded by the agent when working in the corresponding domain.

| Rule | Domain |
| --- | --- |
| [`agents.md`](rules/agents.md) | Agent construction, A2A protocol, orchestrator patterns |
| [`angular.md`](rules/angular.md) | Angular 20+: signals, standalone, typed forms, control flow, DI |
| [`frontend.md`](rules/frontend.md) | Frontend design system (Gravitee palette & typography) |
| [`git.md`](rules/git.md) | Agentic GitHub Flow: branching, commits, PR lifecycle |
| [`java.md`](rules/java.md) | Java 21+, Maven, Gravitee architectural patterns |
| [`llm.md`](rules/llm.md) | Prompting strategies, structured output, model selection |
| [`python.md`](rules/python.md) | Type hints, docstrings, ML/pipeline patterns |
| [`tdd.md`](rules/tdd.md) | Constraint-based TDD as an alignment guardrail |
| [`typescript.md`](rules/typescript.md) | TypeScript typing standards, syntax, libraries |

---

## Skills

18 standalone skills organized by domain. Each is a directory with a `SKILL.md` and optional supporting resources.

### Plan & Code Review Cycles

Multi-model review skills — stress-test plans and code using different AI perspectives.

| Skill | Description |
| --- | --- |
| [`plan-cycle-claude`](skills/plan-cycle-claude/) | Stress-test implementation plans with Claude Code CLI |
| [`plan-cycle-codex`](skills/plan-cycle-codex/) | Stress-test implementation plans with Codex CLI |
| [`plan-cycle-gemini`](skills/plan-cycle-gemini/) | Stress-test implementation plans with Gemini CLI |
| [`review-cycle-claude`](skills/review-cycle-claude/) | Automated code review loop with Claude Code CLI |
| [`review-cycle-codex`](skills/review-cycle-codex/) | Automated code review loop with Codex CLI |
| [`review-cycle-gemini`](skills/review-cycle-gemini/) | Automated code review loop with Gemini CLI |
| [`review-cycle-copilot`](skills/review-cycle-copilot/) | Address Copilot review feedback in loop |

### Multi-Model Collaboration

CLI-powered mixture-of-experts patterns.

| Skill | Description |
| --- | --- |
| [`brainstorm-collab`](skills/brainstorm-collab/) | Collaborative brainstorming with Council of Models |
| [`repo-analysis-collab`](skills/repo-analysis-collab/) | Deep multi-model repository analysis — Council of Models |

### Project Scaffolding: Next.js

| Skill | Description |
| --- | --- |
| [`init-next-project`](skills/init-next-project/) | Initialize a new Next.js project with standard tooling |
| [`create-nextjs-base`](skills/create-nextjs-base/) | Create a Next.js project with TypeScript, Tailwind, App Router, Turbopack |
| [`add-oss-boilerplate`](skills/add-oss-boilerplate/) | Add open-source docs (Apache 2.0, CoC, Contributing) |
| [`add-vitest`](skills/add-vitest/) | Add Vitest with React Testing Library and JSDOM |
| [`configure-eslint-prettier`](skills/configure-eslint-prettier/) | Configure ESLint (Flat Config) + Prettier |
| [`configure-package-scripts`](skills/configure-package-scripts/) | Add standard dev scripts to package.json |

### Project Scaffolding: A2A Agents

| Skill | Description |
| --- | --- |
| [`init-agents`](skills/init-agents/) | Add A2A agent infrastructure to a new or existing project |
| [`add-a2a-infrastructure`](skills/add-a2a-infrastructure/) | Add A2A protocol-compliant agent infra with LangGraph + Express |

### Semantic Commits

| Skill | Description |
| --- | --- |
| [`semantic-commit-messages`](skills/semantic-commit-messages/) | Write semantic commit messages following Gravitee changelog conventions |

### Bug Triage

| Skill | Description |
| --- | --- |
| [`bug-triage`](skills/bug-triage/) | Reproduce a bug, capture evidence, analyze, and produce an issue/PR-ready summary |

### Newsletter

| Skill | Description |
| --- | --- |
| [`two-tips-and-a-gotcha`](skills/two-tips-and-a-gotcha/) | Draft the next edition of the agentic coding field guide newsletter |

---

## Guardrail Plugins

Deterministic guardrails that run automatically as hooks — no LLM discretion involved.

**Two approaches:**
1. **Plugin hooks** (this directory) — packaged as plugins for install via the marketplace
2. **Programmatic hooks** (`gravitee-dev/`) — Python functions composed directly into agent specs via the Claude Agent SDK

### protect-files

`PreToolUse` hook on `Edit|Write` — blocks edits to sensitive files (secrets, lock files, build artifacts, IDE config). Patterns are defined in [`protected-patterns.conf`](plugins/protect-files/scripts/protected-patterns.conf) — one substring pattern per line, checked against the full file path.

### guard-tests

`PreToolUse` hook on `Edit|Write` — enforces TDD test immutability. Once a test file has been committed to git, this hook blocks edits so Claude must escalate to the developer before modifying acceptance tests. New (untracked) test files are always allowed (RED phase).

### check-build

`Stop` hook — fires every time the main Claude agent finishes responding. It:

1. Checks `git status` for changed files
2. If files changed, runs `task check` (requires [Taskfile](https://taskfile.dev/) in the project)
3. If the build passes, spawns a sandboxed commit sub-agent (`claude --print`) to create a semantic commit
4. If the build fails, blocks Claude from stopping and feeds the errors back for a fix loop
5. Guards against infinite loops via `stop_hook_active`

The commit sub-agent runs with restricted permissions defined in [`settings.commit.json`](plugins/check-build/scripts/settings.commit.json) — it can only read files and run git commands (no writes, no pushes, no web access). It uses the `sonnet` model for speed.

---

## Prerequisites

- [Claude Code](https://claude.com/code) installed and configured
- `gh` CLI ([GitHub CLI](https://cli.github.com/)) for GitHub-integrated skills
- For multi-model collaboration skills: `codex` and/or `gemini` CLIs installed
- [Taskfile](https://taskfile.dev/) set up in your project for the `check-hook.py` Stop hook
- [`uv`](https://docs.astral.sh/uv/) for running the Python-based Stop hook (uses `claude-agent-sdk` via inline script dependencies)

---

## Official Documentation

- [Claude Code Overview](https://docs.anthropic.com/en/docs/agents-and-tools/claude-code/overview)
- [Claude Code Best Practices](https://docs.anthropic.com/en/docs/agents-and-tools/claude-code/best-practices)
- [Claude Skills (Beta)](https://docs.anthropic.com/en/docs/agents-and-tools/claude-code/skills)
