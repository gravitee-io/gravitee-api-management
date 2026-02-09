# Button Component

A customizable button component with three appearance styles: filled, outlined, and text. The component supports both internal and external links.

## Appearances

The button component supports three different appearance styles:

### Filled Button

- **Use case**: Primary actions, main CTAs
- **Style**: Solid background with white text
- **Default color**: Blue (#275CF6)

### Outlined Button

- **Use case**: Secondary actions, alternative options
- **Style**: Transparent background with colored border and text
- **Default color**: Blue (#1976d2) with light gray border (#CDD7E1)

### Text Button

- **Use case**: Tertiary actions, subtle interactions
- **Style**: Transparent background with colored text only
- **Default color**: Blue (#1976d2)

## Usage

### Basic Usage

```html
<!-- Filled button -->
<gmd-button appearance="filled">Save Changes</gmd-button>

<!-- Outlined button -->
<gmd-button appearance="outlined">Cancel</gmd-button>

<!-- Text button -->
<gmd-button appearance="text">Learn More</gmd-button>
```

### With Links

The button component supports both internal and external links through the `link` and `target` attributes.

#### Internal Links

Internal links start with "/" and open in the same tab by default:

```html
<!-- Internal navigation buttons -->
<gmd-button appearance="filled" link="/dashboard">Dashboard</gmd-button>
<gmd-button appearance="outlined" link="/settings">Settings</gmd-button>
<gmd-button appearance="text" link="/profile">Profile</gmd-button>
```

#### External Links

External links use full URLs and typically open in a new tab:

```html
<!-- External links -->
<gmd-button appearance="filled" link="https://gravitee.io" target="_blank">Visit Gravitee</gmd-button>
<gmd-button appearance="outlined" link="https://docs.gravitee.io" target="_blank">Documentation</gmd-button>
<gmd-button appearance="text" link="https://github.com/gravitee-io" target="_blank">GitHub</gmd-button>
```

## Properties

| Property     | Type                               | Default    | Description                                                                                         |
| ------------ | ---------------------------------- | ---------- | --------------------------------------------------------------------------------------------------- |
| `appearance` | `'filled' \| 'outlined' \| 'text'` | `'filled'` | The visual style of the button                                                                      |
| `link`       | `string \| null`                   | `null`     | The URL or path for the button link. Use "/path" for internal links or full URLs for external links |
| `target`     | `string`                           | `'_self'`  | Where to open the link: "\_self" for same tab, "\_blank" for new tab                                |

### Link Behavior

- **Internal Links**: Start with "/" (e.g., "/dashboard", "/settings")

  - Default target: `_self` (opens in same tab)
  - Used for navigation within the application

- **External Links**: Full URLs (e.g., "https://example.com")

  - Recommended target: `_blank` (opens in new tab)
  - Used for external websites and resources

- **No Link**: When `link` is not provided or is `null`
  - Button renders with default href="/"
  - Target defaults to `_self`

## For Developers

### Customization with SCSS Mixin

You can customize any aspect of the button styling using the `@gmd.button-overrides()` mixin:

```scss
@use '@gravitee/gravitee-markdown' as gmd;

// Custom button theme
.my-custom-buttons {
  @include gmd.button-overrides(
    (
      filled-container-color: #e91e63,
      filled-hover-container-color: #c2185b,
      filled-label-text-color: #ffffff,
      outlined-outline-color: #e91e63,
      outlined-label-text-color: #e91e63,
      text-label-text-color: #e91e63,
    )
  );
}
```

This approach allows you to scope button customizations to specific components or sections of your application.
