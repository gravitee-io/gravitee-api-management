---
name: gravitee-bump-version
description: Bump a Maven dependency version across support branches by comparing current versions against tags from a source repository, then create GitHub PRs for each branch that needs an upgrade
---

# Bump Maven Dependency Version Across Support Branches

You are tasked with detecting outdated Maven dependency versions across multiple support branches in the current repository, comparing them against available tags from a source repository, and opening a GitHub PR for each branch that requires an upgrade.

## Step 1 — Gather current repository context

Run in parallel:

```bash
git branch --show-current
gh repo view --json defaultBranchRef,name,owner
```

Then list all support branches:

```bash
# Versioned support branches (e.g. 3.x, 4.x, 4.9.x, 4.10.x, 5.x) — last 5 in reverse order
git branch -r --list 'origin/*.x' | sed 's|^[[:space:]]*origin/||' | sort -V | tail -5 | sort -rV

# Main / master
git branch -r | grep -E 'origin/(main|master)$' | sed 's|^[[:space:]]*origin/||'

# Pre-release branches
git branch -r | grep -E 'origin/(alpha|beta)' | sed 's|^[[:space:]]*origin/||' | sort -r
```

Present the full list to the user with a numbered index, versioned branches first (most recent at top), followed by main/master, then pre-release branches.

## Step 2 — Identify the source repository

Prompt the user:

> "Which repository should I look up for newer versions?
> You can provide:
> - A **local path** to the repository (e.g. `../gravitee-access-management`)
> - A **GitHub `owner/repo` slug** (e.g. `gravitee-io/gravitee-access-management`)
> - A **full GitHub URL**"

### If the user provides a local path

Resolve the remote URL:

```bash
git -C <local-path> remote get-url origin
```

Parse the output to extract `owner/repo`:
- SSH `git@github.com:owner/repo.git` → `owner/repo`
- HTTPS `https://github.com/owner/repo.git` → `owner/repo`

Confirm:
> "Detected source repository: `owner/repo`. Is that correct?"

### If the user provides a GitHub slug or URL

Normalize to `owner/repo` (strip `.git` suffix).

Store as `SOURCE_REPO`. Also store `SOURCE_LOCAL_PATH` if a local path was provided.

### Fetch and classify all tags from the source repository

```bash
gh api repos/<SOURCE_REPO>/git/refs/tags \
  --paginate \
  --jq '.[].ref | ltrimstr("refs/tags/")' \
  | sort -V
```

**Tag classification rules:**

A valid tag must start with an optional `v` followed by three numeric segments: `vN.N.N` or `N.N.N`. Everything after the third segment is a pre-release identifier.

- **Stable tag**: matches `^v?[0-9]+\.[0-9]+\.[0-9]+$` — no pre-release suffix.
  Examples: `3.21.0`, `v4.9.1`, `5.1.0`
- **Pre-release tag**: matches `^v?[0-9]+\.[0-9]+\.[0-9]+-.+$` — has a pre-release suffix such as `-alpha.3`, `-beta.1`, `-rc.2`.
  Examples: `6.0.0-alpha.3`, `5.1.0-alpha.6`, `4.10.0-rc.1`

Strip any leading `v` from all tags so versions are always bare semver (e.g. `1.2.3`, `6.0.0-alpha.3`).

Display the following as a sanity check:
- **Latest version per major** (highest tag in each major, including pre-releases — e.g. `1.9.9`, `2.2.6`, `3.2.4`, `4.4.2`, `5.0.3`, `6.0.0-beta.2`)
- **Latest stable tag overall** (highest version matching the stable pattern)
- **Latest pre-release tag overall** (highest version matching the pre-release pattern)

To compute latest-per-major, group all tags by their major version number and pick the semver maximum within each group (pre-releases count but rank lower than the same release without suffix).

## Step 3 — Select target branches

Present the full branch list as a numbered list:

