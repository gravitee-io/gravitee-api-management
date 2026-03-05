# Dark Mode -- Story Breakdown

## Story 1: Backend -- Extend theme model for dark mode

Add a `DarkMode` nested type to `ThemeDefinition` (color + customCss), add `Key.java` entries with dark defaults, and update all serialization/mapping layers so the portal API response and management API accept the new `dark` field. The change is backward-compatible: `dark` is `null` for existing themes.

### Tasks

1. Add `DarkMode` inner class (with `Color` + `customCss`) to `ThemeDefinition.java`.
2. Add `PORTAL_NEXT_THEME_DARK_COLOR_*` and `PORTAL_NEXT_THEME_DARK_CUSTOM_CSS` entries to `Key.java` with defaults:
   - Primary `#8BABF8`, Secondary `#6A95D4`, Tertiary `#8BABF8`, Error `#F2B8B5`
   - Page background `#1C1B1F`, Card background `#2B2930`
3. Update `DefaultThemeDomainService.getPortalNextDefaultTheme()` to populate `dark` from the new keys.
4. Update `ThemeAdapter` serialization/deserialization to handle `dark`.
5. Update portal REST `ThemeMapper` so the portal API response includes `dark`.
6. Update management v2 `ThemeMapper` to map `dark` on read and write.
7. Update OpenAPI specs (`openapi-ui.yaml`, `portal-openapi.yaml`) with the `DarkMode` schema and the new `dark` field on `PortalNextDefinition`.
8. Update existing tests.

### Files

| Action | File |
|--------|------|
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/theme/portalnext/ThemeDefinition.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/parameters/Key.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/theme/domain_service/DefaultThemeDomainService.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/adapter/ThemeAdapter.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/mapper/ThemeMapper.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/mapper/ThemeMapper.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-ui.yaml` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/theme/domain_service/DefaultThemeDomainServiceTest.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/theme/use_case/GetCurrentThemeUseCaseTest.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/test/java/io/gravitee/rest/api/portal/rest/resource/ThemeResourceTest.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/ui/ThemeResourceTest.java` |
| Modify | `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/ui/ThemesResourceTest.java` |

---

## Story 2: Console -- Dark theme customization UI

Add a Light/Dark segmented toggle to the theme editor page. When "Dark" is selected the Colors and Advanced CSS sections show dark mode values. Logos and Font remain shared. Map dark values to/from the API on load and save.

### Tasks

1. Extend `PortalNextDefinition` TS type with `dark?: { color: PortalNextDefinitionColor; customCss?: string }`.
2. Add a `mat-button-toggle-group` (Light / Dark) to the template between the Font and Colors expansion panels.
3. Add dark color and dark customCss form controls to the form group.
4. Conditionally bind the Colors and Advanced CSS sections to light or dark controls based on the toggle.
5. Update `convertThemeToThemeVM` and `convertThemeVMToUpdateTheme` to map dark values.
6. Update the `restoreDefaultValues` flow to also restore dark defaults.
7. Update tests.

### Files

| Action | File |
|--------|------|
| Modify | `gravitee-apim-console-webui/src/entities/management-api-v2/portalCustomization.ts` |
| Modify | `gravitee-apim-console-webui/src/portal/theme/portal-theme.component.ts` |
| Modify | `gravitee-apim-console-webui/src/portal/theme/portal-theme.component.html` |
| Modify | `gravitee-apim-console-webui/src/portal/theme/portal-theme.component.scss` |
| Modify | `gravitee-apim-console-webui/src/portal/theme/portal-theme.component.spec.ts` |

---

## Story 3: Portal -- ThemeService dark mode support

Extend `ThemeService` to load and store both light and dark theme definitions, expose a reactive `darkMode` signal, swap CSS custom properties on toggle, and persist the user's preference in `localStorage` (falling back to `prefers-color-scheme`).

### Tasks

1. Extend the `Theme` interface with `dark?: { color?: Theme['definition']['color']; customCss?: string }`.
2. Add a `darkMode` writable signal (boolean) and a `toggleDarkMode()` method.
3. On `loadTheme()`, store both `definition` (light) and `definition.dark` (dark) in memory.
4. Extract an `applyTheme(mode: 'light' | 'dark')` method that:
   - Picks the right color set and calls existing `addHslToDocument` / `addPropertyToDocument` helpers.
   - Toggles `.dark-mode` class on `document.documentElement`.
   - Injects the right `customCss` style element (remove previous, add new).
