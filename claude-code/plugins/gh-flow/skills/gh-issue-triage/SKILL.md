---
name: gh-issue-triage
description: Triage, prioritize, and organize repository issues
---

Use this skill to perform a comprehensive triage of all open issues in a repository. The goal is to assess each issue's effort, value/impact, and priority — then bucket related issues into logical groups (epics) for execution planning.

## Prerequisites

- `gh` CLI authenticated
- Repository has open issues to triage

---

## Steps

### 1. Gather All Open Issues

- Use `gh issue list --state open --limit 100` to collect **all** open issues.
- For each issue, call `gh issue view <number>` to read the full body.
- Collect labels, assignees, and any existing sub-issue relationships.

> [!TIP]
> If the repo has >100 open issues, paginate using `--limit` and `--page`.

### 2. Build Issue Inventory

Create a structured inventory of every open issue. For each issue, capture:

| Field          | Description                                          |
| -------------- | ---------------------------------------------------- |
| **#**          | Issue number                                         |
| **Title**      | Issue title                                          |
| **Labels**     | Current labels                                       |
| **Created**    | When it was opened                                   |
| **Staleness**  | Days since last update                               |
| **Assignee**   | Currently assigned to                                |
| **Sub-issues** | Parent/child relationships if any                    |
| **Summary**    | One-line summary of what the issue is actually about |

### 3. Assess Each Issue

For every issue, determine the following dimensions:

#### Effort Estimation

| Level  | Criteria                                              |
| ------ | ----------------------------------------------------- |
| **XS** | Typo, config tweak, one-liner — < 30 minutes          |
| **S**  | Small, well-scoped change — a few hours               |
| **M**  | Multi-file change, may need tests — 1-2 days          |
| **L**  | Cross-cutting concern, architectural touch — 3-5 days |
| **XL** | Major feature or refactor, multiple PRs — 1+ weeks    |

#### Value / Impact

| Level        | Criteria                                                        |
| ------------ | --------------------------------------------------------------- |
| **Critical** | Blocking users, data loss risk, security vulnerability          |
| **High**     | Significant UX improvement, major feature gap, reliability fix  |
| **Medium**   | Nice-to-have improvement, developer experience, tech debt       |
| **Low**      | Cosmetic, minor polish, edge case unlikely to affect most users |

#### Priority (derived from effort + value)

| Priority | Meaning                                                |
| -------- | ------------------------------------------------------ |
| **P0**   | Do immediately — high value, any effort                |
| **P1**   | Do next — high value, reasonable effort                |
| **P2**   | Plan for soon — medium value or high effort            |
| **P3**   | Backlog — low value or very high effort, revisit later |

> [!IMPORTANT]
> Challenge your own assumptions. Read the code context if an issue is ambiguous. Don't just go by the title — some innocuous-sounding issues hide significant complexity, and some scary-sounding ones are trivial.

### 4. Bucket Related Issues

Group issues into logical clusters based on:

- **Feature area** (e.g., authentication, API gateway, test infrastructure)
- **Type** (bug fixes, enhancements, tech debt, documentation)
- **Dependency chains** (issues that must be done in order)

For each bucket, determine:

- A descriptive name for the group
- Whether it warrants an **Epic** (parent issue) to track the group
- Suggested execution order within the bucket

> [!TIP]
> Look for hidden dependencies. Issue A might seem independent, but if it touches the same code as Issue B, they should be sequenced or combined.

### 5. Present Triage Report

Present a comprehensive triage report to the user, organized as follows:

#### Summary Dashboard

```
Total open issues: N
  P0 (Critical):  X
  P1 (High):      Y
  P2 (Medium):    Z
  P3 (Backlog):   W

Stale issues (>30 days no update): N
Unassigned issues: N
```

#### Issue Buckets

For each bucket, present a table:

| #   | Title | Effort | Value | Priority | Notes |
| --- | ----- | ------ | ----- | -------- | ----- |

#### Recommended Actions

- Issues to close (duplicates, stale, no longer relevant)
- Issues to merge (overlapping scope)
- Issues to split (too large, multiple concerns)
- Suggested epic structure for related groups
- Recommended execution order across buckets

### 6. Collaborative Refinement

- Discuss the triage results with the user.
- Adjust priorities, groupings, and effort estimates based on user's domain knowledge.
- The user knows the product roadmap and customer needs — defer to their judgment on value/impact when there's disagreement.

### 7. Apply Changes (on approval)

Once the user approves the triage:

- **Labels**: Apply priority and effort labels if the repo uses them.
- **Epics**: Create parent issues for each approved bucket using `gh issue create`.
- **Sub-issues**: Link related issues as sub-issues where supported.
- **Close**: Close any issues agreed to be duplicates or no longer relevant.
- **Update**: Add triage notes to individual issues if needed via `gh issue comment`.

> [!CAUTION]
> Do NOT apply any changes (labels, closures, epic creation) until the user explicitly approves the triage report. This step is always collaborative.

---

## Output

The final deliverable is:

1. A **triage report** presented in the IDE for discussion
2. **Organized issues** on GitHub (epics, sub-issues, labels) — applied only after user approval
