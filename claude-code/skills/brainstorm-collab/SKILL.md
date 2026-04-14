---
name: brainstorm-collab
description: Collaborative brainstorming with Council of Models
---

Use this skill for collaborative brainstorming sessions -- new features, architectural proposals, complex problem-solving, or entirely new systems. T leads a **Council of Models**, facilitating structured dialogue between C (Codex) and G (Gemini CLI) to produce a well-vetted implementation plan or roadmap.

## Participants

| Code  | Identity    | Role                                                    |
| ----- | ----------- | ------------------------------------------------------- |
| **D** | User        | Domain expert, vision owner, final decision-maker       |
| **T** | Claude Code | Facilitator -- synthesizes, challenges, drives consensus |
| **C** | Codex CLI   | Independent thinker -- `codex exec`                      |
| **G** | Gemini CLI  | Independent thinker -- `gemini -p`                       |

> [!NOTE]
> By default, all three models participate. D can scope participation:
>
> - _"Only use Gemini"_ -> T + G only
> - _"Just use Codex"_ -> T + C only
> - _"Solo"_ -> T only (sub-agents still used internally)

## Prerequisites

- `codex` CLI available in the terminal
- `gemini` CLI available in the terminal
- Clear **topic** or **problem statement** from D

## Session Types

| Type                      | When to Use                                | Typical Output                      |
| ------------------------- | ------------------------------------------ | ----------------------------------- |
| **New Feature**           | Designing a feature from scratch           | Implementation plan + task list     |
| **Architecture Proposal** | Major structural change to existing system | ADR-style proposal + migration plan |
| **New System / App**      | Greenfield project design                  | System design + tech stack + plan   |
| **Problem Solving**       | Stuck on a complex technical challenge     | Solution options + recommendation   |
| **Exploration**           | "What if we..." open-ended investigation   | Research summary + feasibility      |

---

## CLI Quick Reference

### Codex (C)

| Command Pattern                                                                                                | Purpose           |
| -------------------------------------------------------------------------------------------------------------- | ----------------- |
| `codex exec --full-auto --skip-git-repo-check -c hide_agent_reasoning=true "prompt" 2>/dev/null`               | Initial thinking  |
| `codex exec resume --last --full-auto --skip-git-repo-check -c hide_agent_reasoning=true "prompt" 2>/dev/null` | Continue dialogue |

### Gemini (G)

| Command Pattern                                                   | Purpose           |
| ----------------------------------------------------------------- | ----------------- |
| `gemini --model gemini-3-pro-preview --yolo -p "prompt"`          | Initial thinking  |
| `gemini --model gemini-3-pro-preview --yolo --resume -p "prompt"` | Continue dialogue |

> [!TIP]
> If `gemini-3-pro-preview` is not available, fall back to `-m pro`.

---

## Steps

### 1. Frame the Problem

- Capture D's vision, constraints, and success criteria.
- Define the session type (see above).
- Gather relevant context:
  - If existing codebase: read key files, architecture, dependencies
  - If greenfield: understand the domain, target users, constraints
  - Run web searches for prior art, patterns, and industry standards

### 2. Independent Ideation (Parallel)

Each model thinks **independently** before seeing others' ideas.

#### Track A: T's Internal Brainstorm

As Lead Architect, generate your own proposal:

- Spawn sub-agents with different perspectives if useful (e.g., "think as a user," "think as an ops engineer," "think as a security auditor")
- Consider at least 2-3 distinct approaches
- For each approach: strengths, weaknesses, risks, effort

#### Track B: Codex (C) Ideation

```bash
codex exec --full-auto --skip-git-repo-check -c hide_agent_reasoning=true \
  "[SESSION_TYPE]: [PROBLEM STATEMENT]

Context:
[RELEVANT CONTEXT -- tech stack, constraints, existing architecture]

Requirements:
[D'S REQUIREMENTS AND SUCCESS CRITERIA]

As a senior architect, propose your approach:
1. High-level design and key architectural decisions
2. Technology choices with rationale
3. At least 2 alternative approaches you considered and why you prefer yours
4. Key risks and how to mitigate them
5. Rough effort estimate and phasing
6. Any open questions or areas where you'd push back on the requirements

Be opinionated. Take a strong position and defend it." 2>/dev/null
```

#### Track C: Gemini (G) Ideation

```bash
gemini --model gemini-3-pro-preview --yolo -p \
  "[SESSION_TYPE]: [PROBLEM STATEMENT]

Context:
[RELEVANT CONTEXT -- tech stack, constraints, existing architecture]

Requirements:
[D'S REQUIREMENTS AND SUCCESS CRITERIA]

As a senior architect, propose your approach:
1. High-level design and key architectural decisions
2. Technology choices with rationale
3. At least 2 alternative approaches you considered and why you prefer yours
4. Key risks and how to mitigate them
5. Rough effort estimate and phasing
6. Any open questions or areas where you'd push back on the requirements

Be opinionated. Take a strong position and defend it."
```

#### While Waiting: Research

While C and G process, use the time productively:

- Search for prior art, open-source implementations, relevant blog posts
- Look up benchmarks, case studies, or post-mortems for similar systems
- Research libraries, frameworks, or services that could accelerate the work
- This research enriches T's synthesis and may reveal options no model considered

### 3. Synthesis -- Round 1

