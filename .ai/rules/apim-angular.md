---
name: APIM Angular Conventions
layer: local
dirs:
  - gravitee-apim-console-webui
  - gravitee-apim-portal-webui-next
  - gravitee-apim-webui-libs/gravitee-dashboard
  - gravitee-apim-webui-libs/gravitee-kafka-explorer
  - gravitee-apim-webui-libs/gravitee-markdown
description: Angular deltas for APIM's frontend applications and libraries - the settled absolutes, member order, DI, dialogs, naming, and test teardown
---

# APIM Angular Conventions

## Absolutes

The shared Angular rule's "where the repo has adopted them" conditions are settled here — these are binary:

- **Standalone:** new components, directives, and pipes are standalone — on this Angular version that is the default, so do not write `standalone: true`. Do not convert existing `standalone: false` declarations as a side effect of an unrelated change.
- **Signals first:** signals for local state (`signal()`), derived state (`computed()`), inputs (`input()` / `input.required()`), and outputs (`output()`).
- **New control flow strictly:** `@if`, `@for`, `@switch` — never `*ngIf` or `*ngFor`.

## Components

- Class member order: 1. `private readonly` injections (`inject()`), 2. inputs, 3. outputs, 4. state, 5. computed (`computed()`, `toSignal()`, `rxResource()`), 6. methods (public then private).
- DI: always `private readonly service = inject(ServiceName);` — never constructor injection, and no constructor or `ngOnInit` subscription setup.
- The destroy pattern the shared Angular rule refers to is `takeUntilDestroyed(this.destroyRef)` — every `.subscribe()` gets it.
- When no existing design-system component or pattern fits, suggest collaborating with UX rather than inventing a new one.
- The consuming-observables decision table, `rxResource` patterns, and exemplar components live in `.ai/guides/angular-async-patterns.md` (a repository-root path) — read it before wiring async data.

## Dialogs

- Strictly type dialog opens: `this.matDialog.open<ComponentType, InputType, OutputType>(...)`.

## Naming

- Functions: verb + noun that reflects the operation (extract, collect, derive, compute, find).
- Variables: name the concept they hold, not the type; no contractions (`comp`, `el`, `attr`) unless established in the file.
- Booleans: `is` / `has` / `can` / `should` prefixes. Observables: `$` suffix.

## Utilities and testing

- Use lodash for common transforms (`kebabCase`, `isEmpty`, `isEqual`, `merge`, ...) before writing a new utility.
- Use `fixture.destroy()` instead of `discardPeriodicTasks`.
