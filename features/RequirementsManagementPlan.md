# Requirements Management Plan

## Purpose
This document defines how feature requirements and implementation-planning artifacts are documented under `features/`.

Use it as the source of truth for:
- Epic-scoped documentation structure
- relationships between requirements, stories, decisions, meetings, screens, and implementation plans
- required content for Epic and story documents
- implementation plan and implementation task conventions

## Artifact Model

### Source of Scope
- A feature is a Jira Epic.
- User stories linked to that Epic define the implementable functional slices.
- Ignore child stories whose title is exactly `Placeholder`; they must not drive documentation structure or delivery scope.

### Documentation Flow
- The Epic requirements document is the main analysis entry point for one Epic.
- Story documents refine the Epic into concrete implementable slices.
- Decision records capture durable Epic-level decisions.
- Meeting notes capture agendas, clarification discussions, and meeting outcomes.
- Screen exports capture design evidence and should be referenced from requirements and story files when available.
- The implementation plan translates the Epic requirements into phased engineering work.
- Implementation task files break the implementation plan into executable technical increments.

### Traceability Rules
- Requirements, stories, decisions, meetings, and implementation planning must stay aligned.
- Implementation plans must derive from the Epic requirements and relevant story files.
- Decision records must be indexed from the Epic requirements document.
- Story files should reference concrete design artifacts when available.
- Meeting notes do not replace decision records; if a meeting produces a durable Epic-level decision, also capture it as a decision record.

## Epic Directory Structure

### Epic Setup
For each Epic:
1. Create a directory under `features/`.
2. Name the directory `<epic-jira-id>-<short-slug>`.
3. Use the Epic issue key as the prefix, for example: `APIM-10345-member-mapping-to-application`.
4. Keep the slug short, descriptive, lowercase, and hyphen-separated.
5. Create an Epic-level `<epic-jira-id>-requirements.md` file inside that directory.
6. Create a `screens/` subdirectory for screen assets or exported designs from Figma.
7. Create a `decisions/` subdirectory for project decision records related to that Epic.
8. Create a `meetings/` subdirectory for meeting notes related to that Epic.
9. Create a `stories/` subdirectory for the Epic's relevant user stories.
10. Create an `impl/` subdirectory when implementation task files are needed.
11. Create or update an Epic-level `<epic-jira-id>-implementation-plan.md` file when implementation planning starts.

### Expected Structure
- `features/<epic-jira-id>-<short-slug>/<epic-jira-id>-requirements.md`
- `features/<epic-jira-id>-<short-slug>/<epic-jira-id>-implementation-plan.md`
- `features/<epic-jira-id>-<short-slug>/screens/`
- `features/<epic-jira-id>-<short-slug>/decisions/`
- `features/<epic-jira-id>-<short-slug>/meetings/`
- `features/<epic-jira-id>-<short-slug>/stories/`
- `features/<epic-jira-id>-<short-slug>/impl/`

### Naming Rules
- Decision records: `<epic-jira-id>-decision-<decision-order-2d>-<short-slug>.md`
- Meeting notes: `YYYY-MM-DD-<short-slug>.md`
- Story files: `<epic-jira-id>-story-<story-order-2d>-<story-jira-id>-<short-slug>.md`
- Implementation task files: `<epic-jira-id>-phase-<phase-number-2d>-task-<task-number-2d>-<area>-<short-slug>.md`

### Epic-Scoped Identifiers
- For each major project decision, assign a stable two-digit decision sequence prefix scoped by the Epic key, for example `APIM-10345-decision-01`, `APIM-10345-decision-02`.
- For each non-placeholder user story, assign a stable two-digit story sequence prefix scoped by the Epic key, for example `APIM-10345-story-01`, `APIM-10345-story-02`, `APIM-10345-story-03`.
- Use the same Epic-scoped story identifier in both `<epic-jira-id>-requirements.md` and the story filename.

