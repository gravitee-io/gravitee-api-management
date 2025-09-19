# Button Component

A customizable button component with three appearance styles: filled, outlined, and text. The component uses a token-based theming system that allows for easy customization through CSS custom properties.

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

```html
<!-- Filled button -->
<gmd-button appearance="filled">Save Changes</gmd-button>

<!-- Outlined button -->
<gmd-button appearance="outlined">Cancel</gmd-button>

<!-- Text button -->
<gmd-button appearance="text">Learn More</gmd-button>
```

## Theming Tokens

The button component uses CSS custom properties for theming. You can override any token by setting the corresponding CSS variable.

### Filled Button Tokens

| Token | CSS Variable | Default Value | Description | Example |
|-------|--------------|---------------|-------------|---------|
| **Label Text Font** | `--gmd-button-filled-label-text-font` | `'Arial'` | Font family for button text | `'Roboto', sans-serif` |
| **Label Text Weight** | `--gmd-button-filled-label-text-weight` | `400` | Font weight for button text | `600` (semi-bold) |
| **Label Text Size** | `--gmd-button-filled-label-text-size` | `14px` | Font size for button text | `16px` |
| **Label Text Tracking** | `--gmd-button-filled-label-text-tracking` | `0.1px` | Letter spacing for button text | `0.5px` |
| **Label Text Transform** | `--gmd-button-filled-label-text-transform` | `capitalize` | Text transformation | `uppercase`, `lowercase`, `none` |
| **Label Text Color** | `--gmd-button-filled-label-text-color` | `#ffffff` | Color of button text | `#000000` |
| **Container Color** | `--gmd-button-filled-container-color` | `#275CF6` | Background color of button | `#e91e63` (pink) |
| **Container Shape** | `--gmd-button-filled-container-shape` | `4px` | Border radius of button | `8px` (more rounded) |
| **Horizontal Padding** | `--gmd-button-filled-horizontal-padding` | `16px` | Left and right padding | `24px` (wider) |
| **Hover Container Color** | `--gmd-button-filled-hover-container-color` | `#0E42DB` | Background color on hover | `#c2185b` (darker pink) |
| **Active Container Color** | `--gmd-button-filled-active-container-color` | `#123391` | Background color when active/pressed | `#ad1457` (even darker pink) |
| **Active Outline Color** | `--gmd-button-filled-active-outline-color` | `#64B5F6` | Border color when active/pressed | `#f8bbd9` (light pink) |

### Outlined Button Tokens

| Token | CSS Variable | Default Value | Description | Example |
|-------|--------------|---------------|-------------|---------|
| **Label Text Font** | `--gmd-button-outlined-label-text-font` | `'Arial'` | Font family for button text | `'Roboto', sans-serif` |
| **Label Text Weight** | `--gmd-button-outlined-label-text-weight` | `400` | Font weight for button text | `600` (semi-bold) |
| **Label Text Size** | `--gmd-button-outlined-label-text-size` | `14px` | Font size for button text | `16px` |
| **Label Text Tracking** | `--gmd-button-outlined-label-text-tracking` | `0.1px` | Letter spacing for button text | `0.5px` |
| **Label Text Transform** | `--gmd-button-outlined-label-text-transform` | `capitalize` | Text transformation | `uppercase`, `lowercase`, `none` |
| **Label Text Color** | `--gmd-button-outlined-label-text-color` | `#1976d2` | Color of button text | `#e91e63` (pink) |
| **Outline Width** | `--gmd-button-outlined-outline-width` | `1px` | Width of button border | `2px` (thicker border) |
| **Outline Color** | `--gmd-button-outlined-outline-color` | `#CDD7E1` | Color of button border | `#e91e63` (pink border) |
| **Container Shape** | `--gmd-button-outlined-container-shape` | `4px` | Border radius of button | `8px` (more rounded) |
| **Horizontal Padding** | `--gmd-button-outlined-horizontal-padding` | `16px` | Left and right padding | `24px` (wider) |
| **Hover Container Color** | `--gmd-button-outlined-hover-container-color` | `#F4F7FD` | Background color on hover | `#fce4ec` (light pink) |
| **Active Container Color** | `--gmd-button-outlined-active-container-color` | `#d4eaff` | Background color when active/pressed | `#f8bbd9` (lighter pink) |

### Text Button Tokens

| Token | CSS Variable | Default Value | Description | Example |
|-------|--------------|---------------|-------------|---------|
| **Label Text Font** | `--gmd-button-text-label-text-font` | `'Arial'` | Font family for button text | `'Roboto', sans-serif` |
| **Label Text Weight** | `--gmd-button-text-label-text-weight` | `400` | Font weight for button text | `600` (semi-bold) |
| **Label Text Size** | `--gmd-button-text-label-text-size` | `14px` | Font size for button text | `16px` |
| **Label Text Tracking** | `--gmd-button-text-label-text-tracking` | `0.1px` | Letter spacing for button text | `0.5px` |
| **Label Text Transform** | `--gmd-button-text-label-text-transform` | `capitalize` | Text transformation | `uppercase`, `lowercase`, `none` |
| **Label Text Color** | `--gmd-button-text-label-text-color` | `#1976d2` | Color of button text | `#e91e63` (pink) |
| **Container Shape** | `--gmd-button-text-container-shape` | `4px` | Border radius of button | `8px` (more rounded) |
| **Horizontal Padding** | `--gmd-button-text-horizontal-padding` | `16px` | Left and right padding | `24px` (wider) |
| **Hover Container Color** | `--gmd-button-text-hover-container-color` | `#F4F7FD` | Background color on hover | `#fce4ec` (light pink) |
| **Active Container Color** | `--gmd-button-text-active-container-color` | `#d4eaff` | Background color when active/pressed | `#f8bbd9` (lighter pink) |

## SCSS Override Example

You can also customize the button tokens using SCSS by importing and using the `overrides` mixin:

```scss
@use '@gravitee/gravitee-markdown' as gmd;

// Custom button theme
.my-custom-buttons {
  @include gmd.button-overrides((
    filled-container-color: #e91e63,
    filled-hover-container-color: #c2185b,
    filled-label-text-color: #ffffff,
    outlined-outline-color: #e91e63,
    outlined-label-text-color: #e91e63,
    text-label-text-color: #e91e63,
  ));
}
```

This approach allows you to scope button customizations to specific components or sections of your application.

