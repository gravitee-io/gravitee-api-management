---
name: repo-analysis-collab
description: Deep multi-model repository analysis -- Council of Models
---

Use this skill to perform a serious, deep examination of a repository's codebase. T leads a **Council of Models** -- spawning parallel sub-agents internally while coordinating with C (Codex) and G (Gemini CLI) as independent peers. Each model analyzes independently first, then results are cross-validated through multiple rounds of structured dialogue.

## Participants

| Code   | Identity       | Role                                                    |
| ------ | -------------- | ------------------------------------------------------- |
| **D**  | User           | Domain expert, final decision-maker                     |
| **T**  | Claude Code    | Lead Architect -- orchestrates, synthesizes, facilitates |
| **C**  | Codex CLI      | Independent analyst -- `codex exec`                      |
| **G**  | Gemini CLI     | Independent analyst -- `gemini -p`                       |
| **CP** | GitHub Copilot | Automated code reviewer (used in GitHub PR workflows)    |

> [!NOTE]
> By default, all three models (T, C, G) participate. The user can scope participation:
>
> - _"Only use Gemini"_ -> T + G only
> - _"Just use Codex for this"_ -> T + C only
> - _"Solo"_ -> T only (sub-agents still used internally)

## Prerequisites

- MCP `github-mcp-server` authenticated
- `codex` CLI available in the terminal
- `gemini` CLI available in the terminal
- Clear **area of focus** agreed with D (see Focus Areas below)

## Focus Areas

Agree on one or more with D before starting:

| Focus Area                | What We're Looking For                                                        |
| ------------------------- | ----------------------------------------------------------------------------- |
| **Code Quality**          | Antipatterns, complexity hotspots, naming inconsistencies, code smells        |
| **Dead / Redundant Code** | Unused exports, unreachable branches, duplicated logic, orphaned files        |
| **Test Coverage Gaps**    | Untested paths, missing edge cases, fragile tests, mock overuse               |
| **Scalability**           | Bottlenecks, O(n^2) patterns, memory leaks, connection pooling issues          |
| **Performance**           | Hot paths, unnecessary allocations, blocking I/O, cache opportunities         |
| **Security**              | Injection vectors, auth bypasses, secret exposure, dependency vulnerabilities |
| **Architecture**          | Coupling violations, circular deps, layer leaks, missing abstractions         |
| **API Design**            | Inconsistent contracts, missing validation, backward compatibility risks      |
| **Documentation**         | Missing JSDoc/Javadoc, outdated README, undocumented behaviors                |

---

## CLI Quick Reference

### Codex (C)

| Command Pattern                                                                          | Purpose               |
| ---------------------------------------------------------------------------------------- | --------------------- |
| `codex exec --full-auto -c hide_agent_reasoning=true "prompt" 2>/dev/null`               | Initial analysis      |
| `codex exec resume --last --full-auto -c hide_agent_reasoning=true "prompt" 2>/dev/null` | Continue conversation |

### Gemini (G)

| Command Pattern                                                   | Purpose                    |
| ----------------------------------------------------------------- | -------------------------- |
| `gemini --model gemini-3-pro-preview --yolo -p "prompt"`          | Initial analysis           |
| `gemini --model gemini-3-pro-preview --yolo --resume -p "prompt"` | Continue conversation      |
| `cat file \| gemini --model gemini-3-pro-preview -p "prompt"`     | Analysis with file context |

> [!TIP]
> If `gemini-3-pro-preview` is not available, fall back to `-m pro` which resolves to the best available pro model.

---

## Steps

### 1. Scope and Context

- Confirm focus area(s) with D.
- Understand the repo:
  - Read `README.md`, `package.json` / `pom.xml` / equivalent
  - Run `fd` to map directory structure
  - Run `git log --oneline -20` for recent activity
  - Count files, LOC, and test coverage to gauge size

### 2. Launch Council -- Independent Analysis (Parallel)

