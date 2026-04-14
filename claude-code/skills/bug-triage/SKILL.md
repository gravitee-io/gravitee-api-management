---
name: bug-triage
description: Reproduce a bug, capture evidence, analyze, and produce an issue/PR-ready summary
---

> **Read-Only**: This skill does NOT modify project source files. It focuses on reproduction, evidence capture, and analysis.

# /bug-triage

Use this skill to triage a bug end-to-end: reproduce the issue, capture evidence, analyze likely causes, and produce a summary that can be used to open a GitHub issue or start a fix.

## Intake Questions

1. **Bug summary?** (1-2 sentences)
2. **Expected vs actual behavior?**
3. **Environment?** (OS, browser/version, app version/commit, feature flags)
4. **Repro reliability?** (always / intermittent / unknown)
5. **Impact & severity?** (blocker, high, medium, low)
6. **Target repo & branch?** (if applicable)

---

## Steps

### 1. Plan Reproduction

- Confirm scope and clarify any missing info.
- Draft a step-by-step repro plan.
- Create **`repro_steps.md`** (Artifact) with:
  - Preconditions, data/setup, feature flags
  - Exact steps to reproduce
  - Expected vs actual for each step (if known)
- Ask the user to confirm the plan before executing.

### 2. Reproduce & Capture Evidence

- Execute the repro steps using the terminal and/or browser.
- Capture evidence as you go:
  - Logs (console output, server logs, stack traces)
  - Screenshots or recordings for UI issues
  - Request/response samples if relevant
- Create **`evidence.md`** (Artifact) containing:
  - Commands run
  - Key logs/snippets (trimmed)
  - Links or references to screenshots/recordings

### 3. Analyze & Hypothesize

- Identify likely root causes and affected components.
- Note any constraints or unknowns.
- Create **`analysis.md`** (Artifact) with:
  - Hypotheses (ranked)
  - Areas of code to inspect
  - Additional data needed (if any)

### 4. Produce Triage Summary

- Create **`triage_summary.md`** (Artifact) including:
  - Title
  - Repro steps (final)
  - Expected vs actual
  - Evidence links/snippets
  - Suspected root cause(s)
  - Recommended next steps (fix plan or follow-up)
  - Severity/impact

### 5. GitHub Handoff (Optional)

- If the repo uses GitHub, use the summary to open or update an issue via `gh issue create`.
- If you need a formal issue + PR flow, invoke the `/gh-init` skill using the summary as the issue body and plan context.

---

## Output Checklist

- `repro_steps.md`
- `evidence.md`
- `analysis.md`
- `triage_summary.md`
