---
name: APIM Angular Conventions
layer: local
description: Angular conventions for APIM's frontend applications and libraries - component structure, state, templates, naming, styling, and testing
---

# APIM Angular Conventions

## Absolutes

- **Standalone only:** every component, directive, and pipe is `standalone: true`.
- **Signals first:** signals for local state (`signal()`), derived state (`computed()`), inputs (`input()` / `input.required()`), and outputs (`output()`).
- **New control flow strictly:** `@if`, `@for`, `@switch` — never `*ngIf` or `*ngFor`.
- **Design system:** do not invent new UI patterns; reuse existing components, and suggest collaborating with UX when a new one is needed.

## Components

- Class member order: 1. `private readonly` injections (`inject()`), 2. inputs, 3. outputs, 4. state, 5. computed (`computed()`, `toSignal()`, `rxResource()`), 6. methods (public then private).
- DI: always `private readonly service = inject(ServiceName);` — never constructor injection, and no constructor or `ngOnInit` subscription setup.
- Smart components fetch data and hold business logic; dumb components are purely presentational (inputs in, outputs out).

## State and RxJS

- Consuming observables, in preference order: `rxResource` → `observable$ | async` → `toSignal(...)` → `.subscribe()` (side effects only, never component state). The decision table, `rxResource` patterns, and exemplar components live in `.ai/guides/angular-async-patterns.md`.
- Prefer a single reactive expression — don't store a private observable in one field and convert it in another.
- `.subscribe()` requires `takeUntilDestroyed(this.destroyRef)`; no nested subscriptions — use `switchMap`, `mergeMap`, and friends.

## Templates

- Never bind to methods in templates — use `computed` signals or pipes.
- No inline styles and no hard-coded hex values — use CSS variables / design tokens.
- Semantic HTML over ARIA retrofits (`<button>`, not `<div role="button">`).

## Forms and dialogs

- Always typed forms.
- Strictly type dialog opens: `this.matDialog.open<ComponentType, InputType, OutputType>(...)`.

## Naming

- Functions: verb + noun that reflects the operation (extract, collect, derive, compute, find).
- Variables: name the concept they hold, not the type; no contractions (`comp`, `el`, `attr`) unless established in the file.
- Booleans: `is` / `has` / `can` / `should` prefixes. Observables: `$` suffix.

## Utilities and styling

- Check for an existing utility before writing one; use lodash for common transforms (`kebabCase`, `isEmpty`, `isEqual`, `merge`, ...).
- Never `:ng-deep`. Modify Material components through CSS tokens, not global class overrides.

## Testing

- Use component harnesses for all interactions, composed along the component hierarchy; never DOM queries (`querySelector`, `debugElement.query`).
- Prefer `data-testid` or accessibility-oriented selectors over brittle DOM structure.
- When a component's services call the backend, use `HttpTestingController` rather than mocking the services.
- Await all promises; use `fixture.destroy()` instead of `discardPeriodicTasks`.
- **Never use `HttpClientTestingModule` or `AppTestingModule` with `rxResource` components** — tests hang on `await fixture.whenStable()`. Use the standalone provider style; the full setup and flush pattern is in `.ai/guides/angular-async-patterns.md`.
