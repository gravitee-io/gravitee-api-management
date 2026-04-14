---
name: review-cycle-gemini
description: Automated code review loop with Gemini CLI
---

Use this skill to iterate on code review feedback from Gemini CLI until all issues are addressed. This skill automates the agent <-> Gemini review cycle, maintaining a single conversation context across multiple review iterations.

## Prerequisites

- Implementation is complete (typically after `/gh-implement`)
- Branch has commits ready for review
- `gemini` CLI is available in the terminal
- `jq` is available in the terminal
- For issue tracking: `gh` CLI authenticated

## Gemini CLI Quick Reference

| Command Pattern                                                                                              | Purpose                      |
| ------------------------------------------------------------------------------------------------------------ | ---------------------------- |
| `gemini -m gemini-3-pro-preview --yolo -o json -p "prompt" 2>/dev/null \| jq -r '.response'`                 | Clean non-interactive prompt |
| `gemini -m gemini-3-pro-preview --yolo -o json --resume latest -p "prompt" 2>/dev/null \| jq -r '.response'` | Continue conversation        |

> [!IMPORTANT]
> Always use `-o json` + `jq -r '.response'` to suppress thinking/tool-call noise. Without this, Gemini's internal reasoning floods stdout and risks context collapse for the agent.

> [!TIP]
> Sessions are project-specific and auto-saved. Use `gemini --resume` or the interactive `/resume` command to browse past sessions. If `gemini-3-pro-preview` is unavailable, fall back to `-m pro`.

---

## Steps

### 1. Gather Context

- Run `git log --oneline origin/main..HEAD` to identify commits to review.
- Locate `implementation_plan.md` (either in artifacts or PR body).
- Run `git diff origin/main..HEAD --stat` to understand scope.

### 2. Initial Gemini Review

Run the initial review and **establish the conversation** for subsequent iterations:

```bash
gemini -m gemini-3-pro-preview --yolo -o json -p \
  "Review all commits on this branch against origin/main. First read any implementation plan or README, then examine each changed file.

For each finding, provide:
- **Priority**: [P1] Critical / [P2] Important / [P3] Minor
- **File**: Absolute path and line range
- **Issue**: Clear description of the problem
- **Suggestion**: How to fix it

Focus on: correctness bugs, spec violations, test gaps, edge cases, security concerns.
Format findings as a numbered list." 2>/dev/null | jq -r '.response'
```

### 3. Review Feedback Loop

**Start an autonomous loop to address all Gemini findings:**

> [!IMPORTANT]
> Do NOT create implementation plans or request user review for each cycle. Proceed directly with analysis and fixes.

#### a. Parse and Analyze Each Finding

For each finding from Gemini, perform a thorough analysis:

| Decision         | Criteria                              | Action                       |
| ---------------- | ------------------------------------- | ---------------------------- |
| **Fix**          | Valid issue, in scope, actionable     | Implement the fix            |
| **Out of Scope** | Valid but unrelated to current work   | Create GitHub issue to track |
| **Reject**       | False positive, not actually an issue | Document rationale           |

**Analysis checklist:**

- [ ] Read the relevant code and understand the context
- [ ] Verify Gemini's understanding is correct
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

git add <files>
git commit -m "fix(runner): handle null response in criteria evaluation"
```

> [!IMPORTANT]
> **Single line only** — no multi-line bodies or bullet lists. Each fix gets its own atomic commit.

For **Out of Scope** items, create GitHub issues and reference them in your walkthrough.md, not in commit messages.

> [!NOTE]
> Do NOT push yet - accumulate fixes across cycles for efficiency.

#### d. Continue Conversation for Follow-up Review

Use `--resume latest` to continue the same conversation with full context:

```bash
gemini -m gemini-3-pro-preview --yolo -o json --resume latest -p \
  "I addressed your review feedback in the last commit. Here's what I did:

[Paste summary of fixes, out-of-scope issues created, and rejected findings with rationale]

Please:
1. Verify the fixes are correct
2. Review my rationale for rejected findings
3. Do another thorough pass to catch anything you might have missed" 2>/dev/null | jq -r '.response'
```

#### e. Loop

If Gemini finds new issues, return to step (a). Continue until:

- All findings are addressed with fixes, OR
- All remaining findings have documented explanations, OR
- All remaining findings are tracked as out-of-scope issues

### 4. Finalization

Once the review cycle converges, update `task.md` with review notes:

- Total review cycles completed
- Summary of issues found by priority
- How each was resolved (fixed / tracked / rejected)
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

## Example Session

**Initial review:**

```bash
gemini -m gemini-3-pro-preview --yolo -o json -p \
  "Review commits on this branch. Focus on correctness, spec compliance, and test coverage." \
  2>/dev/null | jq -r '.response'
```

**Continue after fixes:**

```bash
gemini -m gemini-3-pro-preview --yolo -o json --resume latest -p \
  "Fixed the P2 timeout issue by propagating remaining time to sub-skills. Verify the fix and continue reviewing." \
  2>/dev/null | jq -r '.response'
```

**Final pass:**

```bash
gemini -m gemini-3-pro-preview --yolo -o json --resume latest -p \
  "All findings addressed. Do one final pass focusing on integration correctness." \
  2>/dev/null | jq -r '.response'
```

---

## Troubleshooting

| Issue                     | Solution                                                   |
| ------------------------- | ---------------------------------------------------------- |
| Model not available       | Fall back to `-m pro` or `-m flash`                        |
| Thinking noise in output  | Ensure `-o json` + `jq -r '.response'` is on every command |
| Lost conversation context | Use `gemini --resume` to browse past sessions              |
| Need specific session     | Use `gemini --resume <INDEX>` or session UUID              |
| Gemini modifying files    | Remove `--yolo` and use default approval mode              |
| jq not found              | Install: `brew install jq`                                 |
