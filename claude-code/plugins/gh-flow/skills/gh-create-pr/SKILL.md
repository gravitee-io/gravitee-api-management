---
name: gh-create-pr
description: Push local work and create draft PR
---

Use this skill to transition from local development to GitHub — the first time your work becomes visible on GitHub.

## Prerequisites

- Local branch exists with at least one commit
- Implementation is complete (`/gh-implement` finished)
- All tests passing
- All lint/format checks passing

## Steps

### 1. Safety Checks

Verify the branch is ready to push:

```bash
# Verify tests pass
pnpm test  # or npm test, mvn test, cargo test, etc.

# Verify no uncommitted changes
git status
```

**IF** there are uncommitted changes or failing tests: **STOP**. Fix issues before proceeding.

### 2. Push to GitHub

Push the local branch to origin for the first time:

```bash
git push -u origin HEAD
```

### 3. Read Implementation Plan

Load the content of `implementation_plan.md` (from artifacts or project root) to include in the PR body.

### 4. Create Draft PR

Create a draft Pull Request using `gh pr create`:

- `--draft`
- `--title`: Use the issue title or a descriptive summary
- `--body`: Format as:

    ```markdown
    Closes #<issue_number>

    ## Implementation Plan

    <content_of_implementation_plan.md>
    ```

- `--head`: Current branch name
- `--base`: `main` (or appropriate default branch)

### 5. Assign PR to Self

Assign yourself as the PR **Assignee** (not Reviewer) using `gh pr edit` with your username in the `--add-assignee` flag.

> [!TIP]
> If you don't know the user's GitHub username, use the repo owner or ask them.

### 6. Create Initial Walkthrough

Create `walkthrough.md` (Artifact) documenting what was built:

- **What Changed**: High-level summary of the implementation
- **Files Modified**: List of files added/modified/deleted with brief descriptions
- **What Was Tested**: Test strategy and results
- **Verification**: How correctness was validated (test output, manual checks, etc.)

Keep it concise — this is the initial snapshot. It will be updated during subsequent review cycles.

### 7. Report Completion

```
Create PR complete:
- Branch pushed to GitHub
- Draft PR #<number> created: <URL>
- Initial walkthrough documented

Next: Run `/review-cycle-copilot` to enter Copilot review
```

---

## Notes

- **First Push**: This is the only time the branch is pushed to a new remote. Subsequent pushes during review cycles use `git push origin HEAD` (no `-u` flag).
- **Draft State**: The PR stays in draft until review cycles complete and mark it ready for review.
- **Walkthrough Evolution**: The walkthrough created here is the baseline. Review cycles add findings and fixes.
