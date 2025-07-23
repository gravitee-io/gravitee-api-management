# Button Component Examples

This file demonstrates all the features of the button component with various examples.

## Basic Button

<app-button href="/docs/getting-started" text="Get Started">Get Started</app-button>

## Button Variants

### Filled Button (Default)
<app-button href="/api/docs" text="API Documentation" variant="filled">API Documentation</app-button>

### Outlined Button
<app-button href="/tutorials" text="View Tutorials" variant="outlined">View Tutorials</app-button>

### Text Button
<app-button href="/examples" text="See Examples" variant="text">See Examples</app-button>

## External Links

### External Filled Button
<app-button href="https://github.com/gravitee-io" text="GitHub" type="external" variant="filled">GitHub</app-button>

### External Outlined Button
<app-button href="https://docs.gravitee.io" text="Documentation" type="external" variant="outlined">Documentation</app-button>

## Custom Styled Buttons

### Custom Colors
<app-button href="/contact" text="Contact Us" variant="filled" backgroundColor="#e91e63" textColor="#ffffff">Contact Us</app-button>

### Custom Border Radius
<app-button href="/about" text="About Us" variant="outlined" borderRadius="20px">About Us</app-button>

### Custom Text Transform
<app-button href="/login" text="Sign In" variant="filled" textTransform="uppercase">Sign In</app-button>

### Fully Customized
<app-button href="/signup" text="Create Account" variant="filled" backgroundColor="#4caf50" textColor="#ffffff" borderRadius="8px" textTransform="uppercase">Create Account</app-button>

## Usage in Markdown

You can use buttons anywhere in your markdown content:

```markdown
# Welcome to Our Documentation

<app-button href="/quick-start" text="Quick Start Guide">Quick Start Guide</app-button>

## Next Steps

<app-button href="/advanced-topics" text="Advanced Topics" variant="outlined">Advanced Topics</app-button>

<app-button href="https://github.com/gravitee-io/gravitee-api-management" text="View on GitHub" type="external">View on GitHub</app-button>
```

## Available Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `href` | string | '' | The URL the button links to |
| `text` | string | '' | The button text |
| `type` | 'internal' \| 'external' | 'internal' | Whether to open in same tab or new tab |
| `variant` | 'filled' \| 'outlined' \| 'text' | 'filled' | The button style variant |
| `borderRadius` | string | '4px' | Custom border radius |
| `backgroundColor` | string | '' | Custom background color |
| `textColor` | string | '' | Custom text color |
| `textTransform` | string | 'none' | Text transform (uppercase, lowercase, etc.) |

## Monaco Editor Suggestions

When typing in the Monaco editor, you'll get these suggestions:

- `button` - Basic button with href and text
- `button-filled` - Filled button variant
- `button-outlined` - Outlined button variant  
- `button-text` - Text button variant
- `button-external` - External link button
- `button-custom-styled` - Button with custom styling options 