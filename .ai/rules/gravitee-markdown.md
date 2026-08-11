---
name: Gravitee Markdown Library Conventions
layer: local
dirs: [gravitee-apim-webui-libs/gravitee-markdown]
description: gravitee-markdown (GMD) library conventions - the full file set for a new component, public-api and suggestions registration, the SCSS overrides pattern, and gmd- naming
---

# Gravitee Markdown Library Conventions

Not a design system; these rules define how GMD components are structured, styled, and tested.

## Full file set for a new GMD component

Create the complete set: component `.ts`, `.html`, `.scss`; a `.spec.ts`; a component harness; Storybook stories plus a dedicated stories scss for story-level overrides; registration in the suggestions system; `_overrides.scss` (or component-level overrides) for token/theme overrides; an export via the library's public API; and README documentation (library or component level, as appropriate).

- Every public GMD component is exported through the public API (e.g. `public-api.ts`) **and** registered in the suggestions registry so it appears in GMD suggestion lists — never ship one without both.
- Each GMD component has a harness, used in unit tests and (where applicable) integration tests.

## GMD SCSS pattern

- Use shared token utilities for colors, spacing, and typography where provided.
- Use an `_overrides` partial with a `tokens()` (or equivalent) and an overrides mixin so the main app or Storybook applies overrides in one place.
- Use `slot()` (or the project's slot mixin) for slotted content styling; do not rely on global element selectors.
- Forward component styles and overrides through the library's SCSS public API so consumers import one entry point (e.g. `@use '.../public-api'`).

## Storybook

- Stories use the component's public API for overrides — the same entry point and mixin/tokens as the main app; do not duplicate override logic in stories.

## Naming

- Use the `gmd-*` prefix for selectors and component names in this library. Never `gd-*` (reserved for gravitee-dashboard) or other prefixes for GMD-specific UI.
