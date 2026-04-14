---
name: plan-cycle-gemini
description: Stress-test implementation plans with Gemini CLI
---

Use this skill to refine an `implementation_plan.md` through structured debate with Gemini CLI before presenting to the developer. This sits **between planning and implementation** — the plan gets forged under pressure so the developer reviews something already battle-tested.

## When to Use

- **Big tasks**: Multi-file changes, architectural shifts, new features with significant scope
- **Uncertain designs**: Multiple viable approaches and you're not sure which is best
- **High-risk changes**: Breaking changes, security-sensitive code, performance-critical paths
- **Optional**: The developer may request this explicitly, or the agent can suggest it when the plan warrants it

> [!NOTE]
> For small, well-scoped tasks, skip this and go straight to developer review -> `/gh-implement`.

## Prerequisites

- `implementation_plan.md` exists (created during `/gh-init` planning phase)
- `gemini` CLI and `jq` available in the terminal
- Plan has NOT yet been presented to the developer for review

## Gemini CLI Quick Reference

| Command Pattern                                                                                              | Purpose         |
| ------------------------------------------------------------------------------------------------------------ | --------------- |
| `gemini -m gemini-3-pro-preview --yolo -o json -p "prompt" 2>/dev/null \| jq -r '.response'`                 | Initial review  |
| `gemini -m gemini-3-pro-preview --yolo -o json --resume latest -p "prompt" 2>/dev/null \| jq -r '.response'` | Continue debate |

> [!IMPORTANT]
> Always use `-o json` + `jq -r '.response'` to suppress thinking/tool-call noise. Without this, Gemini's internal reasoning floods stdout and risks context collapse for the agent.

> [!TIP]
> Sessions are project-specific and auto-saved. Use `gemini --resume` to browse past sessions. If `gemini-3-pro-preview` is unavailable, fall back to `-m pro`.

---

## Steps

### 1. Submit Plan for Review

Send the full `implementation_plan.md` to Gemini with a clear review mandate:

```bash
cat <path-to-implementation_plan.md> | \
gemini -m gemini-3-pro-preview -o json -p \
  "You are a senior architect reviewing an implementation plan. Your job is to stress-test this plan — find weaknesses, challenge assumptions, and push for the strongest possible design.

> IMPORTANT: This is a READ-ONLY review. Do NOT create, modify, or delete any files. Do NOT implement any code. Your ONLY output should be your written review.

Context:
- Repository: [REPO]
- Tech stack: [STACK]
- Issue/PR: [REFERENCE]

Review the plan critically across these dimensions:
1. **Correctness**: Will this actually solve the problem?
2. **Completeness**: What's missing? Edge cases? Error handling?
3. **Architecture**: Are the abstractions right? Any coupling concerns?
4. **Risk**: What could go wrong? What are the failure modes?
5. **Alternatives**: Is there a simpler or more robust approach?
6. **Testability**: Is the verification plan sufficient?
7. **Sequencing**: Is the implementation order optimal?

For each concern:
- **Severity**: Blocker / Major / Minor / Nitpick
- **Section**: Which part of the plan
- **Issue**: What's wrong
- **Suggestion**: How to improve it

Be tough but constructive. If the plan is solid, say so — but look hard before you do." 2>/dev/null | jq -r '.response'
```

### 2. Debate Loop

For each piece of feedback, the agent evaluates and responds:

#### a. Analyze Each Concern

| Decision      | Criteria                                     | Action                          |
| ------------- | -------------------------------------------- | ------------------------------- |
| **Accept**    | Valid concern, improves the plan             | Update `implementation_plan.md` |
| **Partially** | Good point but overscoped or impractical     | Adapt the suggestion, explain   |
| **Reject**    | Disagrees with constraints, false assumption | Defend with evidence            |

#### b. Respond and Continue

```bash
gemini -m gemini-3-pro-preview -o json --resume latest -p \
  "Here's my response to your review:

ACCEPTED (plan updated):
[list changes made]

PARTIALLY ACCEPTED:
[list adaptations with rationale]

REJECTED:
[list with detailed defense]

Please:
1. Verify accepted changes improve the plan
2. Push back if you disagree with any rejections
3. Raise any NEW concerns now that you see my reasoning
4. If satisfied, confirm the plan is ready for human review" 2>/dev/null | jq -r '.response'
```

#### c. Loop

Continue until:

- All **Blocker** and **Major** concerns are resolved
- Remaining concerns are **Minor** / **Nitpick** with documented rationale
- Gemini confirms the plan is ready, OR no new substantive feedback

> [!IMPORTANT]
> Don't chase perfection — 2-3 rounds is typical. If round 4+ isn't producing meaningful improvements, converge and move on.

### 3. Update Plan

After the debate converges, update `implementation_plan.md` with:

- All accepted changes integrated into the plan body
- A brief **Review Notes** section at the bottom documenting:
    - Reviewed by Gemini
    - Number of rounds
    - Key concerns raised and how they were resolved
    - Any dissent that the developer should be aware of

### 4. Present to the Developer

> [!NOTE]
> **When running inside `/gh-super-flow`**: Skip this step. The orchestrator's Human Authorization step handles presentation to the developer.

**When running standalone**: Present the refined `implementation_plan.md` to the developer for final review.

> [!TIP]
> Call out the most significant changes from the debate — the developer should know what was strengthened and what was contentious.

### 5. Proceed

Once the developer approves -> `/gh-implement`

---

## Guidelines

- **The agent defends honestly**: Don't cave on every point. If the plan is right, argue for it with evidence.
- **Gemini pushes hard**: The whole point is pressure — easy agreement defeats the purpose.
- **Update the plan, not just the conversation**: Changes must be reflected in `implementation_plan.md`.
- **Preserve the reasoning**: The Review Notes section captures WHY decisions were made.
- **Know when to stop**: Diminishing returns are real. Converge when rounds stop producing insight.

---

## Troubleshooting

| Issue                                  | Solution                                                                         |
| -------------------------------------- | -------------------------------------------------------------------------------- |
| Thinking noise in output               | Ensure `-o json` + `jq -r '.response'` is on every command                       |
| Gemini agrees with everything          | Ask Gemini to steelman the opposite approach                                     |
| Gemini implements instead of reviewing | Remove `--yolo` from the command; this skill should use default approval mode    |
| Feedback is too vague                  | Ask for specific file/section references and concrete fixes                      |
| Debate is going in circles             | Summarize positions, pick the one with stronger evidence                         |
| Gemini model not available             | Fall back to `-m pro` or `-m flash`                                              |
| Plan changed substantially             | Consider re-running from step 1 with the updated plan                            |
| Lost conversation context              | Use `gemini --resume` to browse past sessions                                    |
| jq not found                           | Install: `brew install jq`                                                       |
