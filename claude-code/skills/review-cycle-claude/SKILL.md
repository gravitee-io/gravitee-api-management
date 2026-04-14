---
name: review-cycle-claude
description: Automated code review loop with Claude Code CLI
---

Use this skill to iterate on code review feedback from Claude Code CLI until all issues are addressed. This skill automates the agent <-> Claude review cycle, maintaining a single conversation context across multiple review iterations.

## Prerequisites

- Implementation is complete (typically after `/gh-implement`)
- Branch has commits ready for review
- `claude` CLI is available in the terminal and authenticated

## Claude Code CLI Quick Reference

| Command Pattern           | Purpose                     |
| ------------------------- | --------------------------- |
| `claude -p "prompt"`      | Initial review (print mode) |
| `claude -c -p "prompt"`   | Continue conversation       |
| `claude --print "prompt"` | Same as `-p`                |

> [!TIP]
> Claude automatically maintains project context. Passing `-c` generally resumes the last conversation in the current directory.

---

## Steps

### 1. Gather Context

- Run `git log --oneline origin/main..HEAD` to identify commits to review.
- Locate `implementation_plan.md` (either in artifacts or PR body).
- Run `git diff origin/main..HEAD --stat` to understand scope.

### 2. Initial Claude Review

Run the initial review and **establish the conversation** for subsequent iterations:

```bash
# Pipe diff or just ask Claude to review (it can read files)
# Ideally pipe key context or rely on its ability to read the repo context
claude -p \
  "Review all commits on this branch against origin/main. First read any implementation plan or README if available, then examine each changed file.

For each finding, provide:
- **Priority**: [P1] Critical / [P2] Important / [P3] Minor
- **File**: Absolute path and line range
- **Issue**: Clear description of the problem
- **Suggestion**: How to fix it

Focus on: correctness bugs, spec violations, test gaps, edge cases, security concerns.
Format findings as a numbered list." 2>/dev/null
```

### 3. Review Feedback Loop

**Start an autonomous loop to address all Claude findings:**

> [!IMPORTANT]
> Do NOT create implementation plans or request user review for each cycle. Proceed directly with analysis and fixes.

#### a. Parse and Analyze Each Finding

For each finding, perform a thorough analysis:

| Decision         | Criteria                              | Action                       |
| ---------------- | ------------------------------------- | ---------------------------- |
| **Fix**          | Valid issue, in scope, actionable     | Implement the fix            |
| **Out of Scope** | Valid but unrelated to current work   | Create GitHub issue to track |
| **Reject**       | False positive, not actually an issue | Document rationale           |

**Analysis checklist:**

- [ ] Read the relevant code and understand the context
- [ ] Verify Claude's understanding is correct
- [ ] Determine if fix introduces new risks
- [ ] Check if issue is covered by existing tests

#### b. Implement Fixes

For each **Fix** decision:

1. Make the code change following `/gh-implement` principles
2. Run build/test suite to confirm no regressions
3. Stage the change: `git add .`

For each **Out of Scope** decision:

1. Create a GitHub issue using `gh issue create`
2. Record the issue number in `walkthrough.md` and/or the PR description

For each **Reject** decision:

1. Document the rationale clearly

#### c. Commit Changes (invoke the `/semantic-commit-messages` skill)

After addressing findings, make **atomic single-line commits** for each logical fix:

```bash
git add <files>
git commit -m "fix(runner): validate user input before processing"
```

> [!IMPORTANT]
> **Single line only** — no multi-line bodies. Each fix gets its own atomic commit.

#### d. Continue Conversation for Follow-up Review

Use `claude -c -p` to continue the same conversation:

```bash
claude -c -p \
  "I addressed your review feedback in the last commit. Here's what I did:

[Paste summary of fixes, out-of-scope issues created, and rejected findings with rationale]

Please:
1. Verify the fixes are correct
2. Review my rationale for rejected findings
3. Do another thorough pass to catch anything you might have missed" 2>/dev/null
```

#### e. Loop

If Claude finds new issues, return to step (a). Continue until:

- All findings are addressed with fixes, OR
- All remaining findings have documented explanations, OR
- All remaining findings are tracked as out-of-scope issues

### 4. Finalization

Once the review cycle converges, update `task.md` with review notes:

- Total review cycles completed
- Summary of issues found by priority
- How each was resolved
- Any patterns or learnings

Report:

```
Review cycle complete:
- Cycles: N
- Findings: X fixed, Y tracked as issues, Z rejected
- All tests passing
- Ready for next phase
```

> [!NOTE]
> **For standalone use**: You may create a `walkthrough.md` artifact here for documentation purposes. **Within `/gh-super-flow`**: Walkthrough is created later during `/gh-create-pr`.

---

## Troubleshooting

| Issue                                  | Solution                                                    |
| -------------------------------------- | ----------------------------------------------------------- |
| Output cut off                         | Run again, or ask to "continue"                             |
| Claude agrees with everything          | Ask Claude to deeply inspect a specific complex file        |
| Claude implements instead of reviewing | Emphasize "READ-ONLY review" in the prompt                  |
| Feedback is too vague                  | Ask for specific file/section references and concrete fixes |
| Debate is going in circles             | Summarize positions, pick the one with stronger evidence    |
| Authentication errors                  | Run `claude login` in the terminal first                    |
