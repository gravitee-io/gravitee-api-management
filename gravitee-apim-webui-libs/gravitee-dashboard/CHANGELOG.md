# Changelog

All notable changes to this project will be documented in this file.

## [0.2.1] - 2026-04-27

### Added

- **`[gdToolbarBelow]` content slot** on `gd-dashboard`: optional projection slot rendered between the title toolbar and the widget grid. Lets consumers compose page-level controls (datepicker, dynamic filter bar, banners, etc.) inside the dashboard frame so the natural visual order is "title → consumer controls → grid", without coupling the lib to any specific filtering or analytics concept.

### Removed

- **Deleted `GenericFilterBarComponent` source files** (`components/filter/generic-filter-bar/*`). The component had been removed from `public-api.ts` in `0.2.0` and is now physically gone from the bundle.
- **Removed orphaned `.filterBar` class** in `gravitee-dashboard.component.scss` (left behind by the removal of the legacy filter bar).

## [0.2.0] - 2026-04-27

### Breaking

- **`gd-dashboard` is now input-driven**: removed internal `ActivatedRoute` / `Router` usage and `gd-generic-filter-bar`. The component no longer reads/writes query params. New required/optional inputs: `requestFilters` (`RequestFilter[]`), `timeRange` (`TimeRange`, required), `interval` (`number | undefined`), `refreshToken` (`number`). Removed inputs: `filters` (`Filter[]`), `defaultPeriod`.
- **Removed `GenericFilterBarComponent`** and related exports (`Filter`, `SelectedFilter`) from `public-api.ts`. Use `DynamicFilterBarComponent` and `TimeframeSelectorComponent` separately.

### Added

- **`TimeframeSelectorComponent`** exported from `public-api.ts` (standalone, was previously internal only).
- **`filter-url.codec`** (`encodeFilters`, `decodeFilters`): pure utility for Kong-style `q` + `v=1` URL encoding of `FilterCondition[]`.
- **`RequestFilter`, `TimeRange`** types re-exported from `public-api.ts`.

## [0.1.0] - 2026-04-24

### Breaking

- **Filter chip CSS contract**: removed the `--gio-observability-filter-chip-*` bridge from `@gravitee/gravitee-dashboard`. Integrators must use `--gd-filter-chip-*` (and new `--gd-filter-chip-add-*` on the dynamic filter bar) on any ancestor of `gd-dynamic-filter-bar` / `gd-filter-chip`.
- **Default chip colors**: when `--gd-filter-chip-background` / `--gd-filter-chip-color` are unset, the library now falls back to Storybook hex values (`#fff3eb` / `#f15115`) before Material `--mat-sys-*`, so host apps with a neutral M3 chip palette no longer override the gravitee-dashboard look by accident.

### Added

- **`--gd-filter-chip-add-background`** and **`--gd-filter-chip-add-background-hover`**: style the “Add filter” chip consistently with filter chips (defaults: transparent / same peach hover as before).
- **Add / Edit filter dialog**: optional **Limit values** checkbox (tooltip explains time-range scoping) under the KEYWORD filter value when the host passes `timeFrom` / `timeTo`; when unchecked, value search calls omit those bounds (same `FILTER_VALUES_PROVIDER.getValues` contract).

### Fixed

- **Chip token specificity**: `mat-chip.mat-mdc-chip.mat-mdc-standard-chip` selectors so host apps using global `mat-chip { mat.chips-overrides(...) }` (e.g. Gravitee Console) no longer override gravitee-dashboard chip surfaces.
