# Coding Guidelines for gravitee-apim-portal-webui-next

Design system rules for the **main app**. Use together with the library-specific rule files when working in those libraries.

## SCSS theme usage

- **Always** use the app theme via: `@use '.../scss/theme' as app-theme;` (adjust the path to match your file location).
- Reference theme variables as: `app-theme.$variable-name`.
- Do not use bare theme variables or a different namespace; keep `app-theme` as the single entry point for theme tokens.
- Do not use hard-coded color values for any properties: color, background, etc. Always try to find existing variables in **variables.scss** that semantically match the needed use case even if the color value is not exactly the same as presented on the screenshots.
  - Exceptionally, you can suggest creating a new variable but only if it is for a reusable property not an ad hoc one.

## Layout

- Use mixins and variables from **layout.scss** for spacing, breakpoints, and grid.
- Do not introduce ad-hoc layout values; prefer the canonical layout mixins and variables.

## Accessibility

- **Icons:** Decorative icons must be hidden from screen readers (e.g. `aria-hidden="true"`); meaningful icons must have accessible labels or `aria-label`.
- **Form controls:** Associate labels with inputs (by id/label, or `aria-label`/`aria-labelledby` where appropriate). Announce errors and hints to screen readers.
- **Trees:** Use proper roles and aria attributes (e.g. `role="tree"`, `role="treeitem"`, `aria-expanded`, `aria-selected`) and keyboard navigation.
- **Screen-reader text:** Use a consistent pattern (e.g. `.sr-only` or equivalent) for text that is only for screen readers; do not rely on `display: none` for content that should be announced.
- **Buttons:** Use semantic `<button>` elements; if you must use a div/span, add `role="button"`, keyboard support, and `aria-label` where the label is not visible.
- **No toasts/snackbars:** Do not use `MatSnackBar`, toasts, or similar transient notifications for errors or confirmations. They are easily missed by screen readers. Instead, display inline error or success messages near the relevant action/form with proper `role="alert"` or `aria-live="polite"` attributes.

## Typography

- Use typography classes from **typography.scss**: `.next-gen-`\* (e.g. `.next-gen-h1`, `.next-gen-body`). Do not create ad-hoc classes that duplicate typography; use the typography as the single source for type styles.
- Do not use hard-coded values for typography-related properties: font-size, font-weight, line-height.

## Localization

- **All user-facing text** in portal components must be localized.
- **HTML:** Use the `i18n` attribute for text content; `i18n-aria-label` for `aria-label`:

```html
<mat-card-title i18n="@@logInTitle">Login</mat-card-title>
<button i18n-aria-label="@@themeSelectorAriaLabel" aria-label="Theme">...</button>
```

- **TypeScript:** Use `$localize` tagged template with `@@messageId`. Add placeholder metadata for interpolated strings:

```ts
// Simple string
{ path: 'subscriptions', title: $localize`:@@subscriptionsTitle:Subscriptions` }

// Interpolated
getGoToPageLabel(page: number): string {
  return $localize`:@@paginationGoToPage:Go to page ${page}:page:`;
}
```

## GMD theming from main app

- GMD components are themed from the main app via **gmd-overrides.scss**. Keep all GMD visual overrides (tokens, component overrides) in that file so the main app remains the single source of theming for GMD in the portal.
- **Custom CSS in GMD content** (e.g. homepage, subscription form): To target dark mode, use `:host(.dark-mode) .selector`. The gmd-viewer adds `.dark-mode` to its host when the `darkMode` input is true (Console preview) or when the Portal theme is dark.

## Testing

- Use **Component Harnesses** for interactions and queries in unit tests when a harness exists for the component.
- Use **aria-label** (or `data-testid` where necessary) for test targeting when no harness exists; prefer accessibility-oriented selectors over brittle DOM structure.

## Naming: gmd-_ vs gd-_ in main app

- In the **main app**, use the **gmd-\*** prefix for elements or components that are part of the Gravitee Markdown (GMD) surface (e.g. GMD wrapper or overrides).
- Use the **gd-\*** prefix only for elements or components that belong to the **gravitee-dashboard** library (charts, dashboard widgets). Do not use **gd-\*** for general app UI; reserve it for dashboard-related selectors and components.
