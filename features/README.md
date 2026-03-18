# Features Process Overview

This directory contains feature documentation prepared from a product and delivery perspective.

The detailed source of truth for the process is [RequirementsManagementPlan.md](./RequirementsManagementPlan.md).
This `README.md` is only a short overview of how the process fits together.

The goal of the process is simple:
- start from a Jira Epic and its related work
- understand the business scope and design context
- describe the feature in a structured way
- split it into clear stories
- clarify stories iteratively
- prepare an implementation plan that can later be detailed into phases and tasks

## What Starts the Process

The process starts with a Jira Epic.

The Epic is treated as the main feature container. From there, the first step is to gather the linked Jira items that help explain the feature:
- in-scope user stories
- related design tickets
- related documentation tickets
- other linked issues that explain scope, dependencies, or product intent

Child stories whose title is exactly `Placeholder` are ignored. They must not drive documentation structure or delivery scope.

At this stage, the goal is not to plan implementation yet. The goal is to understand what problem the Epic is trying to solve and what functional slices already exist in Jira.

## Creating the Epic Directory

Each Epic gets a dedicated directory under `features/` named:
- `<epic-jira-id>-<short-slug>`

The expected structure is:
- `<epic-jira-id>-requirements.md`
- `<epic-jira-id>-implementation-plan.md`
- `screens/`
- `decisions/`
- `meetings/`
- `stories/`
- `impl/` when detailed implementation task files are needed

This structure keeps requirements, design evidence, clarification records, and implementation planning in one place.

## Adding Design Context

After the Jira scope is collected, the next step is to add design context from Figma.

This gives the feature a visual reference point:
- which screens already exist
- which states are covered by design
- which details are still missing or ambiguous

If screen exports are available, they are stored inside the Epic directory under `screens/`. This makes the feature package self-contained and easier to review.

Meeting preparation and clarification notes should be stored under `meetings/`. If a meeting leads to a durable Epic-level decision, that outcome must also be captured as a decision record under `decisions/`.

## Creating the Epic Requirements

Once the Jira and Figma context is known, the Epic gets its main requirements document.

That document explains:
- the feature context
- the problem being solved
- the business goal
- what is in scope and out of scope
- which areas are impacted
- the domain model for the current scope
- the list of major project decisions
- the stories covered by the Epic

The domain model is important because it helps describe the business world before implementation starts. It focuses on business concepts and relationships, not on APIs, database structures, or UI components.

The requirements package can also include major project decisions. These are different from open questions:
- open questions capture something that is still unresolved
- project decisions capture a deliberate choice made between meaningful options after discussion

Project decisions should summarize the options considered, the arguments for and against each option, the people involved, and the agreed direction.

## Splitting the Epic into Stories

After the Epic requirements are written, the feature is split into story documents.

Each story document should describe one functional slice clearly enough to review, clarify, test, and later implement. The story should stay close to business behavior and user expectations, while still being precise about:
- what the user can do
- what the UI should show
- what edge cases matter
- what is still unresolved

Stories should also be connected to the most relevant Figma screens whenever possible.

Each story file should contain at least:
- the Jira story reference
- the relationship to the parent Epic
- design references when available
- functional requirements
- acceptance criteria
- edge cases and failure cases
- open questions
- test considerations

## Clarifying Stories

Story clarification is an iterative step. It happens when someone reviews a story and raises a question such as:
- what exactly should happen in a given state
- which data should be shown or searched
- whether the design really supports the written requirement
- whether the current implementation already answers part of the question

The preferred workflow for this step uses the local skill `story-clarifier`.

Its role is:
- to start from one story
- to inspect the Epic and related stories only when needed for context
- to inspect the codebase when the answer may already exist in implementation
- to update the story when the answer is known
- or to add an open question when the answer still does not exist

Useful prompt examples:

```text
Use story-clarifier for APIM-10345-story-01-APIM-12280-view-existing-application-members.md and clarify the default sorting order.
```

```text
Use story-clarifier for APIM-10345-story-02-APIM-11539-search-among-existing-members.md and clarify which fields should be searched.
```

```text
Use story-clarifier for <story-file> and check whether the answer already exists in code or requirements.
```

This step is expected to happen many times. Requirements improve gradually through review and clarification.

## Preparing the Implementation Plan

When the Epic requirements and stories are clear enough, the next step is to prepare the implementation plan.

The implementation plan does not repeat the product requirements. Instead, it translates them into a delivery path:
- which increments should be built first
- how work should be grouped into phases
- which implementation tasks are needed
- which tasks depend on earlier work

The plan should still be readable at a high level. Its purpose is to show how the feature can be delivered step by step in a coherent order.

## Breaking the Plan into Phases and Tasks

The implementation plan is then expanded into:
- phases
- implementation tasks

A phase groups work into a coherent slice that can be validated as a meaningful increment.

A task describes one concrete piece of implementation work. Tasks may concern frontend, backend, contracts, integrations, or another area. Later, those tasks can be refined further as understanding improves.

Implementation planning follows a few important rules:
- test-only tasks are not allowed
- every task must include both implementation changes and the validation needed for that slice
- implementation tasks should reference concrete files when they are known with reasonable confidence
- if a task changes an HTTP API contract, it must describe the endpoint method, path, parameters, request body when applicable, and expected responses
- if a task does not change an HTTP API contract, it should state that explicitly

Detailed implementation tasks belong in `impl/`, while `<epic-jira-id>-implementation-plan.md` stays the high-level entry point for phases and dependencies.

This means the process does not stop when the first implementation plan is written. The plan and tasks are expected to become more precise over time, just like the stories do.

## How the Whole Process Fits Together

In practice, the flow looks like this:
- identify the Epic and gather Jira context
- add Figma context and exported screens
- write the Epic requirements
- describe the domain model
- split the Epic into story documents
- capture meeting notes and major project decisions when needed
- clarify stories iteratively
- prepare the implementation plan
- break implementation into phases and tasks
- continue refining stories and tasks when new questions appear

The result should be a feature package that lets a reader understand:
- what the feature is
- why it exists
- how it is divided into stories
- what is still unclear
- how implementation is expected to proceed

That is the purpose of this directory.
