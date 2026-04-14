---
name: gravitee-plan
description: Define a new feature or fix through conversation, then output as a GitHub issue or a local plan file
---

Use this skill to turn a rough idea into a structured, validated implementation plan. The conversation refines requirements iteratively. Once the plan is approved, the user **chooses the output**: a GitHub issue or a local `implementation_plan.md`.

> This is a **Planning Layer** skill. It does not create branches, write code, or invoke agents.

## Steps

### 1. Understand the Request

Listen to the user's initial brief. Before asking questions, build context:

- Read `README.md`, `CONTRIBUTING.md`, any `docs/` directory
- Scan recent git history (`git log --oneline -20`)
- Check existing open issues (`gh issue list --state open --limit 10`)
- Scan related source files and test patterns relevant to the user's brief

### 2. Refine Requirements (Conversational Loop)

Ask clarifying questions **iteratively**. Do not rush to a plan. Cover all of these:

- **What problem does this solve?** — User-facing motivation, not implementation details.
- **What are the acceptance criteria?** — How do we know it's done?
- **Edge cases and constraints?** — Error handling, backwards compatibility, performance.
- **Dependencies?** — Other issues, external services, libraries.
- **What does "done" look like?** — Concrete, testable outcome.

Keep going until the user signals they are satisfied. Multiple rounds are expected.

### 3. Propose Structured Plan

Present the plan to the user for review using this exact structure:

```markdown
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

**Checklist rules:**
- Each item must be atomic and testable.
- Order by dependency (earlier items unblock later ones).
- Include test tasks explicitly (e.g., `add unit tests for X`).

Ask the user for **explicit approval** before proceeding.

### 4. Choose Output (Mandatory Gate)

> [!IMPORTANT]
> You **must** ask the user to choose. Do not assume one or the other.

Once the plan is approved, present this choice:

```
Plan approved. How would you like to output it?

1. **GitHub Issue** — Create a remote issue with this plan as the body.
   Best when: you want to track this work on GitHub, collaborate with others,
   or reference it from PRs.

2. **Local Plan File** — Write `implementation_plan.md` to the project root
   with an integrity checksum.
   Best when: you want to start implementing immediately, or the work is
   small/local and doesn't need an issue.
```

Wait for the user's explicit choice before proceeding to Step 5A or 5B.

### 5A. Output: GitHub Issue

Create the issue:

```bash
gh issue create \
  --title "<concise title>" \
  --body "$(cat <<'EOF'
<approved plan content>
EOF
)"
```

Assign it:

```bash
gh issue edit <number> --add-assignee @me
```

Report:

```
Issue created: #<number> — <URL>

Next steps:
- Run `/gh-init` to create a branch and set up local context
- Or run `/gh-plan` again and choose "Local Plan File" to also write
  implementation_plan.md linked to this issue
```

### 5B. Output: Local Plan File

If a GitHub issue exists for this work (created earlier or pre-existing), prepend the reference:

```markdown
<!-- Closes #42 -->
<!-- plan-schema-version: 1 -->
```

Write `implementation_plan.md` to the project root.

Then write the integrity checksum:

```bash
mkdir -p .gravitee
```

Compute the SHA-256 hash and write `.gravitee/plan.lock`:

```json
{
  "sha256": "<shasum -a 256 implementation_plan.md | cut -d' ' -f1>",
  "issued_at": "<ISO 8601 timestamp>",
  "issue": null
}
```

Set `"issue": N` instead of `null` if a GitHub issue is linked.

Report:

```
Plan written:
- implementation_plan.md (project root)
- .gravitee/plan.lock (integrity checksum)

Next steps:
- Run `/gh-init` to create a branch and issue (if not already done)
- Or run `gravitee-dev implement` to start the TDD implementation loop
```

## Notes

- The `<!-- plan-schema-version: 1 -->` marker is **required**. The `validate-plan` agent rejects plans without it.
- The `### Checklist` section with `- [ ]` items is **required**. It drives the TDD implementation loop.
- The `.gravitee/plan.lock` checksum (local plan only) is verified by `validate-plan` before implementation starts. Modifying the plan after this skill runs will cause a checksum mismatch.
- This skill does **not** create branches, write code, or start implementation.