```
Available branches:
  1. 3.x
  2. 4.8.x
  3. 4.9.x
  4. 4.10.x
  5. 4.x
  6. 5.x
  7. main
  8. alpha

Which branches should be checked for version bumps?
Enter numbers separated by spaces, "all" for all branches, or press Enter to select all.
```

Wait for the user's selection. Confirm before continuing.

## Step 4 — Detect the Maven artifact and locate the version in the current repository

### 4a — Read the source repository's Maven coordinates

If `SOURCE_LOCAL_PATH` is available, read directly:

```bash
grep -E '<groupId>|<artifactId>' <SOURCE_LOCAL_PATH>/pom.xml | head -10
```

Otherwise, fetch the root `pom.xml` from GitHub:

```bash
gh api repos/<SOURCE_REPO>/contents/pom.xml --jq '.content' | base64 -d \
  | grep -E '<groupId>|<artifactId>' | head -10
```

Extract the top-level `<groupId>` and `<artifactId>` of the source project.

Store as `SOURCE_GROUP_ID` and `SOURCE_ARTIFACT_ID`.

Display to the user:
> "Source library coordinates: `<SOURCE_GROUP_ID>:<SOURCE_ARTIFACT_ID>`"

### 4b — Locate the version declaration in the current repository's pom.xml

Fetch the root `pom.xml` of the default branch:

```bash
git fetch origin
git show origin/<default-branch>:pom.xml
```

Search in order:

**Strategy 1 — Version property** (most common in Gravitee projects):

Look for a `<properties>` entry whose tag name contains a slug derived from `SOURCE_ARTIFACT_ID`:

```bash
git show origin/<default-branch>:pom.xml \
  | grep -n '<.*version>' \
  | grep -i "<slug-from-SOURCE_ARTIFACT_ID>"
```

**Strategy 2 — Direct dependency block:**

```bash
git show origin/<default-branch>:pom.xml \
  | grep -n -A5 "<artifactId>${SOURCE_ARTIFACT_ID}</artifactId>"
```

Present all matches to the user and ask:

> "I found these version declarations. Which one should I bump?"

Store:
- `DEPENDENCY_KEY` — the XML element name (e.g. `gravitee-access-management.version`)
- `DETECTION_METHOD` — `property` or `direct`
- `POM_FILE` — relative path (default: `pom.xml`; may differ in multi-module projects)

## Step 5 — Read the current version on each selected branch

For each selected branch:

**Property method:**
```bash
git show origin/<branch>:<POM_FILE> \
  | grep -oP "(?<=<${DEPENDENCY_KEY}>)[^<]+"
```

**Direct dependency method:**
```bash
git show origin/<branch>:<POM_FILE> \
  | grep -A10 "<artifactId>${SOURCE_ARTIFACT_ID}</artifactId>" \
  | grep -oP "(?<=<version>)[^<]+"
```

Build a summary table:

| Branch | Current Version |
|--------|----------------|
| 4.8.x  | 3.20.5         |
| 4.9.x  | 3.21.0         |
| main   | 3.22.1         |
| alpha  | 6.0.0-alpha.2  |

If a branch has no matching entry, note `NOT FOUND` and skip it.

## Step 6 — Determine the target version for each branch

Apply these eligibility rules. The key distinction is **stable branches only consider stable tags**; **pre-release branches consider all tags**.

| Branch pattern   | Eligible source tags                                 | Rule |
|------------------|------------------------------------------------------|------|
| `N.M.x`          | Stable tags matching `N.M.*`                         | Latest patch in same minor, stable only |
| `N.x`            | Stable tags matching `N.*.*`                         | Latest minor+patch in same major, stable only |
| `main` / `master`| All stable tags                                      | Overall latest stable |
| `alpha`          | All tags (stable + pre-release)                      | Overall latest, including pre-releases |
| `beta`           | All tags (stable + pre-release)                      | Overall latest, including pre-releases |