## General Documentation Rules
- Write requirements, implementation plans, and implementation task descriptions in English.
- Do not use tables; use headings and bullet lists.
- Keep the documents updated when scope or decisions change.
- Store exported Figma screens, screen captures, or design handoff artifacts in the Epic-level `screens/` directory.
- Store major project decision documents in the Epic-level `decisions/` directory.
- Store meeting agendas, clarification notes, and captured meeting outcomes in the Epic-level `meetings/` directory using `YYYY-MM-DD-<short-slug>.md` filenames.

## Epic Requirements Documentation

### Purpose
- The Epic-level document must describe the overall functional scope, boundaries, and open questions for the feature.
- The Epic-level document must include a section that lists major project decisions and links to the detailed decision records stored under `decisions/`.

### Required Epic Requirements Sections
Each Epic `<epic-jira-id>-requirements.md` must use the following sections in this order:
- `# <Epic key> <Epic title>`
- `## Jira References`
- `## Figma References`
- `## Context`
- `## Problem Statement`
- `## Goal`
- `## Scope`
- `## Out of Scope`
- `## Impacted Modules`
- `## Domain Model`
- `## User Stories in Scope`
- `## Suggested Implementation Order`
- `## Functional Requirements`
- `## Non-Functional Requirements`
- `## Cross-Story Rules and Constraints`
- `## Acceptance Criteria`
- `## Project Decisions`
- `## Open Questions`
- `## Assumptions`
- `## Dependencies`

### Epic Section Guidance
- `# <Epic key> <Epic title>`: Use the Epic Jira key and summary as the document title.
- `## Jira References`: Link the Epic and list any directly related Jira issues that provide source context.
- `## Figma References`: Link the relevant Figma files, pages, or frames that define or clarify the expected UX and UI behavior. Identify which designs are the source of truth when multiple variants exist, and reference any exported artifacts stored in the local `screens/` directory.
- `## Context`: Describe the product or business context needed to understand the feature.
- `## Problem Statement`: Explain the current gap, pain point, or opportunity.
- `## Goal`: State the intended outcome in concrete product terms.
- `## Scope`: Define what is included in the Epic-level delivery.
- `## Out of Scope`: Explicitly list what is excluded to prevent scope drift.
- `## Impacted Modules`: List backend, frontend, gateway, documentation, or other impacted areas.
- `## Domain Model`: Describe the business concepts that matter for the current Epic scope, using domain language only. Treat it as a map of concepts, relations, and business-significant attributes, not as a technical data model or implementation design.
- `## User Stories in Scope`: List the non-placeholder stories covered by this Epic, with one short summary per story.
- `## Suggested Implementation Order`: Propose an implementation sequence for the in-scope stories so that each later story builds on earlier groundwork when possible. Use Markdown subheadings for each story instead of a numbered list. Prefix each subheading with the Epic-scoped story identifier. For each item, include the Jira key, the story title, its suggested order, the prerequisite stories, and a short explanation of why those dependencies exist. When linking Jira issues in this section, always include both the Jira key and the issue summary.
- `## Functional Requirements`: Capture the Epic-level behavior and rules that span one or more stories.
- `## Non-Functional Requirements`: Capture performance, security, usability, observability, compliance, or operational constraints.
- `## Cross-Story Rules and Constraints`: Document rules that must stay consistent across all story implementations.
- `## Acceptance Criteria`: Define testable Epic-level outcomes.
- `## Project Decisions`: List the major project decisions that shape the Epic, with links to the detailed decision records in `decisions/`.
- `## Open Questions`: Track unresolved product, UX, technical, or dependency questions. For each item, include the question itself, the current answer or `TBD`, and an explicit status such as `Open` or `Resolved`.
- `## Assumptions`: Record assumptions currently used to move forward.
- `## Dependencies`: List external systems, teams, feature flags, migrations, or prerequisite work.

### Domain Model Guidance
- Build the domain model before `## User Stories in Scope`.
- Use business language and describe the problem domain, not the system architecture.
- Keep the model limited to concepts relevant to the current Epic scope.
- Include business concepts, business-significant relations, and business-significant attributes only.
- Do not mix analysis with implementation design, API structure, database structure, DTOs, or UI component structure.
- Do not collapse important concepts into plain text fields when they carry distinct business meaning.
- Distinguish an object from its description, specification, invitation, or assignment when that distinction matters to the business rules.
- Prefer short Markdown subheadings per concept with concise prose describing what the concept is, how it relates to other concepts, and which attributes matter for the business behavior.

