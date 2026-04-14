---
name: gh-retro
description: Retrospective, next work selection, and context handoff
---

Use this skill to close the development loop after shipping. It covers three activities: reflecting on the process, selecting next work, and generating a handoff prompt for the next conversation.

## Prerequisites

- Feature has been merged (Ship skill complete)
- `task.md` checklist is fully marked up

## Steps

### 1. Retrospective

Review the completed flow and identify improvements:

| Category           | Question                                              |
| ------------------ | ----------------------------------------------------- |
| **What went well** | Which phases added the most value? What saved time?   |
| **What didn't**    | Where did the flow get stuck? What was wasted effort? |
| **Improvements**   | What would you change for next time?                  |

For each improvement, classify:

| Action             | When to use                                         |
| ------------------ | --------------------------------------------------- |
| **Fix now**        | Quick config/skill tweak, do it immediately         |
| **Discuss with D** | Needs human input — flag for the developer          |
| **Note**           | Good observation, no immediate action — log it here |

### 2. Next Work Selection

Invoke the `/gh-issue-triage` skill to review open issues and select the next task:

1. Review open issues in the repository
2. Prioritize based on urgency, dependencies, and momentum
3. Recommend the top candidate to the developer

### 3. Context Handoff

Generate a handoff prompt for the next conversation. A good handoff includes:

- **Repository and stack** — where and what
- **What was just shipped** — PR reference, one-line summary
- **Next task** — issue reference and brief description
- **Key context** — constraints, patterns, decisions from this conversation that carry forward
- **Skill** — which flow to use and any reviewer preferences

```
Working in [repo]. [Stack].
Just shipped: [one-line summary] (PR #N).
Next: Issue #M — [description].
Key context:
- [relevant constraint or decision]
- [relevant pattern or convention]
/gh-super-flow, [reviewer preferences]
```

> [!TIP]
> The handoff prompt should be **minimal** — just enough to orient the next conversation. Implementation details belong in the PR, not the handoff.

### 4. Report

```
Retrospective complete:
- Improvements identified: N (fix now: X, discuss: Y, noted: Z)
- Next work: Issue #M — [title]
- Handoff prompt generated
```