**Semver ordering for pre-release versions** (per semver spec): a pre-release version has lower precedence than the associated normal version. For example: `5.1.0-alpha.6 < 5.1.0 < 5.2.0`.

For each branch:
1. Filter source tags to the eligible set using the rules above.
2. Find the maximum version in semver order.
3. `max > current` → **upgrade available**.
4. `max == current` → **already up to date** (skip).
5. No eligible tags → **cannot determine** (skip).

Present the plan before any changes:

```
Upgrade plan:
  4.8.x:  3.20.5        →  3.20.8        ✅ will upgrade
  4.9.x:  3.21.0        →  3.21.3        ✅ will upgrade
  4.10.x: 3.22.0        →  3.22.0        ✔  already up to date
  main:   3.22.1        →  3.22.1        ✔  already up to date
  alpha:  6.0.0-alpha.2 →  6.0.0-alpha.3 ✅ will upgrade

Proceed? (yes / edit / abort)
```

Allow the user to override individual target versions.

## Step 7 — Find the Jira reference for each target version

For each branch that will be upgraded, search the git history **between the previous tag and the target tag** in the source repository for Jira issue keys.

### 7a — Find the previous tag

From the full sorted tag list (already fetched in Step 2), find the tag immediately preceding `<target-version>` in semver order. Store it as `PREV_TAG`.

Example: for target `5.0.3`, `PREV_TAG` = `5.0.2`. For `6.0.0-alpha.6`, `PREV_TAG` = `6.0.0-alpha.5`.

### 7b — Collect all commit SHAs in the range

Use the GitHub compare API to get all commits introduced by the target tag (excludes commits already in `PREV_TAG`). Output one SHA per line (no array wrapper — avoids concatenation bugs):

```bash
gh api "repos/<SOURCE_REPO>/compare/<PREV_TAG>...<target-version>" \
  --jq '.commits[:100][].sha'
```

Store as `RANGE_COMMITS` (newline-separated).

### 7c — Search commit messages and associated PR bodies for Jira URLs

Search commit messages in the range:
```bash
gh api "repos/<SOURCE_REPO>/compare/<PREV_TAG>...<target-version>" \
  --jq '.commits[:100][].commit.message'
```

Then for each SHA in `RANGE_COMMITS`, fetch associated PR bodies using a `while read` loop (not `for`) to correctly iterate over newline-separated SHAs:

```bash
while IFS= read -r sha; do
  gh api repos/<SOURCE_REPO>/commits/$sha/pulls --jq '.[].body' 2>/dev/null
done <<< "$RANGE_COMMITS"
```

Search all collected text for Jira URLs first:
```
https://[a-z0-9-]+\.atlassian\.net/browse/[A-Z]{2,10}-[0-9]+
```

If no URLs found, fall back to bare key pattern:
```
[A-Z]{2,10}-[0-9]+
```

Collect **all** matches, deduplicate them. Extract unique Jira keys and base URL (e.g. `https://gravitee.atlassian.net/browse/APIM-1234` → key `APIM-1234`, base URL `https://gravitee.atlassian.net`).

### 7d — Result

- If **one** Jira URL is found: extract the key and base URL, store as `JIRA_KEY` and `JIRA_BASE_URL`. Announce:
  > "Found Jira reference **APIM-1234** in git history for `<target-version>`."
- If **multiple** Jira URLs or keys are found: list all of them and ask the user which one(s) to use:
  > "Found multiple Jira references in git history for `<target-version>`: APIM-1234, APIM-5678. Which should I use?"
  Use the user's selection as `JIRA_KEY`. Multiple keys may be included in the PR body (comma-separated).
- If only bare keys found (no URL): store as `JIRA_KEY`, ask for the Jira base URL if not already known.
- If no key found at all: ask the user:
  > "No Jira key found for version `<target-version>`. Enter one (e.g. `APIM-1234`) or press Enter to skip."

