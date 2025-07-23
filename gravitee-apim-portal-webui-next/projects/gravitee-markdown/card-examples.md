# Card Component Examples

This file demonstrates all the features of the card component with various examples.

## Basic Card

<app-card title="Welcome to Gravitee">
  This is a basic card with a title and content. The card provides a clean, organized way to present information with optional action buttons.
</app-card>

## Card with Actions

<app-card title="API Documentation" actions="[{text: 'View Docs', href: '/api/docs', variant: 'filled'}, {text: 'Examples', href: '/api/examples', variant: 'outlined'}]">
  Access comprehensive API documentation with examples, tutorials, and best practices. Learn how to integrate with our platform effectively.
</app-card>

## Elevated Card

<app-card title="Featured Content" elevation="4">
  This card has increased elevation for more visual prominence. Perfect for highlighting important content or featured items.
</app-card>

## Custom Styled Card

<app-card title="Custom Design" backgroundColor="#f8f9fa" borderColor="#007bff" borderRadius="12px" borderWidth="2px">
  This card demonstrates custom styling with a light background, blue border, and rounded corners. You can customize all aspects of the card's appearance.
</app-card>

## Card with External Actions

<app-card title="External Resources" actions="[{text: 'GitHub', href: 'https://github.com/gravitee-io', type: 'external', variant: 'filled'}, {text: 'Documentation', href: 'https://docs.gravitee.io', type: 'external', variant: 'outlined'}]">
  Links to external resources that will open in new tabs. Perfect for directing users to additional documentation or resources.
</app-card>

## Complete Custom Card

<app-card title="Premium Feature" elevation="3" backgroundColor="#ffffff" borderColor="#28a745" borderRadius="8px" actions="[{text: 'Get Started', href: '/premium/signup', variant: 'filled', backgroundColor: '#28a745'}, {text: 'Learn More', href: '/premium/features', variant: 'outlined'}]">
  A fully customized card with premium styling, custom action buttons, and elevated design. This demonstrates the full range of customization options available.
</app-card>

## Usage in Markdown

You can use cards anywhere in your markdown content:

```markdown
# Product Overview

<app-card title="Core Features" actions="[{text: 'Explore', href: '/features'}]">
  Discover the powerful features that make our platform stand out.
</app-card>

## Getting Started

<app-card title="Quick Start" elevation="2" actions="[{text: 'Tutorial', href: '/tutorial', variant: 'filled'}, {text: 'Examples', href: '/examples', variant: 'outlined'}]">
  Follow our step-by-step guide to get up and running quickly.
</app-card>
```

## Available Properties

### Card Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `title` | string | '' | The card title displayed in the header |
| `borderRadius` | string | '8px' | Custom border radius |
| `backgroundColor` | string | '#ffffff' | Custom background color |
| `borderColor` | string | '#e0e0e0' | Custom border color |
| `borderWidth` | string | '1px' | Custom border width |
| `elevation` | 1 \| 2 \| 3 \| 4 \| 5 | 2 | Shadow elevation level |

### Action Properties

Each action in the `actions` array can have these properties:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `text` | string | - | The button text |
| `href` | string | - | The URL the button links to |
| `type` | 'internal' \| 'external' | 'internal' | Whether to open in same tab or new tab |
| `variant` | 'filled' \| 'outlined' \| 'text' | 'filled' | The button style variant |
| `borderRadius` | string | '4px' | Custom border radius for the button |
| `backgroundColor` | string | - | Custom background color for the button |
| `textColor` | string | - | Custom text color for the button |

## Monaco Editor Suggestions

When typing in the Monaco editor, you'll get these suggestions:

- `card` - Basic card with title and content
- `card-with-actions` - Card with action buttons
- `card-elevated` - Card with custom elevation
- `card-custom-styled` - Card with custom styling
- `card-complete` - Complete card with all options
- `card-external-actions` - Card with external action links

## Action Examples

### Basic Actions
```json
[
  {text: "Primary Action", href: "/primary"},
  {text: "Secondary Action", href: "/secondary", variant: "outlined"}
]
```

### External Actions
```json
[
  {text: "View on GitHub", href: "https://github.com", type: "external", variant: "filled"},
  {text: "Documentation", href: "https://docs.example.com", type: "external", variant: "outlined"}
]
```

### Custom Styled Actions
```json
[
  {text: "Get Started", href: "/signup", variant: "filled", backgroundColor: "#28a745", textColor: "#ffffff"},
  {text: "Learn More", href: "/learn", variant: "outlined", borderRadius: "20px"}
]
``` 