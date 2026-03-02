# Work Request: Step 012 — Polish & Error Handling

## Context

We are on `feat/zee-mode`. All core Zee Mode functionality is complete — full pipeline from UI to Azure OpenAI and back, for all 5 resource types (Flow, Plan, Endpoint, Entrypoint, Api). We currently have 80+ passing tests.

This is the final step: **polish, edge cases, and UX refinements** before we consider v0.1 done.

## Frontend Tooling

```bash
cd gravitee-apim-console-webui
nvm use
yarn test --testPathPattern="zee|adapter"
```

## Backend Tooling

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn compile -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service -DskipTests -Dskip.validation -am -T 2C
```

## What to Do

Read:

- **Task file**: `tmp/feat-roadmap/012-polish-error-handling.md`

### 1. Frontend: File upload validation

The `ZeeWidgetComponent` accepts file uploads via the `<input type="file">` dropzone. Currently, no limits are enforced.

Add validation:

- **Max files**: 5 files per request
- **Max file size**: 5MB per file (configurable via `@Input() maxFileSizeMb = 5`)
- **Allowed types**: json, yaml, txt, md only (whitelist by extension)
- Show error messages directly in the UI via Angular Material form field errors or a `mat-error` element when the user adds a disallowed file.

### 2. Frontend: Rate limit UX

The backend enforces 10 requests per environment per 60 seconds but currently returns a 429 response with no user-friendly explanation.

On a `429` response:

- Show a banner/`mat-snack-bar` message: _"Zee is cooling down — you've hit the request limit. Please wait a minute and try again."_
- Do **not** leave the widget in `error` state for rate limiting — drop it back to `idle` state after showing the snackbar so the user can retry.

### 3. Frontend: Empty/null state

When the LLM returns an empty or invalid JSON result (e.g., a `null` or malformed `generated` field), the preview should degrade gracefully:

- `FlowCardComponent` (and all other cards) should handle `null` / missing fields without throwing template errors.
- If `generated` is entirely null, show a friendly message in the preview: _"Zee couldn't generate a result. Try rephrasing your prompt."_

### 4. Backend: Input validation

In `ZeeResource.java`, add basic server-side validation before calling `GenerateResourceUseCase`:

- **Prompt max length**: Reject prompts > 2000 chars with a `400 Bad Request`. Return `{ "error": "Prompt too long. Max 2000 characters." }`
- **File count**: Reject if more than 5 files uploaded. Return `400`.
- **File size**: Reject if any file exceeds 5MB. Return `400`.

### 5. UX: Prompt character counter

In `zee-widget.component.html`, add a character counter below the prompt textarea:

```html
<mat-hint align="end">{{ prompt.length }} / 2000</mat-hint>
```

And disable the "Generate" button if the prompt is empty OR over 2000 chars.

## Acceptance Criteria

- [ ] File validation enforced on frontend (type, size, count) with user-facing messages
- [ ] 429 rate limit response handled gracefully (snackbar, back to idle)
- [ ] Null/empty generated data handled in all cards without UI crash
- [ ] Backend validates prompt length, file count, and file size (400s returned correctly)
- [ ] Character counter shown in widget
- [ ] `yarn test` passes
- [ ] `mvn compile` passes
- [ ] No regressions in any existing tests

## Hard Constraints

- **No new dependencies**: No new Angular or Java libraries. Use `MatSnackBar`, existing form validation, and JAX-RS `Response.status()` respectively.
- **Branch**: `feat/zee-mode`

## After Implementation

1. **Commit**: `feat(zee): add input validation, rate limit UX, and error handling polish`
2. **Request external review** — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 012 — Polish & Error Handling

### What Was Done

- [files modified]

### Decisions Made

- [How you chose to surface the validation errors (inline vs. snackbar vs. banner)]

### Deviations from Plan

- [anything different — and why]

### Test Results

- [yarn test output with total count]
- [mvn compile: BUILD SUCCESS]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

This is the final step — confirm the feature is complete!
```
