# Agentic GitHub Flow

We follow a structured "Vibe-Coding" / Pair-Programming approach that mirrors standard GitHub Flow but adapted for agentic workflows.

**Critical Rule**: ALL work must be tracked. No loose commits.

## Workflow Steps

### 1. Work Initiation & Tracking

- **Start with an Issue**: Every task, no matter how small, begins with a GitHub Issue.
    - _Agent Action_: Check if an issue exists. If not, create one using `gh search issues` / `gh issue create`.
    - **Self-Assignment**: IMMEDIATELY assign the current authenticated user to the Issue.
- **Branch Safety**: Before creating a branch, verify clean state and current branch.
    - _Agent Action_: Always pull latest `main` before branching: `git checkout main && git pull origin main`.

### 2. Branching Strategy

- Create a new branch for the specific task.
    - Format: `user/feature-name` or `user/fix-some-bug`.
    - Example: `your-username/refactor-auth` or `your-username/fix-login-bug`.

### 3. Implementation Loop

- **Verify Locally**: Run builds, tests, and linters _before_ pushing.
- **STOP & Review**: Before committing or pushing, you MUST present your changes to the user and wait for explicit approval.
    - _Agent Action_: Show plan updates, diffs, or results, then ask for confirmation.
- **Iterate**: Make changes, update plan, verify again.

### 4. Commit Messages

We use **semantic commits** with single-line messages. Keep commits atomic and focused.

**Format**: `<type>(<scope>): <short description>`

**Types**:

- `feat`: New feature
- `fix`: Bug fix
- `chore`: Maintenance (deps, config)
- `refactor`: Code restructuring
- `docs`: Documentation only
- `test`: Adding/updating tests

**Rules**:

- **Single line only** - no multi-line bodies or bullet lists
- Keep descriptions concise (~50 chars ideal, max 72)
- Scope is optional but recommended (e.g., `types`, `parser`, `runner`)
- Use imperative mood ("add feature" not "added feature")

**Examples**:

```
feat(types): add Arazzo enum types
feat(parser): implement YAML deserialization
fix(runner): handle null workflow inputs
chore: update Jackson dependencies
test(types): add serialization round-trip tests
```

### 5. Pull Request (PR)

- Create a Pull Request as soon as the branch is pushed.
- **Link**: Ensure the PR description links to the Issue (e.g., "Fixes #123").
- **Self-Assignment**: **MANDATORY**. Assign yourself as the owner/assignee of the PR.
- **Draft First**: Create PRs in draft state. Only mark ready for review after all verification steps pass.
- **Review**: Move out of Draft when ready.

### 6. Final Quality Check

- **Copilot Review**: Before considering the task "Complete", explicitly check if you can assign **GitHub Copilot** as a reviewer.
    - _Agent Action_: Use `gh pr edit --add-reviewer` if available.
- **Merge**: Only merge after approval and CI green.
