---
name: Gravitee Dashboard Library Conventions
layer: local
dirs: [gravitee-apim-webui-libs/gravitee-dashboard]
description: gravitee-dashboard library conventions - the gd- selector prefix, composition, theme-agnosticism, and converter services
---

# Gravitee Dashboard Library Conventions

- **Selectors:** all component and host selectors in this library use the `gd-*` prefix (`gd-chart`, `gd-dashboard-tile`) — never `gmd-*` or other prefixes; `gd-*` is reserved for gravitee-dashboard.
- **Composition:** place subcomponents in the same folder as the parent or in subfolders (a chart component may have a `chart/` subfolder with converters and related types). Keep related logic and UI together; do not scatter dashboard pieces across unrelated directories.
- **Theme-agnostic:** no `@use` of a host app's SCSS theme and no app-theme variables — style through CSS custom properties (`--mat-sys-*` and the library's own `--gd-*` tokens) so the library works in different host apps.
- **Charts:** limit chart host styles to layout and sizing (e.g. `:host { display: block; width: 100%; }`). All chart data transformation, options building, and rendering logic belongs in converter services and shared utilities, not in the component class, template, or SCSS — keep components thin.
