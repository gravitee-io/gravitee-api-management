# Card Component

A flexible card component for displaying structured content with optional title, subtitle, and markdown content. 
The component uses a token-based theming system that allows for easy customization through CSS custom properties and input properties.

## Features

The card component provides:

- **Flexible content structure**: Support for title, subtitle, and markdown content
- **Customizable styling**: Token-based theming with CSS custom properties
- **Input-based overrides**: Direct styling through component inputs
- **Responsive design**: Adapts to different screen sizes
- **Markdown support**: Rich content rendering through `gmd-md` component

## Usage

### Basic Card

```html
<gmd-card>
  <gmd-md>
    This is a simple card with markdown content.
  </gmd-md>
</gmd-card>
```

### Card with Title

```html
<gmd-card>
  <gmd-card-title>Card Title</gmd-card-title>
  <gmd-md>
    This card has a title and content.
  </gmd-md>
</gmd-card>
```

### Card with Title and Subtitle

```html
<gmd-card>
  <gmd-card-title>Card Title</gmd-card-title>
  <gmd-card-subtitle>Card Subtitle</gmd-card-subtitle>
  <gmd-md>
    This card has both a title and subtitle.
  </gmd-md>
</gmd-card>
```

### Card with Custom Colors

```html
<gmd-card backgroundColor="#ffffff" textColor="#333333">
  <gmd-card-title>Custom Styled Card</gmd-card-title>
  <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
  <gmd-md>
    This card has custom background and text colors.
  </gmd-md>
</gmd-card>
```

## Component Structure

The card component consists of:

- **Container**: Main card wrapper with padding and border
- **Header**: Contains title and subtitle elements
- **Content**: Contains markdown content blocks

## Input Properties

The card component supports the following input properties for dynamic styling:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `backgroundColor` | `string \| null` | `null` | Override the card background color |
| `textColor` | `string \| null` | `null` | Override the card text color |

### Input Usage Examples

```html
<!-- Using string values -->
<gmd-card backgroundColor="#ff0000" textColor="#ffffff">
  <gmd-card-title>Red Card</gmd-card-title>
  <gmd-md>This card has a red background with white text.</gmd-md>
</gmd-card>

<!-- Using CSS color functions -->
<gmd-card backgroundColor="rgb(0, 102, 204)" textColor="white">
  <gmd-card-title>Blue Card</gmd-card-title>
  <gmd-md>This card uses RGB color values.</gmd-md>
</gmd-card>

<!-- Using CSS custom properties -->
<gmd-card backgroundColor="var(--my-custom-color)" textColor="var(--my-text-color)">
  <gmd-card-title>Themed Card</gmd-card-title>
  <gmd-md>This card uses CSS custom properties.</gmd-md>
</gmd-card>
```

## Theming Tokens

The card component uses CSS custom properties for theming. You can override any token by setting the corresponding CSS variable.

### Card Tokens

| Token                    | CSS Variable                      | Default Value | Description | Example |
|--------------------------|-----------------------------------|---------------|-------------|---------|
| **Container Color**      | `--gmd-card-container-color`      | `#f4f7fd` | Background color of card | `#ffffff` (white) |
| **Text Color**           | `--gmd-card-text-color`           | `#1d192b` | Text color for card content | `#333333` (dark gray) |

### Inline Style Overrides

```html
<gmd-card style="--gmd-card-outline-color: darkblue; --gmd-card-text-color: pink;">
  <gmd-card-title>Centered Card</gmd-card-title>
  <gmd-card-subtitle>With Custom Border</gmd-card-subtitle>
  <gmd-md>This card is centered with a dark blue border.</gmd-md>
</gmd-card>
```

## Advanced Usage

### Multiple Cards in a Grid

```html
<gmd-grid [columns]="3">
  <gmd-cell>
    <gmd-card backgroundColor="#ffffff">
      <gmd-card-title>First Card</gmd-card-title>
      <gmd-md>Content for the first card.</gmd-md>
    </gmd-card>
  </gmd-cell>
  <gmd-cell>
    <gmd-card backgroundColor="#f0f0f0">
      <gmd-card-title>Second Card</gmd-card-title>
      <gmd-md>Content for the second card.</gmd-md>
    </gmd-card>
  </gmd-cell>
  <gmd-cell>
    <gmd-card backgroundColor="#e0e0e0">
      <gmd-card-title>Third Card</gmd-card-title>
      <gmd-md>Content for the third card.</gmd-md>
    </gmd-card>
  </gmd-cell>
</gmd-grid>
```

## For Developers

### SCSS Override Example

You can also customize the card tokens using SCSS by importing and using the `overrides` mixin:

```scss
@use '@gravitee/gravitee-markdown' as gmd;

// Custom card theme
.my-custom-cards {
  @include gmd.card-overrides((
    container-color: #ffffff,
    outline-color: #e0e0e0,
    text-color: #333333,
    title-text-weight: 600,
    title-text-size: 18px,
    subtitle-text-size: 16px,
    container-shape: 8px,
    text-align: center,
  ));
}
```

This approach allows you to scope card customizations to specific components or sections of your application.
