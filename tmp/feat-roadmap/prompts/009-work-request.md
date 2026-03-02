# Work Request: Step 009 — Preview Component (JSON/Cards toggle)

## Context

We are on `feat/zee-mode` inside `gravitee-apim-console-webui`.
Step 008 added `ZeeModule` and `ZeeWidgetComponent`. The widget currently has a basic `<pre>` tag for its "preview" state.

We now need to build the **real** preview component (`<zee-preview>`) that offers a dual-view toggle: raw JSON tree vs. human-readable structured cards.

## Frontend Tooling

```bash
cd gravitee-apim-console-webui
nvm use
yarn test --testPathPattern="zee"
```

## Your Task

Read:

- **Task file**: `tmp/feat-roadmap/009-preview-component.md`
- **Frontend widget design**: `tmp/feat-roadmap/frontend-widget.md`

## What to Create

All files go in `gravitee-apim-console-webui/src/shared/components/zee/`.

### 1. `zee-preview` Component

Create `zee-preview/zee-preview.component.ts` (along with html, scss, and spec).

**Inputs**:

- `data`: any (the generated JSON payload)
- `resourceType`: ZeeResourceType (to know which card to render)

**Template**:
Use `mat-tab-group` to provide two tabs:

1. **"Structured View"** (Default). Uses `[ngSwitch]="resourceType"` to render the specific card (e.g., `<zee-flow-card>`). Provide a fallback (e.g., just the JSON again) if the resource type doesn't have a specific card yet.
2. **"JSON"**. Displays the raw JSON. You can use `<pre>{{ data | json }}</pre>`, but if APIM has a preferred JSON syntax highlighter component currently in use, use that instead. (Check for something like `<gio-code-editor>` or similar).

### 2. `zee-structured-view/flow-card.component`

Create `zee-structured-view/flow-card.component.ts` (along with html, scss, and spec).

**Inputs**:

- `flow`: any (The generated Flow JSON structure)

**Template**:
Use `mat-card` to display the flow visually. The generated flow matches the `Flow` schema from `gravitee-api-definition-spec`. Important fields to display:

- `name` (header)
- `enabled` (chip/badge)
- `request`, `response`, `subscribe`, `publish` (lists of Steps). Show the step `name`, `policy`, and `description`. Use `mat-list` or just styled divs.
- `selectors` (list of Selector objects). Show the `type` and relevant routing info (e.g., `path` for HTTP). Use `mat-chip-listbox` or similar.
- `tags` (array of strings)

_Note: Since the backend currently returns the raw LLM output, step `configuration` fields will be strings (JSON encoded). You don't need to render the full configuration in this card layout unless it's easy, just the policy type and description._

### 3. Wire them into ZeeModule

- Declare and export `ZeePreviewComponent` and `FlowCardComponent` in `ZeeModule`.
- Add `MatTabsModule`, `MatCardModule`, `MatChipsModule`, `MatListModule`, etc., to the imports array.
- Update `zee-widget.component.html`: Replace `<pre>{{ generatedResource | json }}</pre>` with `<zee-preview [data]="generatedResource" [resourceType]="resourceType"></zee-preview>`.

## Acceptance Criteria

- [ ] `ZeePreviewComponent` toggles between tabs
- [ ] `FlowCardComponent` renders the flow details in a clean, human-readable format
- [ ] Both components are covered by Jest tests
- [ ] `yarn test` passes
- [ ] `ZeeModule` is updated
- [ ] Widget is updated to use the new preview

## Hard Constraints

- **Angular 19 / NgModule**: Stick to the `standalone: false` pattern.
- **Branch**: `feat/zee-mode`

## After Implementation

1. **Commit**: `feat(zee): add preview component with JSON/card toggle`
2. **Request external review** — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 009 — Preview Component

### What Was Done

- [files created]

### Decisions Made

- [how the Flow JSON structure was mapped to the UI (what did you hide vs show?)]
- [Did you use a specific syntax highlighter for the JSON view?]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [e.g., any difficulties matching the generated JSON shape to the UI]

### Test Results

- [yarn test output]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
