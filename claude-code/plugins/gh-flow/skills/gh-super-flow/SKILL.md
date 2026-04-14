---
name: gh-super-flow
description: Orchestrated end-to-end development flow with configurable AI model reviews
---

Use this skill to orchestrate the entire feature development lifecycle, from issue to shipping. This skill generates a **tailored `task.md` checklist** that tracks progress across sub-skills. Each phase references its sub-skill for full instructions — the agent reads the skill file when it reaches that step, then returns to `task.md` to mark progress and move to the next phase.

> [!CAUTION]
> **Model-switching mid-flow**: If the backing model changes during execution (e.g., Claude -> Gemini), the agent MUST re-read `task.md` and the most recent artifact(s) before continuing. Treat a model switch like resuming from a checkpoint — verify where you are, what's been done, and what's next.

---

## 1. Parse Configuration

Analyze the user's prompt to determine the execution mode.

**Inputs:**

- **Context**: Issue # / Link OR Raw Description
- **Plan Reviewers**: Which models to use for plan review?
    - _Default_: **Gemini**
    - _Options_: Codex, Gemini, Claude, Any Combination, None (skip)
- **Local Code Reviewers**: Which models to use for local code review (pre-PR, via CLI)?
    - _Default_: **Codex**
    - _Options_: Codex, Gemini, Claude, Any Combination, None (skip)
- **Max Review Rounds**: _Default_: **3**

**Example Prompts & Interpretations:**

- _"/gh-super-flow Fix bug #123"_ -> Plan: Gemini, Local Review: Codex (Defaults)
- _"/gh-super-flow Add feature X, use Claude for planning"_ -> Plan: Claude, Local Review: Codex
- _"/gh-super-flow Refactor login, local review with Gemini and Claude"_ -> Plan: Gemini, Local Review: Gemini + Claude
- _"/gh-super-flow Super flow for issue #55, use Claude for everything"_ -> Plan: Claude, Local Review: Claude
- _"/gh-super-flow Skip plan review for this simple bug, use Gemini for local review"_ -> Plan: None, Local Review: Gemini

---

## 2. Generate Tailored Checklist

Generate a `task.md` checklist based on the parsed configuration. Each phase is a single checklist item that points to its sub-skill. When the agent reaches a step, it invokes the referenced skill for full instructions (prompt templates, CLI flags, conversation patterns, troubleshooting). After completing that phase, it returns here to check it off and move on.

> [!IMPORTANT]
> `task.md` is the **single source of truth for progress**. The sub-skill files are the **single source of truth for instructions**. Don't duplicate instruction detail into the checklist — that's how context gets diluted.

**Template** (adapt based on configuration):

```markdown
# Super GH Flow — [Issue/Description]

Config: Plan=[Gemini/Claude/Both/None], Local Review=[Codex/Gemini/Claude/.../None], Max Rounds=3

## Phase 1: Initialization

- [ ] Invoke the `/gh-init` skill — read issue (or create one), assign to self, create local branch, scaffold `implementation_plan.md`
      All work stays local — no push or PR until Phase 8 (gh-create-pr).

## Phase 2: Plan Review

- [ ] Invoke the `/plan-cycle-[model]` skill — stress-test `implementation_plan.md` with [Model]
      (if Both: run sequentially, integrate first reviewer's feedback before starting second)

## Phase 3: Plan Alignment Check

- [ ] Re-read original issue — all acceptance criteria addressed? Nothing beyond scope?
- [ ] Cross-reference codebase — files exist? `[NEW]` vs `[MODIFY]` correct? Patterns match siblings?
- [ ] Self-critique — "If I were the reviewer, what would I flag?"
- [ ] Update plan with corrections (or note "alignment check passed clean")

## Phase 4: Human Authorization

- [ ] Present `implementation_plan.md` to the developer (include alignment check results)
- [ ] Wait for the developer's approval before proceeding

## Phase 5: Implementation

- [ ] Invoke the `/gh-implement` skill — TDD gate -> implement -> atomic commits
      TDD GATE: At least one failing test must exist before ANY implementation code (see `rules/tdd.md`)

## Phase 6: Local Code Review (max [N] rounds)

- [ ] Round 1: Invoke the `/review-cycle-[model]` skill — initial review, analyze findings, fix/reject/track
- [ ] Round 2 (if needed): Continue review conversation, apply convergence check
- [ ] Round 3 (if needed, FINAL): Last pass — if P1/P2 remain, escalate to the developer rather than continuing
      (if multiple reviewers: run sequentially, each gets its own round counter)

## Phase 7: Docs Sync

- [ ] Invoke the `/gh-docs-sync` skill — review what changed, update any stale docs (README, CONTRIBUTING, etc.)
      MANDATORY: Always run the full assessment. The outcome may be "no updates needed" but you must never skip this phase.

## Phase 8: Create PR

- [ ] Invoke the `/gh-create-pr` skill — push local branch, create draft PR with plan, create initial walkthrough
      This is the transition from local to remote. After this, work becomes visible on GitHub.

## Phase 9: Copilot Review

- [ ] Invoke the `/review-cycle-copilot` skill — request Copilot review on PR, iterate on feedback, mark PR ready
      Use intelligent polling (30s intervals, 5min timeout) for Copilot responses, NOT fixed sleeps

## Phase 10: Human Review

- [ ] Present PR to Dev for final review (link to PR, walkthrough summary)
- [ ] Wait for Dev's approval before shipping
      Dev reviews the final PR. This is the last gate before merge.

## Phase 11: Ship

- [ ] Invoke the `/gh-ship` skill — verify completion, rebase, merge, cleanup
      Merges the PR and cleans up the branch. `/gh-retro` follows.

## Phase 12: Retrospective

- [ ] Invoke the `/gh-retro` skill — reflect on the flow, select next work, generate context handoff prompt
      Closes the loop: retro -> triage -> handoff prompt for the next conversation.
```

