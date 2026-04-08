# Coding Guidelines for gravitee-apim-console-webui

## Particles Design System

### Components & icons

- Particles (`gio-*`) first. Angular Material (`mat-*`) only when no Particles equivalent exists.
- `gio-icons` first. Material Icons only as fallback.

### Styling

- Tokens only: no hex/RGB or arbitrary font-size px. Use design tokens, CSS/SCSS variables.
- 4px grid: all margins, padding, gaps MUST be multiples of 4.
- Material overrides via SCSS mixins (e.g. `@include mat.button-overrides`), not ad-hoc CSS variables.
- Card surfaces MUST be distinct from app background.
- Use Flexbox/Grid for layout.

### Layout & UI patterns

- Layout MUST use containers with centered content (not full-width where a centered layout is intended).
- Cards MUST have proper spacing between elements (4px grid).
- Each screen MUST have a single primary CTA (clear hierarchy).
- Button spacing and placement MUST be consistent.
- Table headers MUST align with cell content and use current header styles.
- Tables MUST use appropriate padding and action column sizing.
- Page titles MUST be placed correctly outside cards at the top.
- Links MUST use themed colors.

### Accessibility (WCAG 2.1 AA)

- Color contrast MUST meet AA for text and interactive elements.
- Focus outlines MUST remain visible (or provide a replacement).
- Use native elements (e.g. `<button>`) over clickable divs/spans.
- Respect `prefers-reduced-motion`.

## Console Conventions

### Shared components

- Hierarchy: Particles (`gio-*`) > Console shared (`src/shared/components/`, `gio-*`) > Angular Material (`mat-*`) > from scratch (ask user: feature-scoped or `src/shared`?).
- Key shared components: `gio-permission`, `gio-license-banner`, `gio-table-wrapper`, `gio-api-select-dialog`, `gio-widget-layout`.
- Feature code MUST import from `shared/` rather than reimplementing.

### Layout

- Single source of truth: `src/scss/gio-layout.scss`.
- Usage: `@use '<path>/scss/gio-layout' as gio-layout;`
- Breakpoint mixins: `small-desktop`, `desktop`, `large-desktop`.

### Shared guards & services

- Guards: `PermissionGuard`, `HasLicenseGuard`, `HasUnsavedChangesGuard` from `shared/`.
- Permissions: `GioPermissionService`, `ApimFeature`, `GioLicenseBannerModule`.
- Utilities: prefer `shared/utils`, `shared/pipes`, `shared/validators`.

### Testing

- Use `GioTestingModule` and `CONSTANTS_TESTING` from `shared/testing` in specs.
- Prefer harness methods over `document.querySelector` / `querySelectorAll`. Add appropriate methods to the component harness (`.harness.ts`) and use them in specs. Use `querySelector` only when there is no other choice.
- Before creating a custom harness class, search the codebase for existing patterns. Prefer built-in harnesses (`SpanHarness`, `DivHarness`, `MatButtonHarness`, etc.) from `@gravitee/ui-particles-angular/testing` or Angular Material with `locatorFor` / `locatorForOptional` / `locatorForAll` and a selector. Example: `locatorForOptional(SpanHarness.with({ selector: '[data-testid="..."]' }))`.

### Storybook

- Shared components MUST have `.stories.ts` files (e.g. under "Shared / ...").

### Legacy AngularJS

- Preserve existing AngularJS patterns. Do not mix Angular and AngularJS in the same component.
