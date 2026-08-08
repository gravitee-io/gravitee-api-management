---
name: Console Web UI Conventions
layer: local
dirs: [gravitee-apim-console-webui]
description: Console-specific conventions - the Particles component hierarchy, shared components and guards, layout and styling bars, testing utilities, Storybook, and AngularJS preservation
---

# Console Web UI Conventions

## Particles design system

- Components: Particles (`gio-*`) first; Angular Material (`mat-*`) only when no Particles equivalent exists. Icons: `gio-icons` first, Material Icons as fallback.
- Hierarchy when adding UI: Particles (`gio-*`) → Console shared (`src/shared/components/`, `gio-*`) → Angular Material (`mat-*`) → from scratch (ask the requester: feature-scoped or `src/shared`?).
- Key shared components: `gio-permission`, `gio-license-banner`, `gio-table-wrapper`, `gio-api-select-dialog`, `gio-widget-layout`. Feature code imports from `shared/` rather than reimplementing.

## Styling

- Tokens only: no hex/RGB values and no arbitrary font-size px — use design tokens and CSS/SCSS variables. The shared Angular rule's "unless already local convention" escape is closed here.
- 4px grid: all margins, padding, and gaps are multiples of 4.
- Material overrides via SCSS mixins (e.g. `@include mat.button-overrides`), not ad-hoc CSS variables.
- Card surfaces stay distinct from the app background, with spacing between card elements on the 4px grid.
- Layout single source of truth: `src/scss/gio-layout.scss` — `@use '<path>/scss/gio-layout' as gio-layout;`, breakpoint mixins `small-desktop`, `desktop`, `large-desktop`. Use Flexbox/Grid for layout.

## Layout and UI patterns

- Layout uses containers with centered content — not full-width — where a centered layout is intended.
- Each screen has a single primary CTA; button spacing and placement stay consistent.
- Table headers align with cell content and use current header styles; tables use appropriate padding and action-column sizing.
- Page titles sit outside cards at the top. Links use themed colors.

## Accessibility (WCAG 2.1 AA)

- Color contrast meets AA for text and interactive elements.
- Focus outlines remain visible, or a replacement is provided.
- Respect `prefers-reduced-motion`.

## Shared guards and services

- Guards from `shared/`: `PermissionGuard`, `HasLicenseGuard`, `HasUnsavedChangesGuard`.
- Permissions: `GioPermissionService`, `ApimFeature`, `GioLicenseBannerModule`.
- Utilities: prefer `shared/utils`, `shared/pipes`, `shared/validators`.

## Testing

- Use `GioTestingModule` and `CONSTANTS_TESTING` from `shared/testing` in specs — they are provider-based (`provideHttpClient()` + `provideHttpClientTesting()`), so they satisfy the shared Angular rule's `rxResource` testing exception.
- Add methods to the component's harness (`.harness.ts`) and use them in specs; reach for `document.querySelector` only when no harness path exists.
- Before creating a custom harness class, search the codebase for existing patterns. Prefer built-in harnesses (`SpanHarness`, `DivHarness`, `MatButtonHarness`, ...) from `@gravitee/ui-particles-angular/testing` or Angular Material with `locatorFor` / `locatorForOptional` / `locatorForAll` and a selector, e.g. `locatorForOptional(SpanHarness.with({ selector: '[data-testid="..."]' }))`.

## Storybook

- Shared components have `.stories.ts` files (e.g. under "Shared / ...").

## Legacy AngularJS

- Preserve existing AngularJS patterns. Do not mix Angular and AngularJS in the same component.
