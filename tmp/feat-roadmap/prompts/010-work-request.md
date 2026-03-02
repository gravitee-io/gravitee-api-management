# Work Request: Step 010 — Flow Adapter & Console UI Integration

## Context

We are on `feat/zee-mode` inside `gravitee-apim-console-webui`.
Step 009 added the dual-view `<zee-preview>` component to the widget.
The Zee Mode widget is now fully functional in isolation. We need to actually hook it up to the APIM Console UI so users can generate flows.

## Frontend Tooling

```bash
cd gravitee-apim-console-webui
nvm use
yarn test --testPathPattern="zee|flow-adapter"
```

## Your Task

Read:

- **Task file**: `tmp/feat-roadmap/010-flow-adapter-integration.md`
- **Frontend widget design**: `tmp/feat-roadmap/frontend-widget.md`

## What to Create / Modify

### 1. `flow-adapter.ts`

Create `src/shared/components/zee/adapters/flow-adapter.ts` (and its `.spec.ts`).

This implements the `ZeeResourceAdapter` interface. Its job is to take the raw JSON emitted by the LLM (which matches the APIM Definition `Flow` schema) and map it into the exact payload expected by the V2 Management API for saving a flow.

Reference `tmp/feat-roadmap/010-flow-adapter-integration.md` for the exact mapping logic required. Pay special attention to the `configuration` field on steps, which arrives as stringified JSON and must be parsed back to an object.

### 2. Integrate Widget into Console UI

Find the page where API Flows are created/edited.

In the Console V2 API, flows are typically managed in the Policy Studio or the V4 API Flow setup pages.
Look for `api-v4-policy-studio-design.component.html` or similar.

Use `grep` to find the right place:

```bash
grep -rl "createFlow\|addFlow" gravitee-apim-console-webui/src/app/pages/api/v4/
```

**What to do when you find it**:

1. Import `ZeeModule` into the corresponding NgModule.
2. In the component controller, instantiate the `FlowAdapter`: `flowAdapter = FLOW_ADAPTER;`
3. Expose the `apiId` to the template context.
4. In the template (HTML), place the `<zee-widget>` at the top of the flow list or inside an "Add Flow" dialog/panel.

```html
<zee-widget [resourceType]="'FLOW'" [adapter]="flowAdapter" [contextData]="{ apiId: api.id }" (accepted)="onFlowGenerated($event)">
</zee-widget>
```

5. Implement the `onFlowGenerated(payload)` method to actually call the existing `FlowService` (or equivalent) to save the flow. Show a success/error snackbar based on the result.

### 3. Unit Tests

Write comprehensive unit tests for `flow-adapter.ts` verifying all fields map correctly and `configuration` is parsed correctly. Ensure the modified Console UI component's tests still pass.

## Acceptance Criteria

- [ ] `FLOW_ADAPTER` correctly implements `ZeeResourceAdapter`.
- [ ] `configuration` field on steps is parsed from JSON string to object.
- [ ] Selector types correctly mapped based on their discriminator format.
- [ ] `ZeeWidgetComponent` is rendered on the Flow creation page.
- [ ] Emitting `(accepted)` successfully calls the backend API to save the flow.
- [ ] Unit tests pass for the adapter and modified components.
- [ ] `yarn test` is green.

## Hard Constraints

- **Only existing API endpoints**: Use the existing `FlowService.createFlow` (or similar) method. Do not invent new backend APIs for saving the flow. The adapter's job is to bridge that gap.
- **Branch**: `feat/zee-mode`

## After Implementation

1. **Commit**: `feat(zee): integrate Zee Widget into flow creation page`
2. **Request external review** — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 010 — Flow Adapter Integration

### What Was Done

- [files created/modified]
- [where specifically the widget was injected in the UI]

### Decisions Made

- [any special mapping logic required in the adapter]
- [how you wired the accepted event to the save action]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [e.g., mismatched payload formats discovered during development]

### Test Results

- [yarn test output]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
