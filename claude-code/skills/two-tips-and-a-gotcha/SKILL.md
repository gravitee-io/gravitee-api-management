---
name: two-tips-and-a-gotcha
description: Draft the next edition of the agentic coding field guide newsletter
---

Use this skill to write a new edition of **Two Tips and a Gotcha**, an irregular internal newsletter distilling practical lessons from real-world agentic coding.

---

## Format

Every edition follows the **2T+G** structure:

1. **Tip 1** — A workflow pattern, tool trick, or prompt technique that works
2. **Tip 2** — A second tip, ideally from a different domain or toolchain
3. **Gotcha** — A sharp edge, anti-pattern, or surprise that cost real time

## Writing Guidelines

- Each tip must be grounded in a **real scenario** — no fabricated examples
- Include concrete code snippets, CLI invocations, or mermaid diagrams where they add clarity
- Keep the tone conversational and practitioner-focused — this is a field guide, not a whitepaper
- Close with the standing invitation for contributions
- Sign off as `— D & T ✌️`

## File Convention

Save new editions to `editions/` using the naming pattern:

```
vol-{volume}-issue-{issue}-{slug}.md
```

## Header Template

```markdown
# Two Tips and a Gotcha 🗞️

**Vol. {V}, Issue {I}** | {Month} {Year}

_Agentic Coding Field Guide_

---
```

## Process

1. Read all existing editions in `editions/` to understand voice and avoid repetition
2. Identify candidate topics from recent conversations, retros, or user-supplied themes
3. Draft the edition following the 2T+G format
4. Save to `editions/` with the correct filename
5. Update the editions table below

## Editions

| Volume | Issue | Title | Date |
| ------ | ----- | ----- | ---- |
| 1 | 1 | [Context, MoE, and Ki](editions/vol-1-issue-1-context-moe-ki.md) | 2026-02-10 |
