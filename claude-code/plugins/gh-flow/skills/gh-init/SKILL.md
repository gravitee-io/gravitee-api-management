---
name: gh-init
description: Initialize work on a new feature or bug fix
---

Use this skill to set up an issue, local branch, and architectural plan. All work stays local until `/gh-create-pr`.

## Steps

### 1. Safety Check

- Run `git status` to ensure the current branch is clean.
- **IF** dirty: Abort and warn the user.
- **IF** clean: Switch to default branch (`main`) and pull latest.

### 2. Branch

- Create local branch: `git checkout -b <type>/<short-description>`
- No empty commit or push needed — keep everything local until Ship.

### 3. Issue

- Check if a GitHub issue already exists for this work.
- **IF NOT**: Create one using `gh issue create`.
- **Assign**: Immediately assign the issue to yourself.
    > [!TIP]
    > If you do not know the user's GitHub username, ask them for it or use the `owner` of the repo if appropriate.

### 4. Context Hydration

> [!IMPORTANT]
> **Do NOT skip this step.** Before you plan anything, build a solid mental model of the problem space. Shallow context leads to shallow plans.

1. **Read the issue in full** — acceptance criteria, discussion thread, labels, linked issues
2. **Read project docs** — `README.md`, `CONTRIBUTING.md`, any `docs/` directory, module-level doc comments
3. **Scan related code** — the files, modules, and patterns relevant to the issue. Look at sibling implementations for conventions (naming, structure, error handling)
4. **Note constraints** — test patterns, CI requirements, dependency versions, anything that will shape the implementation

Only proceed to planning once you have a clear understanding of:

- What the issue is asking for
- How the existing code is structured
- What patterns and conventions to follow

### 5. Architectural Planning

- **Plan**: Create `implementation_plan.md` (Artifact) based on the context gathered.
- **Tasking**: Create `task.md` (Artifact).
- **Review**: Ask user for approval on the plan.