### Project Decision Guidance
- Use the `decisions/` directory for major project decisions, not for small clarifications that belong in `Open Questions`.
- Use project decision records when the topic has meaningful scope, delivery, UX, domain, or architecture impact and requires discussion across multiple responsible people.
- A project decision record should capture the decision topic, the options considered, arguments for and against each option, the people involved in the discussion, and the agreed direction.
- A decision record may be drafted before the decision is actually made, but in that case it must clearly say that the topic is still under discussion.
- Do not mark a decision as agreed, accepted, or resolved unless the record includes the agreed direction and the concrete full names of the people involved in making that decision.
- An AI agent may draft or refine a project decision record, but it must not independently make or mark a project decision as agreed.
- The Epic `## Project Decisions` section should act as an index of decision records.

## Story Documentation

### Story Minimum Content
Each story file should include at least:
- Jira Story reference
- Relationship to the parent Epic
- Design references when available
- Functional requirements
- Acceptance criteria
- Edge cases and failure cases
- Open questions
- Test considerations

### Story Design Reference Guidance
- When possible, link each story to one or more concrete Figma frames or exported screens from the Epic `screens/` directory.
- In story documents, structure `## Design References` so each screen or design artifact is described under its own Markdown subheading.
- Use one Markdown subheading per screen, frame, or other design artifact inside `## Design References`.
- When a local exported screen exists, include both its relative path and an embedded image preview.
- After each screen preview, document what part of the story it supports.
- Under each screen subheading, explicitly note any mismatch, missing state, or ambiguity when the screen content does not fully reflect the story's `Functional Requirements`.
- If no exported screen exists for a story, state that explicitly in `## Design References` and explain what design source, if any, is currently being used instead.

### Story Functional Requirements Guidance
- In story documents, prefer `## Functional Requirements` written under short Markdown subheadings with descriptive prose.
- Avoid using a flat bullet list as the primary way to describe story functional requirements.
- Use bullets inside `## Functional Requirements` only when a small finite enumeration is genuinely clearer than prose.
- The goal of `## Functional Requirements` is to make the intended behavior easy to elaborate in detail, so favor short paragraphs over terse one-line bullets.

### Open Question Format
- In both Epic and story documents, each open question entry must include:
- A stable identifier prefixed with the Jira issue key, for example `APIM-10345-OQ-1` or `APIM-12280-OQ-2`
- `Question`: the decision or uncertainty that needs clarification
- `Answer`: the current answer, decision, or `TBD`
- `Status`: `Open` or `Resolved`

## Implementation Planning Documentation

### Structure
- Each Epic must store its implementation plan in `features/<epic-jira-id>-<short-slug>/<epic-jira-id>-implementation-plan.md`.
- Each Epic must store detailed implementation task descriptions in `features/<epic-jira-id>-<short-slug>/impl/`.
- Use the templates:
- `features/_templates/implementation-plan.md`
- `features/_templates/impl-task.md`

### Purpose
- The implementation plan translates the Epic requirements document into executable engineering work.
- It must describe how the Epic will be delivered incrementally across frontend, backend, contract, integration, and other technical work when needed.
- It must stay aligned with the related `<epic-jira-id>-requirements.md`, story files, open questions, and design references.

### Plan Rules
- `<epic-jira-id>-implementation-plan.md` is the main entry point for implementation planning inside the Epic directory.
- It must describe phases and implementation tasks.
- Phases must group tasks into coherent, testable increments.
- A phase should represent a closed slice of work where backend and frontend can be validated together when applicable.
- A task does not need to map one-to-one to a Jira story.
- A task may describe enabling work such as API contracts, shared models, migrations, integration wiring, or other technical foundations.
- Test-only tasks are not allowed.
- Each task must include implementation changes and the tests or validation needed for that task.
- The implementation plan should reference concrete files when that information is already known with reasonable confidence.
- The implementation plan may stay at a summary level, but each task file must capture file-level planning in more detail.