After generating the checklist, **begin executing from the first unchecked item**. When reaching each phase, invoke the referenced skill for full instructions.

---

## 3. Execution Rules

### Reading Sub-Skills

When you reach a checklist item that says "Invoke the `/skill-name` skill":

1. Invoke the skill for full instructions
2. Follow those instructions completely
3. When the sub-skill is done, return to `task.md`
4. Mark the item as complete (`[x]`)
5. Proceed to the next unchecked item

> [!WARNING]
> **Never forget the parent flow.** After completing any sub-skill, always return to `task.md` to determine what's next. The checklist is your anchor.

### Walkthrough Phase Checklist

During Phase 10 (Human Review), the walkthrough must include a snapshot of the `task.md` checklist with each completed phase marked:

- **Completed** — phase was executed successfully
- **Skipped** — phase was intentionally skipped, with reason (e.g., "Plan review skipped per the developer's request")

This provides a clear audit trail of what was and wasn't done during the flow.

### Local vs Remote Work

The pipeline has a clear boundary between local and remote work:

| Phases | Location        | Visibility                                     |
| ------ | --------------- | ---------------------------------------------- |
| 1-7    | Local only      | No GitHub visibility                           |
| 8-11   | Remote (GitHub) | Public on your fork/org                        |
| 12     | Meta            | Skill improvements + next conversation seed    |

### Walkthrough Lifecycle

The `walkthrough.md` artifact evolves across phases:

| Phase                  | Action                                                                                         |
| ---------------------- | ---------------------------------------------------------------------------------------------- |
| **1-7 (Local)**        | No walkthrough yet — work is local-only                                                        |
| **8 (Create PR)**      | Create initial `walkthrough.md` — snapshot of implementation, files changed, tests, validation |
| **9 (Copilot Review)** | Update walkthrough — add review findings and fixes                                             |
| **10 (Human Review)**  | Dev reviews final state before merge                                                           |
| **11 (Ship)**          | Finalize walkthrough — confirm delivery phases complete, add phase checklist                   |

### TDD Enforcement

Before writing **any** implementation code, confirm that at least one failing test exists for the acceptance criteria. If no tests exist yet, write them first. This is non-negotiable — the TDD rule in `rules/tdd.md` applies here and is enforced as a gate in Phase 5.

### Local Code Review Convergence

Review cycles have explicit exit conditions to prevent endless loops:

| Condition                                                             | Action                                                           |
| --------------------------------------------------------------------- | ---------------------------------------------------------------- |
| All findings are P3/Minor or design decisions rejected with rationale | **STOP** — converge and proceed to next phase                    |
| Round limit reached (default: 3) with no P1/P2 remaining              | **STOP** — converge and proceed to next phase                    |
| Round limit reached with P1/P2 still open                             | **ESCALATE** — flag for the developer's review                   |
| Reviewer agrees code is clean                                         | **STOP** — proceed to next phase                                 |

### Multiple Reviewers

When multiple reviewers are configured, use the preferred ordering: **Gemini -> Claude -> Codex** (G -> C -> X).

If multiple local code reviewers are configured:

1. Run them sequentially in preferred order (G -> C -> X)
2. Each reviewer gets its own round counter (3 rounds each, not 3 total)
3. Fixes from reviewer 1 are committed before reviewer 2 starts

If multiple plan reviewers are configured:

1. Run sequentially in preferred order (G -> C -> X) — integrate feedback from the first before starting the second
2. This avoids conflicting advice
3. **Skip the "Present to Dev" step** inside each plan cycle skill — Phase 4 (Human Authorization) handles this for the orchestrator

---

## Summary of Defaults

If the user gives no specific instruction, the standard path is:

1.  **Init** — `/gh-init`
2.  **Plan Review** — `/plan-cycle-gemini`
3.  **Alignment Check** — the agent self-reviews plan against issue + codebase
4.  **Human Approval** — the developer reviews plan
5.  **Implement** — `/gh-implement` (TDD gate enforced)
6.  **Local Code Review** — `/review-cycle-codex`, max 3 rounds
7.  **Docs Sync** — `/gh-docs-sync`
8.  **Create PR** — `/gh-create-pr` (push + draft PR + initial walkthrough)
9.  **Copilot Review** — `/review-cycle-copilot`
10. **Human Review** — Dev reviews final PR before merge
11. **Ship** — `/gh-ship` (rebase + merge + cleanup)
12. **Retrospective** — `/gh-retro` (retro, next work selection, context handoff)