5. On init, read `localStorage` key `gio-portal-dark-mode`; fall back to `window.matchMedia('(prefers-color-scheme: dark)')`.
6. On toggle, write preference to `localStorage`.
7. Update tests.

### Files

| Action | File |
|--------|------|
| Modify | `gravitee-apim-portal-webui-next/src/services/theme.service.ts` |
| Modify | `gravitee-apim-portal-webui-next/src/services/theme.service.spec.ts` |

---

## Story 4: Portal -- Material dark theme SCSS integration

Activate the already-generated `$dark-theme` behind an `html.dark-mode` selector, fix any hardcoded light-theme references, add dark-appropriate fallbacks for custom variables, and add a global CSS transition for smooth switching.

### Tasks

1. In `styles.scss`, add `html.dark-mode { @include mat.all-component-colors(theme.$dark-theme); }` and override `body` vars (e.g., `--mat-app-background-color`, `--mat-menu-container-color`).
2. In `material-design-overrides.scss`, add `.dark-mode .secondary-button` using `theme.$dark-theme`.
3. In `variables.scss`, verify that the hardcoded fallback colors (`#1d192b` for text, `#fff` for contrast, `#f7f8fd` for background) will be overridden at runtime by the CSS variables set by `ThemeService`. No SCSS changes are strictly needed here since `ThemeService.applyTheme()` will set the correct values, but review each fallback to confirm.
4. Add a global smooth-transition rule (e.g., on `html.transitioning *`) that applies `transition: background-color 0.3s ease, color 0.2s ease, border-color 0.3s ease` and is removed after the transition completes, to avoid interfering with other animations.
5. Verify the `$dark-theme` generated by `m3-theme.scss` works correctly end-to-end (it uses the same CSS var palette as `$light-theme` but with `theme-type: dark`).

### Files

| Action | File |
|--------|------|
| Modify | `gravitee-apim-portal-webui-next/src/styles.scss` |
| Modify | `gravitee-apim-portal-webui-next/src/scss/material-design-overrides.scss` |
| Review | `gravitee-apim-portal-webui-next/src/scss/theme/variables.scss` |
| Review | `gravitee-apim-portal-webui-next/src/scss/theme/m3-theme.scss` |
| Review | `gravitee-apim-portal-webui-next/src/scss/theme/theme.scss` |
| Review | `gravitee-apim-portal-webui-next/src/scss/m3-adapter.scss` |

---

## Story 5: Portal -- Toggle button in nav bar

Create a `ThemeToggleComponent` (icon button with sun/moon icon and rotation animation) and add it to both the desktop and mobile nav bars, next to the user avatar / sign-in button. It should be visible regardless of authentication state.

### Tasks

1. Create `ThemeToggleComponent` -- an `mat-icon-button` that injects `ThemeService`, reads `darkMode()`, and calls `toggleDarkMode()` on click.
2. Icon: `light_mode` when in dark mode (click to go light), `dark_mode` when in light mode (click to go dark).
3. Add a CSS rotation + opacity transition on the icon when the mode changes.
4. Add the component to `desktop-nav-bar.component.html` in the `.actions` div, before `app-user-avatar` / Sign in button.
5. Add the component to `mobile-nav-bar.component.html` in an appropriate location.
6. Update `nav-bar` tests.

### Files

| Action | File |
|--------|------|
| Create | `gravitee-apim-portal-webui-next/src/components/theme-toggle/theme-toggle.component.ts` |
| Create | `gravitee-apim-portal-webui-next/src/components/theme-toggle/theme-toggle.component.html` |
| Create | `gravitee-apim-portal-webui-next/src/components/theme-toggle/theme-toggle.component.scss` |
| Modify | `gravitee-apim-portal-webui-next/src/components/nav-bar/desktop-nav-bar/desktop-nav-bar.component.ts` |
| Modify | `gravitee-apim-portal-webui-next/src/components/nav-bar/desktop-nav-bar/desktop-nav-bar.component.html` |
| Modify | `gravitee-apim-portal-webui-next/src/components/nav-bar/mobile-nav-bar/mobile-nav-bar.component.ts` |
| Modify | `gravitee-apim-portal-webui-next/src/components/nav-bar/mobile-nav-bar/mobile-nav-bar.component.html` |
| Modify | `gravitee-apim-portal-webui-next/src/components/nav-bar/nav-bar.component.spec.ts` |
