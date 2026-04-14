---
name: gh-ship
description: Verify completion, rebase, merge, and cleanup
---

Use this skill to merge the PR and clean up the branch after all reviews are complete.

## Prerequisites

- PR exists and is marked ready for review (not draft)
- All review feedback has been addressed
- Walkthrough is up-to-date

## Steps

### 1. Completion Check

Verify the PR is ready to merge:

- **Approvals**: Check that required reviews are approved using `gh pr view`.
- **CI Status**: Verify all required status checks pass using `gh pr checks`.

**IF NOT SATISFIED**: **STOP**. Notify the developer: "PR is not ready to merge — [missing approvals / failing checks]. Please resolve before continuing."

**IF SATISFIED**: Proceed to rebase.

### 2. Rebase & Conflict Check

Update local history to ensure a clean rebase:

```bash
git fetch origin main
git rebase origin/main
```

**IF Conflicts**:

- **STOP**. Alert the developer: "Merge conflicts detected during rebase. Please resolve them manually, then run `/gh-ship` again."

**IF Clean**:

- Force push the rebased branch:
    ```bash
    git push origin HEAD --force-with-lease
    ```

### 3. Merge

Use `gh pr merge` with the rebase strategy:

```bash
gh pr merge --rebase
```

### 4. Cleanup

```bash
# Switch to default branch
git checkout main

# Pull latest changes
git pull

# Verify integration — confirm branch commits are now in main history
git log --oneline -10

# Delete remote branch
git push origin --delete <branch_name>
```

### 5. Done

Report success:

```
Ship complete:
- PR #<number> merged successfully: <URL>
- Resulting commit: <SHA>
- Branch cleaned up
```