Spawn all three analysis tracks **simultaneously**:

#### Track A: T's Internal Sub-Agents

Act as Lead Architect. Spawn sub-agents with distinct analytical roles:

| Agent Role               | Perspective                                                          |
| ------------------------ | -------------------------------------------------------------------- |
| **Structural Analyst**   | Directory layout, module boundaries, dependency graph, circular deps |
| **Quality Auditor**      | Code smells, complexity, naming, antipatterns, DRY violations        |
| **Test Strategist**      | Coverage gaps, test quality, edge cases, flaky patterns              |
| **Security Reviewer**    | Input validation, auth flows, secret handling, injection surfaces    |
| **Performance Engineer** | Hot paths, algorithmic complexity, memory patterns, I/O blocking     |

Each sub-agent should receive a detailed prompt with repo context, tech stack, and specific questions.

#### Track B: Codex (C) Analysis

```bash
codex exec --full-auto -c hide_agent_reasoning=true \
  "You are an independent lead architect analyzing this repository.

Tech stack: [STACK]
Focus area: [AREA]

Perform a comprehensive analysis:
1. Understand the overall architecture and design patterns
2. Identify issues across: [FOCUS AREAS]
3. For each finding:
   - Severity: Critical / High / Medium / Low
   - Location: File path and line range
   - Issue: Clear description
   - Evidence: Code snippet or metric
   - Recommendation: Specific fix
   - Effort: XS / S / M / L / XL

Be thorough and opinionated. Challenge the existing design.
Format as a numbered list grouped by category." 2>/dev/null
```

#### Track C: Gemini (G) Analysis

```bash
gemini --model gemini-3-pro-preview --yolo -p \
  "You are an independent lead architect analyzing this repository.

Tech stack: [STACK]
Focus area: [AREA]

Perform a comprehensive analysis:
1. Understand the overall architecture and design patterns
2. Identify issues across: [FOCUS AREAS]
3. For each finding:
   - Severity: Critical / High / Medium / Low
   - Location: File path and line range
   - Issue: Clear description
   - Evidence: Code snippet or metric
   - Recommendation: Specific fix
   - Effort: XS / S / M / L / XL

Be thorough and opinionated. Challenge the existing design.
Format as a numbered list grouped by category."
```

#### While Waiting: Web Research

While C and G are processing, T should use wait time productively:

- Search for known issues, best practices, and community patterns relevant to the repo's tech stack and focus area
- Look for security advisories on key dependencies
- Research architectural patterns that may improve identified problem areas
- This additional context enriches T's synthesis in later steps

### 3. Synthesize Results -- Round 1

Once all tracks complete, T acts as facilitator:

#### a. Deduplicate

- Merge findings that identify the same issue from different angles
- **Strong signal**: When 2+ models independently flag the same issue

#### b. Categorize

| Category      | Meaning                                 |
| ------------- | --------------------------------------- |
| **Confirmed** | 2+ models agree, evidence is clear      |
| **Probable**  | One model found it, evidence reasonable |
| **Uncertain** | Flagged but needs deeper investigation  |
| **Disputed**  | Models disagree -- needs resolution      |

#### c. Identify Gaps

- What did C find that G missed (and vice versa)?
- What did T's sub-agents find that neither CLI caught?
- What did neither find that web research suggests we should look for?

### 4. Cross-Validation -- Round 2+

Share the synthesized findings with each model and challenge them:

#### To Codex:

```bash
codex exec resume --last --full-auto -c hide_agent_reasoning=true \
  "Here are findings from an independent parallel analysis. Some overlap with yours, some don't.

FINDINGS ONLY YOU FLAGGED:
[list]

FINDINGS YOU MISSED (found by others):
[list]

DISPUTED FINDINGS (disagreement):
[list]

For each:
1. Do you stand by your original assessment?
2. Do you agree with findings you missed?
3. For disputes -- argue your position with evidence.
4. Any NEW findings after seeing this broader context?" 2>/dev/null
```