Compare all three proposals:

#### a. Common Ground

Identify where all models converge -- this is your **foundation**.

#### b. Divergence

Where do proposals differ? Capture each distinct approach:

| Decision Point | T's Position | C's Position | G's Position |
| -------------- | ------------ | ------------ | ------------ |

#### c. Novel Ideas

Flag any approach or insight that only one model proposed -- these are often the most valuable contributions.

### 4. Structured Debate -- Round 2+

Share the synthesis with each model and force them to engage with alternatives:

#### To Codex:

```bash
codex exec resume --last --full-auto --skip-git-repo-check -c hide_agent_reasoning=true \
  "Two other architects proposed alternative approaches. Here's where we agree and disagree:

COMMON GROUND:
[consensus points]

YOUR UNIQUE PROPOSALS:
[what only C suggested]

ALTERNATIVES YOU DIDN'T CONSIDER:
[proposals from T and G that C didn't suggest]

KEY DISAGREEMENTS:
[where positions conflict]

For each:
1. Does seeing alternatives change your recommendation?
2. Where do you hold firm? Argue your case.
3. Where would you concede?
4. Any hybrid approach that takes the best of each?" 2>/dev/null
```

#### To Gemini:

```bash
gemini --model gemini-3-pro-preview --yolo --resume -p \
  "Two other architects proposed alternative approaches. Here's where we agree and disagree:

COMMON GROUND:
[consensus points]

YOUR UNIQUE PROPOSALS:
[what only G suggested]

ALTERNATIVES YOU DIDN'T CONSIDER:
[proposals from T and C that G didn't suggest]

KEY DISAGREEMENTS:
[where positions conflict]

For each:
1. Does seeing alternatives change your recommendation?
2. Where do you hold firm? Argue your case.
3. Where would you concede?
4. Any hybrid approach that takes the best of each?"
```

> [!IMPORTANT]
> Run as many rounds as needed. The goal is to **converge on the strongest possible proposal**, not to reach artificial consensus. Genuine dissent is valuable -- present minority opinions to D.

### 5. Produce Deliverable

Create an `implementation_plan.md` artifact (or equivalent) that captures the Council's output.

#### For Feature / Architecture / System proposals:

```markdown
# [Proposal Title]

**Session Type**: [TYPE]
**Date**: [DATE]
**Council**: T (lead) + C + G | [N] rounds

## Problem Statement

[What we're solving and why]

## Recommended Approach

[The proposal the Council converged on]

## Key Architecture Decisions

[Major choices and rationale]

## Alternative Approaches Considered

[What was rejected and why -- preserve the reasoning]

## Dissenting Opinions

[Where any model disagrees with the recommendation -- and their argument]

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
| ---- | ---------- | ------ | ---------- |

## Implementation Phases

[Ordered phases with dependencies]

## Proposed Issue Breakdown

[Epics and work items -- for if/when we proceed]

## Open Questions for D

[Things the Council couldn't resolve without domain input]
```

#### For Problem Solving sessions:

```markdown
# [Problem Title]

**Date**: [DATE]
**Council**: T + C + G | [N] rounds

## Problem Description

[The challenge]

## Root Cause Analysis

[What the Council determined]

## Recommended Solution

[With evidence]

## Alternative Solutions

[Considered and rejected]

## Next Steps

[Concrete actions]
```

### 6. Present and Decide with D

Present the deliverable to D. Two possible outcomes:

#### Outcome A: Roadmap for Later

The brainstorm produced a plan worth keeping but not executing immediately:

- Save the `implementation_plan.md` for future reference
- Optionally create tracking issues for visibility
- Move on to other work

#### Outcome B: Execute Now

The brainstorm produced a clear, actionable plan ready for execution:

- Transition directly into implementation
- The `implementation_plan.md` becomes the PR plan
- Full speed ahead

> [!TIP]
> D decides the outcome. T should recommend based on scope, risk, and readiness -- but never assume.

---

## Guidelines for Brainstorm Sessions

- **Independence first**: Each model must propose before seeing others' ideas
- **Strong opinions, loosely held**: Take positions, but update when evidence warrants
- **Challenge assumptions**: Push back on requirements if they seem wrong
- **Concrete over abstract**: Proposals should include specific technologies, patterns, and file structures -- not just hand-wavy architecture diagrams
- **Leverage wait time**: While CLIs process, research prior art and industry patterns
- **Preserve dissent**: Minority opinions are not failures -- they're insurance against groupthink
- **Know when to stop**: If rounds aren't producing new insights, converge and present

---

## Troubleshooting

| Issue                                   | Solution                                                                                |
| --------------------------------------- | --------------------------------------------------------------------------------------- |
| Codex: "Not inside a trusted directory" | Already handled -- `--skip-git-repo-check` flag is included in all commands above        |
| Gemini: "Directory mismatch" warning    | Non-blocking -- Gemini still produces output. Run from the IDE workspace dir to suppress |
| Gemini model not available              | Fall back to `--model pro` or `--model flash`                                           |
| Models converge too fast                | Ask each to steelman the approach they like least                                       |
| Scope is too broad                      | Ask D to constrain; break into multiple focused sessions                                |
| No clear winner between options         | Present trade-off matrix to D -- let domain expertise decide                             |
| Session is going in circles             | Force a vote: each model ranks options, T synthesizes                                   |