### Expected File Layout
- `features/<epic-jira-id>-<short-slug>/<epic-jira-id>-implementation-plan.md`
- `features/<epic-jira-id>-<short-slug>/impl/`
- `features/<epic-jira-id>-<short-slug>/impl/<epic-jira-id>-phase-<phase-number-2d>-task-<task-number-2d>-<area>-<short-slug>.md`

### Required Plan Content
- Title
- Related requirements references
- Scope and impacted modules
- Assumptions and constraints
- Phase breakdown
- Task breakdown per phase
- Validation approach
- Open issues or follow-up items

### File Planning Rules
- Each implementation task file must include a dedicated section describing planned file changes.
- Planned file changes must explicitly call out which files are expected to be created, updated, or deleted.
- When the exact file is not yet known, document the narrowest reliable target, for example a concrete module plus the likely package or directory.
- File planning should distinguish between:
- `Create`: new files that are expected to be introduced
- `Update`: existing files that are expected to be modified
- `Delete`: existing files that are expected to be removed
- Do not leave file planning implicit in generic module names when a concrete file path can already be identified.
- If a task intentionally avoids file-level planning because the target is still unknown, state that explicitly as an open question or blocker instead of omitting the detail.

### API Contract Planning Rules
- When a task introduces or changes an HTTP API contract, the task file must include a dedicated `API Contract Changes` section.
- That section must describe each endpoint change separately.
- For each endpoint, document:
- HTTP method
- Path
- Path parameters
- Query parameters
- Request body when applicable
- Expected responses, including the main success response and important error responses when they are part of the contract behavior
- Response payload shape at a level sufficient to understand the delivery impact
- If a task does not change an HTTP contract, state `None` in `API Contract Changes` instead of skipping the section.
- Apply the same rule to portal, management, admin, gateway, or any other HTTP-facing contract surface.

### Phase Rules
- Each phase must have a clear goal.
- Each phase must explain why it is a coherent increment.
- Each phase must list the tasks it contains.
- Each phase must define how the completed increment is validated.

### Task Rules
- Each task must have a short title.
- Each task must state its area: `frontend`, `backend`, `contract`, `integration`, `mixed`, or another explicit technical area.
- Each task must describe the concrete implementation changes.
- Each task must enumerate planned file changes.
- Each task must include `API Contract Changes`, even when the answer is `None`.
- Each task must list impacted modules.
- Each task must reference related stories when applicable.
- Each task must define the tests or validation needed as part of the task.
- Each task file should focus only on the task itself.
- Do not document task-to-task dependencies inside the task file.

### Dependency Rules
- The implementation plan must propose task dependencies explicitly.
- For each task dependency, do not list only the prerequisite task identifiers.
- Also include a short explanatory reason describing why the dependency exists.
- Dependency explanations should make clear whether the dependency comes from shared contracts, data flow, UI composition, backend capability, migration ordering, or another technical constraint.
- Document task dependencies in `<epic-jira-id>-implementation-plan.md`, not in the individual task files.
- Order phases and tasks so that later work can build on earlier technical foundations.
- Prefer delivering phases that can be validated end-to-end instead of collecting disconnected backend-only and frontend-only tasks without a testable increment.

### Implementation Start
- Create or update `features/<epic-jira-id>-<short-slug>/<epic-jira-id>-implementation-plan.md`.
- Create `features/<epic-jira-id>-<short-slug>/impl/` when implementation task files are needed.
- Initialize the implementation plan and task files from the templates under `features/_templates/`.
- Ensure implementation scope stays aligned with the Epic `<epic-jira-id>-requirements.md` and the relevant story files.
- Resolve open questions before finalizing the change when possible, or explicitly document what remains unresolved.

### Definition of Readiness
- A feature is ready for implementation when the Epic `<epic-jira-id>-requirements.md` exists, relevant non-placeholder story files exist, scope is explicit, acceptance criteria are testable, and open questions are clearly listed.
