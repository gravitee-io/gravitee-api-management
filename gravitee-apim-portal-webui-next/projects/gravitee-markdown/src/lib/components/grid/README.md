# Grid Component

A responsive grid component that provides flexible layout capabilities for organizing content in columns.

## Usage

```html
<gmd-grid columns="3">
  <gmd-cell>Content 1</gmd-cell>
  <gmd-cell>Content 2</gmd-cell>
  <gmd-cell>Content 3</gmd-cell>
</gmd-grid>
```

## Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `columns` | `number` | `1` | Number of columns (1-6) |

## Responsive Behavior

The grid automatically adjusts to different screen sizes:

- **Large screens (>1200px)**: Uses the specified number of columns
- **Medium screens (768px-1200px)**: Reduces columns for better mobile experience
- **Small screens (<768px)**: Stacks all content in a single column

## Examples

### Basic Grid
```html
<gmd-grid columns="2">
  <gmd-cell>Left content</gmd-cell>
  <gmd-cell>Right content</gmd-cell>
</gmd-grid>
```

### Three Column Layout
```html
<gmd-grid columns="3">
  <gmd-cell>Column 1</gmd-cell>
  <gmd-cell>Column 2</gmd-cell>
  <gmd-cell>Column 3</gmd-cell>
</gmd-grid>
```

## Styling

The grid component uses CSS Grid for layout and includes responsive breakpoints. You can customize the appearance by targeting the `.grid-container` class and its responsive variants.

## For Developers

### SCSS Override Example

You can customize the grid spacing using SCSS by importing and using the `overrides` mixin:

```scss
@use '@gravitee/gravitee-markdown' as gmd;

// Custom grid theme
.my-custom-grid {
  @include gmd.grid-overrides((
    spacing: 24px,
  ));
}
```

This approach allows you to scope grid customizations to specific components or sections of your application.
