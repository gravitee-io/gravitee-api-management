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

### Basic Usage

```html
<!-- Filled button -->
<gmd-button appearance="filled">Save Changes</gmd-button>

<!-- Outlined button -->
<gmd-button appearance="outlined">Cancel</gmd-button>

<!-- Text button -->
<gmd-button appearance="text">Learn More</gmd-button>
```


## For Developers

### Customization with SCSS Mixin

You can customize any aspect of the button styling using the `@gmd.button-overrides()` mixin:

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

