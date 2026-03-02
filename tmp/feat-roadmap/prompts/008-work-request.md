# Work Request: Step 008 — Frontend Zee Module & Widget

## Context

Zee Mode backend is fully complete on `feat/zee-mode`! We are now moving to the **Frontend** phase.
The goal is to build the generic AI chat widget (`<zee-widget>`) for the Console UI (Angular 19).

You are working on the `feat/zee-mode` branch in `~/workspace/Gravitee/gravitee-api-management`.

## Frontend Tooling

All work happens inside `gravitee-apim-console-webui`.

```bash
cd gravitee-apim-console-webui
nvm use  # loads Node 20.19.0
yarn     # yarn 4.1.1
```

Testing:

```bash
yarn test --testPathPattern="zee"
```

## Your Task

Read:

- **Task file**: `tmp/feat-roadmap/008-frontend-zee-module-widget.md`
- **Frontend widget design**: `tmp/feat-roadmap/frontend-widget.md`

## What to Create

All files go in `gravitee-apim-console-webui/src/app/shared/components/zee/`.

### 1. `zee.model.ts`

Implement the TypeScript interfaces matching the backend:

- `ZeeResourceType` (enum: `FLOW`, `PLAN`, `API`, `ENDPOINT`, `ENTRYPOINT`)
- `ZeeResourceAdapter` (interface with `transform` and `previewLabel`)
- `ZeeGenerateRequest`
- `ZeeGenerateResponse`

### 2. `zee.service.ts`

HTTP service to hit the `/v2/ai/generate` endpoint. **Important**: It must send `multipart/form-data`. Use Angular's `HttpClient` and `FormData`.

```typescript
// The URL should use the environment constants
import { Constants } from "@gravitee/ui-environments";
// ... `${this.constants.env.v2BaseURL}/ai/generate`
```

_(Note: Find how `Constants` is injected in existing Console UI services. It's often `@Inject("Constants")`)_

### 3. `zee-widget/` Component

The core component managing the state machine:

- **Inputs**: `resourceType`, `adapter`, `contextData`
- **Outputs**: `accepted`, `rejected`
- **State**: `idle` → `loading` → `preview` → `error`

- **Idle**: Show a `textarea` for the prompt and a dropzone for files. Use `MatInput` and a standard HTML file input.
- **Loading**: Show a `MatProgressSpinner`.
- **Preview**: For this step, just render the generated JSON in a `<pre>` tag. (Step 009 will build the complex dual-view preview). Show "Accept" and "Reject" buttons.
- **Error**: Show the error message and a "Try Again" button.

### 4. `zee.module.ts`

Declare `ZeeWidgetComponent`, export it, and import necessary Angular Material modules (`MatButtonModule`, `MatProgressSpinnerModule`, `MatInputModule`, `MatIconModule`, `ReactiveFormsModule`, etc.).

## Acceptance Criteria

- [ ] Model, Service, Component, and Module created in `src/app/shared/components/zee/`
- [ ] Component handles all 4 states correctly
- [ ] Service correctly constructs a `FormData` payload for the backend
- [ ] Compiles successfully (`yarn build` or `yarn test` passes)
- [ ] Unit tests for `ZeeWidgetComponent` (mocking the service and testing state transitions) and `ZeeService` (mocking `HttpClient`)

## Hard Constraints

- **Angular 19**: Use appropriate Angular features, but this is an existing codebase. Follow their patterns (NgModule, RxJS Observables, Jest tests).
- **No Standalone Components**: The council decided to stick to `NgModule` (`ZeeModule`) to match the surrounding Console UI architecture.
- **Styling**: `zee-widget.component.scss` should use SCSS. Keep it clean and follow standard BEM/Material styling where possible.

## After Implementation

1. **Commit**: `feat(zee): add Zee Widget Angular component`
2. **Request external review** — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 008 — Frontend Zee Module & Widget

### What Was Done

- [files created with full paths]

### Architecture Compliance

- [confirm: Used NgModule instead of standalone component]

### Decisions Made

- [how file selection/drag-and-drop was handled]
- [how Constants was injected in the service]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [surprises or things affecting future steps]

### Test Results

- [output of `yarn test --testPathPattern="zee"`]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
