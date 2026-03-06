# Dark Mode -- Progress

## Stories

- ✅ **Story 1:** Backend -- Extend theme model for dark mode
- ✅ **Story 2:** Console -- Dark theme customization UI
- ✅ **Story 3:** Portal -- ThemeService dark mode support
- ✅ **Story 4:** Portal -- Material dark theme SCSS integration
- ✅ **Story 5:** Portal -- Toggle button in nav bar

---

## Story Notes

### Story 1: Backend

**Status:** Done

**What was done:**
- Added `DarkMode` inner class (`color` + `customCss`) to `ThemeDefinition.java`
- Added 7 `PORTAL_NEXT_THEME_DARK_*` keys to `Key.java` with sensible defaults
- Updated `DefaultThemeDomainService` to populate dark defaults from parameters
- Added `PortalNextDefinitionDarkMode` schema to management v2 OpenAPI spec
- Extended management v2 `ThemeMapper` with dark background field mappings
- Updated tests (`DefaultThemeDomainServiceTest`, `GetCurrentThemeUseCaseTest`, `ThemeResourceTest`)
- No changes needed to `ThemeAdapter` (Jackson handles new field) or portal `ThemeMapper` (definition is generic `type: object`)

**Data model decision -- nested `dark` field:**
Dark mode is nested inside `PortalNextDefinition` as `definition.dark.color` / `definition.dark.customCss`. This keeps both themes in a single API response and is backward-compatible (`dark` is null for existing themes). Font and logos are shared (live at the top level only).

**Future refactor -- flatter data model (breaking change):**
If we decide the nested `dark` object feels awkward, we can flatten to sibling fields (`darkColor`, `darkCustomCss`) on `PortalNextDefinition`. This would touch ~8-10 files, all mechanical:
1. `ThemeDefinition.java` -- remove `DarkMode` class, add `darkColor` (type `Color`) and `darkCustomCss` (type `String`) fields directly
2. `openapi-ui.yaml` -- remove `PortalNextDefinitionDarkMode` schema, add `darkColor` and `darkCustomCss` properties to `PortalNextDefinition`
3. Management v2 `ThemeMapper.java` -- update `@Mapping` paths (`definition.darkColor.pageBackground` instead of `definition.dark.color.pageBackground`)
4. `DefaultThemeDomainService.java` -- adjust builder calls
5. Console TS types + form bindings -- update property paths
6. Portal `ThemeService` -- update property access when swapping CSS vars
7. Tests -- update builders and assertions

Estimated effort: ~2 hours of mechanical refactoring. No architectural impact -- the nesting choice doesn't create deep coupling.

**Feedback:**

**Gotchas / Surprises:**

**Effective Prompts:**

---

### Story 2: Console

**Status:** Done

**What was done:**
- Added `PortalNextDefinitionDarkMode` TS interface and `dark` field to `PortalNextDefinition` in `portalCustomization.ts`
- Added Light/Dark segmented toggle (`mat-button-toggle-group` with sun/moon icons) between Font and Colors panels
- Added 7 dark form controls (`darkPrimaryColor`, `darkSecondaryColor`, etc.) to the reactive form
- Colors and Advanced CSS sections conditionally show light or dark controls based on `themeMode` signal
- `convertThemeToThemeVM` and `convertThemeVMToUpdateTheme` map dark fields to/from the API
- `restoreDefaultValues` restores both light and dark defaults
- Monaco editor refreshes on mode switch via `onThemeModeChange()`
- Updated test fixtures with dark mode data, updated submit expectation to include `dark`
- Converted `*ngIf` → `@if` in the color error messages per Angular rules

**Design approach:**
All dark form controls exist in the form at all times (not swapped). The toggle only controls visibility. On submit, both light and dark values are always sent. This avoids value-swapping complexity and makes dirty-checking work naturally.

**Feedback:**

**Gotchas / Surprises:**

**Effective Prompts:**

---

### Story 3: Portal -- ThemeService

**Status:** Done

**What was done:**
- Extracted `ThemeColorDefinition` interface for reuse between light and dark
- Added `dark` field to `Theme.definition` interface
- Added `darkMode` signal (boolean) and `toggleDarkMode()` method
- Stores both `lightDefinition` and `darkDefinition` in memory on `loadTheme()`
- `applyTheme(mode)` swaps all `--gio-app-*` CSS vars to the active color set and toggles `.dark-mode` class on `<html>`
- Custom CSS handled via a single `<style id="gio-theme-custom-css">` element that's replaced on each toggle
- Initial mode resolved from `localStorage` (`gio-portal-dark-mode`), falling back to `prefers-color-scheme: dark`, then light
- Dark mode falls back to light colors if dark definition is absent (backward compat)
- 4 tests: light default, localStorage restore, toggle + persist, CSS swap on toggle

**Feedback:**

**Gotchas / Surprises:**

**Effective Prompts:**

---

### Story 4: Portal -- SCSS Integration

**Status:** Done

**What was done:**
- Added `html.dark-mode { @include mat.all-component-colors(theme.$dark-theme); }` in `styles.scss` — activates Material dark tone mapping when `.dark-mode` class is present
- Added `.dark-mode .secondary-button` override in `material-design-overrides.scss` using `theme.$dark-theme`
- Added `html.transitioning *` transition rule in `styles.scss` for smooth background/color/border transitions (300ms)
- Updated `ThemeService.toggleDarkMode()` to add/remove `.transitioning` class with 350ms timeout so transitions only apply during mode switches (not on page load or other animations)
- Re-enabled `prefers-color-scheme: dark` detection in `resolveInitialMode()` now that the Material dark SCSS is wired
- Reviewed `variables.scss` — fallback values are light-specific (#1d192b, #fff, #f7f8fd) but they're only used when CSS vars aren't set. Since `ThemeService.applyTheme()` always sets the vars, no changes needed.
- Reviewed `m3-theme.scss` — `generate-m3-dark-theme()` already exists with `theme-type: dark` and uses the same CSS var palettes, so dark palette tones work automatically

**Feedback:**

**Gotchas / Surprises:**

**Effective Prompts:**

---

### Story 5: Portal -- Toggle Button

**Status:** Done

**What was done:**
- Created `ThemeToggleComponent` — standalone component with `mat-icon-button` injecting `ThemeService`
- Icon shows `dark_mode` (moon) in light mode, `light_mode` (sun) in dark mode
- CSS rotation (180deg) + opacity transition on icon when mode changes via `.dark` class
- Added to desktop nav bar in `.actions` div, before user avatar / sign-in button
- Added to mobile nav bar in `.actions` div, before the hamburger menu button
- Toggle is visible regardless of auth state (works for everyone)
- Added 3 new tests: toggle presence in desktop, toggle click calls `toggleDarkMode()`, toggle presence in mobile
- All 14 nav-bar tests pass

**Feedback:**

**Gotchas / Surprises:**

**Effective Prompts:**
