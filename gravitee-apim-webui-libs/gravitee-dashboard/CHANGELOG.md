# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2026-04-24

### Breaking

- **Filter chip CSS contract**: removed the `--gio-observability-filter-chip-*` bridge from `@gravitee/gravitee-dashboard`. Integrators must use `--gd-filter-chip-*` (and new `--gd-filter-chip-add-*` on the dynamic filter bar) on any ancestor of `gd-dynamic-filter-bar` / `gd-filter-chip`.
- **Default chip colors**: when `--gd-filter-chip-background` / `--gd-filter-chip-color` are unset, the library now falls back to Storybook hex values (`#fff3eb` / `#f15115`) before Material `--mat-sys-*`, so host apps with a neutral M3 chip palette no longer override the gravitee-dashboard look by accident.

### Added

- **`--gd-filter-chip-add-background`** and **`--gd-filter-chip-add-background-hover`**: style the “Add filter” chip consistently with filter chips (defaults: transparent / same peach hover as before).
- **Add / Edit filter dialog**: optional **Limit values** checkbox (tooltip explains time-range scoping) under the KEYWORD filter value when the host passes `timeFrom` / `timeTo`; when unchecked, value search calls omit those bounds (same `FILTER_VALUES_PROVIDER.getValues` contract).

### Fixed

- **Chip token specificity**: `mat-chip.mat-mdc-chip.mat-mdc-standard-chip` selectors so host apps using global `mat-chip { mat.chips-overrides(...) }` (e.g. Gravitee Console) no longer override gravitee-dashboard chip surfaces.
