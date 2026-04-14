---
name: gravitee-cherry-pick
description: Cherry-pick bugfix commits to multiple support branches and open GitHub PRs with Jira references
---

# Cherry-Pick Bugfix to Support Branches

You are tasked with cherry-picking one or more bugfix commits to multiple versioned support branches (e.g., `3.x`, `4.x`, `4.8.x`, `4.9.x`) and opening a GitHub PR for each.

## Step 1 — Gather context

Run the following in parallel:

```bash
git branch --show-current            # current branch name
git log --oneline -10                # recent commits
gh repo view --json defaultBranchRef,name,owner   # repo info
```

Then list available support branches (versioned `*.x` branches and any `alpha*` branches):
```bash
git branch -r --list 'origin/*.x' | sed 's|origin/||' | sort -V
git branch -r --list 'origin/alpha*' | sed 's|origin/||' | sort
```

Present this info to the user clearly.

## Step 2 — Select commits

Ask the user:
> "Which commits should be cherry-picked? You can specify:
> - A list of SHAs: `abc1234 def5678`
> - A range: `abc1234^..def5678`
> - A count from HEAD: `last 3 commits`
>
> Current recent commits:"

Show the last 20 commits (`git log --oneline -20`) so the user can pick.

Confirm the selected commits and their messages before proceeding.

## Step 3 — Select target branches

Present the available support branches as a numbered list, like:

```
Available support branches:
  1. 3.x
  2. 4.8.x
  3. 4.9.x
  4. 4.10.x
  5. 5.x

Which branches should receive the cherry-pick?
Enter numbers separated by spaces (e.g. "1 3 4"), or "all" for all branches.
```

Wait for the user's selection, then confirm the chosen branches before proceeding.

## Step 4 — Jira issue

Ask the user:
> "Is there a Jira issue for this bugfix? If so, provide the issue key (e.g., `APIM-1234`) and I'll include the link in each PR description."

If provided, construct the Jira URL. Ask the user for their Jira base URL if not already known (e.g., `https://gravitee.atlassian.net`).

Store: `JIRA_KEY`, `JIRA_URL` (full link).

## Step 5 — Detect build/test command

Check for a build tool in the repository root (in priority order):

```bash
[ -f pom.xml ] && echo "maven" || \
[ -f build.gradle ] && echo "gradle" || \
[ -f package.json ] && echo "npm" || \
echo "unknown"
```

Map to commands:
- `maven` → compile: `mvn compile -q`, test: `mvn test -q`
- `gradle` → compile: `./gradlew compileJava -q`, test: `./gradlew test`
- `npm` → compile: `npm run build`, test: `npm test`
- `unknown` → ask user for their compile and test commands

Ask the user:
> "Should I run compilation and tests after each cherry-pick to validate the result? (Recommended — helps detect conflicts that resolved cleanly but broke the build)"

## Step 6 — Process each target branch

For each target branch in the list, execute the following sequence. Handle them **one at a time** (not in parallel) to keep interactive conflict resolution manageable.

### 6a — Prepare branch

```bash
git fetch origin <target-branch>
git checkout <target-branch>
git pull --ff-only origin <target-branch>
```

Create a cherry-pick branch:
```bash
git checkout -b cherry-pick/<JIRA_KEY>/<target-branch>
# If no Jira: cherry-pick/bugfix/<short-description>/<target-branch>
```

### 6b — Cherry-pick commits

```bash
git cherry-pick <sha1> [<sha2> ...]
```

**If cherry-pick exits with conflicts:**

1. Show the conflicted files: `git status`
2. For each conflicted file, read it and show the conflict markers to the user
3. Ask the user how to resolve each conflict, OR offer to resolve it yourself if the intent is clear from context
4. After resolution: `git add <resolved-files>` then `git cherry-pick --continue`
5. If the user wants to skip a commit: `git cherry-pick --skip`
6. If the user wants to abort: `git cherry-pick --abort` and skip this target branch

### 6c — Validate (if user opted in)

Run compile:
```bash
<compile-command>
```

If compile fails:
- Show the error output
- Ask the user: "Compilation failed on `<target-branch>`. Do you want to (1) fix the issue now, (2) skip this branch, or (3) continue without validation?"
- If fix: help the user fix it, then re-run compile

Run tests (only if compile succeeded):
```bash
<test-command>
```

If tests fail:
- Show failing test names and errors
- Ask: "Tests failed on `<target-branch>`. Do you want to (1) fix, (2) push anyway (with a note in the PR), or (3) skip this branch?"

### 6d — Format and push

For Maven projects, run the code formatter before pushing to ensure the commit passes pre-commit hooks:
```bash
mvn prettier:write -q
git add -u
git commit --amend --no-edit
```

Then push:
```bash
git push origin cherry-pick/<JIRA_KEY>/<target-branch>
```

### 6e — Open PR

Create the PR body dynamically:

```
## Summary

Cherry-pick of bugfix from `<source-branch>` to `<target-branch>`.

Commits cherry-picked:
<list each commit SHA and message>

## Jira

[<JIRA_KEY>](<JIRA_URL>)   ← only if Jira was provided

## Notes

<if any conflicts were resolved, describe them here>
<if tests were skipped or failed, note it>
```

Create the PR:
```bash
gh pr create \
  --base <target-branch> \
  --head cherry-pick/<JIRA_KEY>/<target-branch> \
  --title "fix(<target-branch>): <original-commit-title> [<JIRA_KEY>]" \
  --body "$(cat <<'EOF'
<body>
EOF
)"
```

Capture and display the PR URL.

## Step 7 — Summary

After all branches are processed, print a summary table:

| Target Branch | Status | Conflicts? | Tests | PR |
|---------------|--------|-----------|-------|-----|
| `4.8.x` | ✅ Done | No | Passed | <url> |
| `4.9.x` | ✅ Done | Yes (resolved) | Passed | <url> |
| `3.x` | ⚠️ Skipped | — | — | — |

Return to the original branch at the end:
```bash
git checkout <original-branch>
```

## Important Notes

- **Never force-push** to support branches or the cherry-pick branches without explicit user confirmation.
- **Never skip `--no-verify`** on commits unless the user explicitly asks.
- If a cherry-pick branch already exists remotely, ask the user before overwriting.
- Always confirm the final list of commits and target branches with the user **before starting** any git operations.
- Keep the user informed of progress after each branch completes — do not silently batch all branches.
- If the user provides no Jira key, use a short description slug in branch names (e.g., `cherry-pick/fix-null-pointer/4.8.x`).
