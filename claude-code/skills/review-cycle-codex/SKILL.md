---
name: review-cycle-codex
description: Automated code review loop with Codex CLI
---

Use this skill to iterate on code review feedback from Codex CLI until all issues are addressed. This skill automates the agent <-> Codex review cycle, maintaining a single conversation context across multiple review iterations.

## Prerequisites

- Implementation is complete (typically after `/gh-implement`)
- Branch has commits ready for review
- `codex` CLI is available in the terminal
- For issue tracking: `gh` CLI authenticated

## Codex CLI Quick Reference

| Command Pattern                                                              | Purpose                                |
| ---------------------------------------------------------------------------- | -------------------------------------- |
| `codex exec -c hide_agent_reasoning=true "prompt" 2>/dev/null`               | Clean output for prompts               |
| `codex exec resume --last "prompt" -c hide_agent_reasoning=true 2>/dev/null` | Continue conversation                  |
| `--full-auto`                                                                | Workspace write + on-request approvals |
| `--json`                                                                     | Machine-readable JSONL output          |
| `-o <path>`                                                                  | Write final message to file            |

> [!TIP]
> Sessions are stored in `~/.codex/sessions/` as JSONL files. Use `codex resume` interactively to browse past sessions.

---

## Steps

### 1. Gather Context

- Run `git log --oneline origin/main..HEAD` to identify commits to review.
- Locate `implementation_plan.md` (either in artifacts or PR body).
- Run `git diff origin/main..HEAD --stat` to understand scope.

### 2. Initial Codex Review

Run the initial review and **establish the conversation** for subsequent iterations:

```bash
codex exec --full-auto -c hide_agent_reasoning=true \
  "Review all commits on this branch against origin/main. First read any implementation plan or README, then examine each changed file.

For each finding, provide:
- **Priority**: [P1] Critical / [P2] Important / [P3] Minor
- **File**: Absolute path and line range
- **Issue**: Clear description of the problem
- **Suggestion**: How to fix it

Focus on: correctness bugs, spec violations, test gaps, edge cases, security concerns.
Format findings as a numbered list." 2>/dev/null
```

### 3. Review Feedback Loop

**Start an autonomous loop to address all Codex findings:**

> [!IMPORTANT]
> Do NOT create implementation plans or request user review for each cycle. Proceed directly with analysis and fixes.

#### a. Parse and Analyze Each Finding

For each finding from Codex, perform a thorough analysis:

| Decision         | Criteria                              | Action                       |
| ---------------- | ------------------------------------- | ---------------------------- |
| **Fix**          | Valid issue, in scope, actionable     | Implement the fix            |
| **Out of Scope** | Valid but unrelated to current work   | Create GitHub issue to track |
| **Reject**       | False positive, not actually an issue | Document rationale           |

**Analysis checklist:**

- [ ] Read the relevant code and understand the context
- [ ] Verify Codex's understanding is correct
- [ ] Determine if fix introduces new risks
- [ ] Check if issue is covered by existing tests

#### b. Implement Fixes

For each **Fix** decision:

1. Make the code change following `/gh-implement` principles
2. Run build/test suite to confirm no regressions
3. Stage the change: `git add .`

For each **Out of Scope** decision:

1. Create a GitHub issue using `gh issue create`
2. Note the issue number in commit message

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

Use `codex exec resume --last` to continue the same conversation with full context:

```bash
codex exec resume --last --full-auto -c hide_agent_reasoning=true \
  "I addressed your review feedback in the last commit. Here's what I did:

[Paste summary of fixes, out-of-scope issues created, and rejected findings with rationale]

Please:
1. Verify the fixes are correct
2. Review my rationale for rejected findings
3. Do another thorough pass to catch anything you might have missed" 2>/dev/null
```

#### e. Loop

If Codex finds new issues, return to step (a). Continue until:

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
codex exec --full-auto -c hide_agent_reasoning=true \
  "Review commits on this branch. Focus on correctness, spec compliance, and test coverage." 2>/dev/null
```

**Continue after fixes:**

```bash
codex exec resume --last --full-auto -c hide_agent_reasoning=true \
  "Fixed the P2 timeout issue by propagating remaining time to sub-skills. Verify the fix and continue reviewing." 2>/dev/null
```

**Final pass:**

```bash
codex exec resume --last --full-auto -c hide_agent_reasoning=true \
  "All findings addressed. Do one final pass focusing on integration correctness." 2>/dev/null
```

---

## Troubleshooting

| Issue                        | Solution                                      |
| ---------------------------- | --------------------------------------------- |
| Noisy output                 | Add `2>/dev/null` to suppress stderr          |
| Thinking blocks showing      | Add `-c hide_agent_reasoning=true`            |
| Lost conversation context    | Use `codex resume` to pick from past sessions |
| Need specific session        | Use `codex exec resume <SESSION_ID> "prompt"` |
| Want machine-readable output | Add `--json` flag                             |