#### To Gemini:

```bash
gemini --model gemini-3-pro-preview --yolo --resume -p \
  "Here are findings from an independent parallel analysis. Some overlap with yours, some don't.

FINDINGS ONLY YOU FLAGGED:
[list]

FINDINGS YOU MISSED (found by others):
[list]

DISPUTED FINDINGS (disagreement):
[list]

For each:
1. Do you stand by your original assessment?
2. Do you agree with findings you missed?
3. For disputes -- argue your position with evidence.
4. Any NEW findings after seeing this broader context?"
```

> [!IMPORTANT]
> The goal is **genuine intellectual friction**. Don't rubber-stamp. If a model changes its position, demand evidence. If it holds firm, respect the dissent and present both views to D.

#### Additional Rounds

Run as many rounds as needed until:

- All **Confirmed** findings have clear evidence and remediation
- All **Disputed** findings have documented positions from each model
- No new significant findings are emerging

### 5. Final Report

Present directly in the IDE for discussion with D.

#### Report Structure

```markdown
# Repository Analysis: [REPO NAME]

**Date**: [DATE]
**Focus Areas**: [AREAS]
**Council**: T (lead) + C + G | [N] rounds of cross-validation

## Executive Summary

[2-3 paragraphs: overall health, key themes, headline findings]

## Critical Findings (P0)

[Immediate attention required]

## High-Priority Findings (P1)

[Address in next sprint]

## Medium-Priority Findings (P2)

[Plan for near-term]

## Low-Priority / Backlog (P3)

[Track for future consideration]

## Cross-Validation Summary

| #   | Finding | T   | C   | G   | Consensus | Notes |
| --- | ------- | --- | --- | --- | --------- | ----- |

## Patterns and Trends

[Recurring themes, systemic issues]

## Proposed Issue Breakdown

[See Exit Strategy below]

## Methodology

[Agents used, rounds performed, areas examined, web research conducted]
```

### 6. Exit Strategy -- Proposed Issues

> [!CAUTION]
> Do NOT create issues until D reviews the report and explicitly approves.

For each major finding area, propose:

- **Epic** (parent issue): Overall improvement area
- **Sub-issues**: Individual units of work, each scoped to one PR

For each proposed issue:

| Field         | Value                      |
| ------------- | -------------------------- |
| **Title**     | Clear, actionable title    |
| **Epic**      | Parent epic                |
| **Priority**  | P0 / P1 / P2 / P3          |
| **Effort**    | XS / S / M / L / XL        |
| **Summary**   | What needs to be done      |
| **Evidence**  | File paths and line ranges |
| **Rationale** | Impact of not fixing       |

Once D approves:

1. Create epic issues via `gh issue create`
2. Create sub-issues and link them via `gh issue create` with parent references
3. Apply labels for priority/effort via `gh issue edit --add-label`

---

## Guidelines for the Council

- **Independence first**: Each model must analyze before seeing others' results
- **Evidence over opinion**: Every finding needs file paths, line numbers, code snippets
- **Challenge everything**: "This looks fine" is not a finding
- **Respect dissent**: Genuine disagreement is valuable -- present both sides to D
- **Leverage wait time**: While CLIs process, T does web research for extra context
- **Be thorough**: Follow data flows end-to-end, check error handling, look for what's **missing**

---

## Troubleshooting

| Issue                          | Solution                                                      |
| ------------------------------ | ------------------------------------------------------------- |
| Gemini model not available     | Fall back to `--model pro` or `--model flash`                 |
| Sub-agent output too shallow   | Provide more specific prompts with concrete questions         |
| CLI times out                  | Break analysis into smaller, focused prompts                  |
| Too many low-severity findings | Filter to P0-P2 for report, mention P3 count in summary       |
| Models agree on everything     | Push back -- ask each to play devil's advocate on key findings |
| Repo too large for single pass | Divide into modules and analyze each separately               |
