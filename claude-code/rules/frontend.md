# Frontend Design System (Gravitee)

## Core Principles

- **Framework**: Angular Material (Legacy), Material 3 (Standard).
- **Palette Generation**: Use HSL-based palette generation.

## Color Palette

Based on `gravitee-apim-portal-webui-next` theme:

- **Primary**: Defined via CSS variables (`--gio-app-primary-main-color`). Fallback often implies a deep violet/purple branding.
- **Secondary**: Supported for accents (`--gio-app-secondary-main-color`).
- **Background**: Light gray/white for light mode (`#f7f8fd`), dark for dark mode.
- **Surface**: Card backgrounds are distinct from app background.

## Topography

- **Font Family**: `var(--gio-app-font-family)` (Default to standard sans-serif: Inter, Roboto, or System UI).
- **Text Color**:
  - **Primary**: `#1d192b`
  - **Contrast**: `#fff`
  - **Muted**: `color-mix(in srgb, $default-text-color 75%, transparent)`

## Component Styling

- **Shapes**: Default container radius is `4px` (`var(--gio-app-card-container-shape)`).
- **Cards**: Elevated with shadow (`--gio-app-card-elevation`) and bordered (`#c9c4d0`).
- **Buttons**:
  - **Filled**: High contrast text on primary color.
  - **Outlined**: Border width `1px`, color `#79757f`.
  - **Text Transform**: Configurable, often Uppercase for avatars/buttons.

## Layout

- **Grid**: Standard Material grid or Flexbox/Grid CSS.
- **Spacing**: 8px baseline grid.
