---
name: story-clarifier
description: Use this skill when the user is reviewing a specific story and raises unclear points about its functional scope, design mapping, implementation assumptions, or missing decisions. The skill works from the perspective of the current story first. It may inspect the parent Epic requirements, sibling story files, implementation plan, task files, design references, or codebase only to gather supporting context. When code inspection is needed, search the most relevant module for the story first. If the answer exists, it updates the current story's functional requirements to make the behavior explicit. If the answer is found in code, it also records a structured open-question trace entry with the code location and short reasoning. If the answer does not exist, it records a structured open question in the current story or, when appropriate, in the parent Epic instead of inventing an answer.
---

# Story Clarifier

Use this skill when reviewing feature documentation from the perspective of one specific story and clarifying unclear behavior.

## Goal

Keep story-level documentation precise while reviewing it incrementally with the user.

This skill should:
- start from the current story as the main unit of work
- search the current story first, then gather supporting context from the Epic, other stories, implementation plan, task files, design references, and current implementation
- inspect the project code when documentation is insufficient, starting with the module most likely affected by the current story
- determine whether the raised issue is already answered for the current story
- if answered, make the answer explicit in the current story's `Functional Requirements`
- if the answer is found in code, leave a trace entry in `Open Questions` with code location, short reasoning, and resolved status
- if unanswered, add or update an `Open Questions` entry in the current story or, when the question is clearly cross-story, in the Epic document
- preserve the documentation conventions defined in `AGENTS.md`

## When To Use

Use this skill when the user:
- asks whether some behavior is already defined for a specific story
- points out ambiguity in a story requirement
- asks to clarify a story-level functional rule
- asks to map a story requirement to an existing screen or implementation artifact
- wants to refine one story without inventing unsupported behavior

Do not use this skill for:
- direct feature implementation
- unrelated refactors
- inventing product decisions without evidence

## Workflow

1. Start from the current story file under `features/<epic-jira-id>-<short-slug>/stories/`.
2. Read the current story first.
3. Expand into the parent Epic requirements file, sibling story files, implementation plan, task files, and design references only when that extra context is needed to answer the question.
4. If needed, inspect the current implementation to see whether behavior already exists in code.
5. When searching code, start with the module most relevant to the current story and expand only if that module does not answer the question.
6. Prefer existing evidence over assumptions.
7. If the answer is supported by documentation:
- update `Functional Requirements` in the current story when the rule is story-local
- keep the wording explicit, descriptive, and testable
- update the Epic requirements as well only when the clarification affects cross-story behavior
8. If the answer is supported by code:
- update `Functional Requirements` in the current story when the rule is story-local
- keep the wording explicit, descriptive, and testable
- add or update an `Open Questions` entry in the current story with:
- the question being clarified
- the answer derived from implementation
- status `Resolved`
- the exact code location where the answer was found
- a short explanation of the reasoning from implementation to documented behavior
- update the Epic requirements as well only when the clarification affects cross-story behavior
9. If the answer is not supported:
- add or update an `Open Questions` entry in the current story by default
- use the Epic `Open Questions` section only when the unresolved issue is genuinely broader than the current story
- do not silently invent the missing rule
10. When design exports partially conflict with requirements:
- keep the requirement explicit
- record the mismatch under the current story's `Design References`
- add an open question only when the mismatch represents a real unresolved decision
11. Keep all updates aligned with `AGENTS.md`.

## Source Priority

Use this order when deciding whether something is already answered for the current story:
1. Current story file
2. Epic requirements file
3. Implementation plan
4. Relevant task files
5. Relevant sibling story files
6. Exported screens and Figma references
7. Current implementation, starting from the most relevant module for the current story
8. Open questions already recorded

If sources disagree:
- prefer the more specific project documentation
- prefer the newer project documentation when recency is clear
- if still unclear, record an open question instead of forcing a conclusion

## Update Targets

Prefer these update targets:
- current story `Functional Requirements` for story-local behavior
- Epic requirements for cross-story rules
- current story `Design References` for screen mismatches
- current story `Open Questions` when evidence is missing
- current story `Open Questions` with status `Resolved` when the answer is derived from code and should retain an audit trail
- Epic `Open Questions` only for unresolved cross-story or Epic-wide issues

## Documentation Rules

Follow the repository rules from `AGENTS.md`, especially:
- Epic requirements file naming
- story file naming
- structured `Open Questions`
- `Design References` with per-screen headings and mismatch notes
- descriptive `Functional Requirements` written with headings and prose instead of flat bullet lists

## Response Expectations

After making changes:
- state whether the answer was found or remained open
- list which files were updated
- state whether the clarification was added to the current story's functional requirements or recorded as an open question
- if the answer came from code, include where it was found and summarize the implementation-based reasoning
