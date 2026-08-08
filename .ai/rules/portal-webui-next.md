---
name: Portal Web UI Conventions
layer: local
dirs: [gravitee-apim-portal-webui-next]
description: Portal-specific conventions - app-theme SCSS discipline, typography classes, accessibility bars including no-toasts, i18n patterns, GMD theming, and the init(params) test helper
---

# Portal Web UI Conventions

Design-system rules for the main portal app; the `gravitee-apim-webui-libs` libraries carry their own rules.

## SCSS theme

- Always use the app theme via `@use '.../scss/theme' as app-theme;` (path adjusted to the file location) and reference variables as `app-theme.$variable-name`. Keep `app-theme` the single entry point for theme tokens.
- No hard-coded color values for any property — the shared Angular rule's "unless already local convention" escape is closed here. Find an existing variable in `variables.scss` that semantically matches the use case, even when its value is not exactly what a screenshot shows. Suggest a new variable only for a reusable property, never an ad-hoc one.
- Layout: use the mixins and variables from `layout.scss` for spacing, breakpoints, and grid — no ad-hoc layout values.

## Typography

- Use the typography classes from `typography.scss` (`.next-gen-h1`, `.next-gen-body`, ...) as the single source for type styles. No ad-hoc classes duplicating typography and no hard-coded font-size, font-weight, or line-height.

## Accessibility

- Decorative icons are hidden from screen readers (`aria-hidden="true"`); meaningful icons carry accessible labels or `aria-label`.
- Associate labels with inputs (id/label, or `aria-label` / `aria-labelledby`). Announce errors and hints to screen readers.
- Trees use proper roles and aria attributes (`role="tree"`, `role="treeitem"`, `aria-expanded`, `aria-selected`) and keyboard navigation.
- Screen-reader-only text uses a consistent pattern (`.sr-only` or equivalent); never `display: none` for content that should be announced.
- **No toasts or snackbars** (`MatSnackBar` or similar transient notifications) for errors or confirmations — they are easily missed by screen readers. Display inline error or success messages near the relevant action or form with `role="alert"` or `aria-live="polite"`.

## Localization

All user-facing text in portal components is localized. HTML uses the `i18n` attribute for text content and `i18n-aria-label` for `aria-label`; TypeScript uses `$localize` with `@@messageId`, adding placeholder metadata for interpolated strings:

```html
<mat-card-title i18n="@@logInTitle">Login</mat-card-title>
<button i18n-aria-label="@@themeSelectorAriaLabel" aria-label="Theme">...</button>
```

```ts
{ path: 'subscriptions', title: $localize`:@@subscriptionsTitle:Subscriptions` }

getGoToPageLabel(page: number): string {
  return $localize`:@@paginationGoToPage:Go to page ${page}:page:`;
}
```

## GMD theming from the main app

- GMD components are themed from the main app via `gmd-overrides.scss` — keep all GMD visual overrides (tokens, component overrides) in that file so the main app remains the single source of GMD theming in the portal.
- Custom CSS in GMD content (e.g. homepage, subscription form) targets dark mode with `:host(.dark-mode) .selector` — the gmd-viewer adds `.dark-mode` to its host when the `darkMode` input is true (Console preview) or the Portal theme is dark.

## Testing

- Portal's `AppTestingModule` wraps `HttpClientTestingModule`, so the shared Angular rule's `rxResource` testing exception bans it in `rxResource` specs — do not copy it from a neighbouring spec; use the provider-style setup in `.ai/guides/angular-async-patterns.md` (a repository-root path).
- One `init(params)` helper when a spec needs 2+ different TestBed configurations (providers, flags, stubs): give it default params so `await init()` works with no arguments, and call `await init({ ... })` only to override. Do not duplicate `configureTestingModule` blocks or add nested `describe`s just to change config — nested `describe`s remain fine for logical grouping. Canonical example: `src/app/log-in/log-in.component.spec.ts`.

## Naming: `gmd-*` vs `gd-*` in the main app

- Use the `gmd-*` prefix for elements or components on the Gravitee Markdown (GMD) surface (wrappers, overrides).
- Use the `gd-*` prefix only for elements or components belonging to the gravitee-dashboard library (charts, dashboard widgets) — never for general app UI.