Build `JIRA_URL = <JIRA_BASE_URL>/browse/<JIRA_KEY>` for each selected key.

Note: If multiple branches share the same target version, reuse the same `JIRA_KEY` without re-fetching.

## Step 8 — Process each branch needing an upgrade

Handle branches **one at a time**.

### 8a — Create a bump branch

```bash
git fetch origin <target-branch>
git checkout -b bump/<dependency-slug>-<new-version>/<target-branch> origin/<target-branch>
```

`dependency-slug` = last segment of `SOURCE_REPO`, lowercased.
For pre-release versions that contain dots (e.g. `6.0.0-alpha.3`), replace dots with dashes in the branch name: `6.0.0-alpha-3`.

### 8b — Apply the version change

Use the Edit tool to do a precise replacement in `<POM_FILE>`.

**Property method:** replace `<DEPENDENCY_KEY>CURRENT_VERSION</DEPENDENCY_KEY>` with `<DEPENDENCY_KEY>TARGET_VERSION</DEPENDENCY_KEY>`.

**Direct dependency method:** find the `<dependency>` block for `SOURCE_ARTIFACT_ID` and replace its `<version>` value.

Verify:
```bash
git diff
```

Show the diff to the user. Wait for confirmation before committing.

### 8c — Commit the change

```bash
git add <POM_FILE>
```

Without Jira:
```bash
git commit -m "chore: bump <dependency-slug> from <current> to <target>"
```

With Jira:
```bash
git commit -m "chore: bump <dependency-slug> from <current> to <target>

Ref: <JIRA_KEY>"
```

### 8d — Run Maven formatter

```bash
mvn prettier:write -q
git add -u
git diff --cached --quiet || git commit --amend --no-edit
```

### 8e — Push

```bash
git push origin bump/<dependency-slug>-<new-version-safe>/<target-branch>
```

### 8f — Open the PR

```bash
gh pr create \
  --base <target-branch> \
  --head bump/<dependency-slug>-<new-version-safe>/<target-branch> \
  --title "chore(<target-branch>): bump <dependency-slug> from <current> to <target>" \
  --body "$(cat <<'EOF'
## Summary

Bumps **[<SOURCE_REPO>](https://github.com/<SOURCE_REPO>)** from `<current>` to `<target>` on branch `<target-branch>`.

## Changelog

See the [releases](https://github.com/<SOURCE_REPO>/releases) page for details.

## Jira

[<JIRA_KEY>](<JIRA_URL>)
EOF
)"
```

Omit the Jira section if no key was provided. Display the returned PR URL.

## Step 9 — Return to original branch

```bash
git checkout <original-branch>
```

## Step 10 — Summary

Print a final table:

| Branch | Current | Target | Status | PR |
|--------|---------|--------|--------|----|
| `4.8.x` | 3.20.5 | 3.20.8 | ✅ PR created | <url> |
| `4.9.x` | 3.21.0 | 3.21.3 | ✅ PR created | <url> |
| `4.10.x` | 3.22.0 | 3.22.0 | ⏭ Up to date | — |
| `main` | 3.22.1 | 3.22.1 | ⏭ Up to date | — |
| `alpha` | 6.0.0-alpha.2 | 6.0.0-alpha.3 | ✅ PR created | <url> |

## Important Rules

- **Maven only** — this skill handles only Maven `pom.xml` files.
- **Never force-push** without explicit user confirmation.
- **Never use `--no-verify`** unless the user explicitly asks.
- If a bump branch already exists remotely, ask the user before overwriting it.
- Always show `git diff` and wait for confirmation **before** each commit.
- In multi-module Maven projects, the version property may live in a BOM submodule — always read and modify the correct `pom.xml`.
- **Stable branches (`N.M.x`, `N.x`, `main`) never get bumped to pre-release versions**, even if a pre-release is the latest tag in the source repo.
- Do not batch all branches silently; keep the user informed after each branch completes.
