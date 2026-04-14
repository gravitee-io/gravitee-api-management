---
name: gh-docs-sync
description: Sync project documentation with code changes
---

Use this skill to ensure project documentation stays aligned with code changes. Can be run standalone on any branch, or as a phase within `/gh-super-flow`.

> [!WARNING]
> **This skill is NOT optional.** When called as part of `/gh-super-flow`, you MUST execute the full assessment (Steps 1-2). The _outcome_ may be "no updates needed" — that's fine — but you may never skip the assessment itself. Stale docs erode trust in the project and waste reviewer time.

## When to Use

- **After implementation + local review** — code is final, docs may be stale
- **Before opening/updating a PR** — so reviewers (human and Copilot) see accurate docs
- **Ad-hoc** — when you suspect docs have drifted, run this independently on any branch

## Steps

### 1. Assess What Changed

Understand the scope of code changes to determine which docs might be affected:

```bash
git diff origin/main --stat
```

Look for changes that typically require doc updates:

| Change Type                               | Docs to Check                                         |
| ----------------------------------------- | ----------------------------------------------------- |
| New public API / CLI flag / config option | README, API docs, usage examples                      |
| Changed function signatures or behavior   | README examples, inline module docs                   |
| New dependencies added                    | README (installation/setup), CONTRIBUTING             |
| Removed or renamed features               | README, migration guides, any reference to old names  |
| New files or modules                      | README (project structure), module-level doc comments |
| Changed build/test/dev workflow           | README (development section), CONTRIBUTING            |
| New environment variables or secrets      | README (configuration section)                        |

### 2. Read Relevant Docs

For each potentially affected doc file:

1. Read the current content
2. Cross-reference against the actual code changes
3. Note any stale, missing, or inaccurate sections

**Common doc files to check:**

- `README.md` — the most common drift target
- `CONTRIBUTING.md` — if build/test/setup changed
- Module-level doc comments (e.g., Rust `//!`, JSDoc `@module`, Javadoc package-info)
- `docs/` directory if one exists
- API documentation or OpenAPI specs

### 3. Update

For each stale section found:

1. Update the documentation to reflect current code behavior
2. Keep the existing tone and style of the document
3. Don't rewrite sections that are still accurate — surgical updates only

> [!IMPORTANT]
> This is a **sync** step, not a rewrite. Only touch what's actually stale. If the README structure needs a major overhaul, that's a separate task — file an issue for it.

### 4. Commit

If changes were made, commit them following the `/semantic-commit-messages` skill:

```bash
git add <doc-files>
git commit -m "docs: update README to reflect <what changed>"
```

If no changes were needed, note "docs check passed, no updates needed" and move on.

---

## Guidelines

- **Be surgical** — update what's stale, don't rewrite the world
- **Preserve voice** — match the existing doc's tone and formatting
- **Examples matter most** — if code examples in docs are wrong, that's the highest priority fix
- **Don't over-document** — if a change is purely internal (private functions, test-only code), docs probably don't need updating
- **When in doubt about a specific section, skip that section** — if you're unsure whether a particular doc section needs updating, err on the side of not touching it. Unnecessary doc changes add noise to PRs. But always complete the full assessment (Steps 1-2) before concluding nothing needs updating.
