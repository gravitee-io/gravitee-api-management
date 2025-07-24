# Card Component Examples (New Approach)

This file demonstrates the new card component approach using inner `<card-actions>` components instead of JSON strings.

## Basic Card

<app-card title="Welcome to Gravitee">
  This is a basic card with a title and content. The card provides a clean, organized way to present information with optional action buttons.
</app-card>

## Card with Actions

<app-card title="API Documentation">
  Access comprehensive API documentation with examples, tutorials, and best practices. Learn how to integrate with our platform effectively.
  
  <card-actions>
    <app-button href="/api/docs" text="View Docs" variant="filled">View Docs</app-button>
    <app-button href="/api/examples" text="Examples" variant="outlined">Examples</app-button>
  </card-actions>
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

<app-card title="External Resources">
  Links to external resources that will open in new tabs. Perfect for directing users to additional documentation or resources.
  
  <card-actions>
    <app-button href="https://github.com/gravitee-io" text="GitHub" type="external" variant="filled">GitHub</app-button>
    <app-button href="https://docs.gravitee.io" text="Documentation" type="external" variant="outlined">Documentation</app-button>
  </card-actions>
</app-card>

## Complete Custom Card

<app-card title="Premium Feature" elevation="3" backgroundColor="#ffffff" borderColor="#28a745" borderRadius="8px">
  A fully customized card with premium styling, custom action buttons, and elevated design. This demonstrates the full range of customization options available.
  
  <card-actions>
    <app-button href="/premium/signup" text="Get Started" variant="filled" backgroundColor="#28a745">Get Started</app-button>
    <app-button href="/premium/features" text="Learn More" variant="outlined">Learn More</app-button>
  </card-actions>
</app-card>

## Multiple Actions

<app-card title="Multiple Action Buttons">
  You can add as many action buttons as you need within the card-actions component.
  
  <card-actions>
    <app-button href="/primary" text="Primary Action" variant="filled">Primary Action</app-button>
    <app-button href="/secondary" text="Secondary Action" variant="outlined">Secondary Action</app-button>
    <app-button href="/tertiary" text="Tertiary Action" variant="text">Tertiary Action</app-button>
  </card-actions>
</app-card>

## Usage in Markdown

You can use cards anywhere in your markdown content:

```markdown
# Product Overview

<app-card title="Core Features">
  Discover the powerful features that make our platform stand out.
  
  <card-actions>
    <app-button href="/features" text="Explore">Explore</app-button>
  </card-actions>
</app-card>

## Getting Started

<app-card title="Quick Start" elevation="2">
  Follow our step-by-step guide to get up and running quickly.
  
  <card-actions>
    <app-button href="/tutorial" text="Tutorial" variant="filled">Tutorial</app-button>
    <app-button href="/examples" text="Examples" variant="outlined">Examples</app-button>
  </card-actions>
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

### Card Actions Component

The `<card-actions>` component is a simple container that:
- Provides consistent styling for action buttons
- Supports any number of action buttons
- Uses the same button styling as the standalone button component
- Automatically handles spacing and layout

## Monaco Editor Suggestions

When typing in the Monaco editor, you'll get these suggestions:

- `card` - Basic card with title and content
- `card-with-actions` - Card with action buttons using card-actions
- `card-elevated` - Card with custom elevation
- `card-custom-styled` - Card with custom styling
- `card-complete` - Complete card with all options
- `card-external-actions` - Card with external action links

## Advantages of This Approach

✅ **More intuitive**: No need to write JSON strings  
✅ **Better error handling**: No JSON parsing issues  
✅ **Easier to read**: Clear, readable markup  
✅ **Flexible**: Add as many buttons as needed  
✅ **Type-safe**: No risk of malformed JSON  
✅ **Better IDE support**: Proper syntax highlighting and autocomplete  

## Migration from JSON Approach

If you were using the old JSON approach:

**Old (JSON):**
```markdown
<app-card title="Card" actions="[{text: 'Action', href: '/link'}]">
  Content
</app-card>
```

**New (Inner Components):**
```markdown
<app-card title="Card">
  Content
  
  <card-actions>
    <app-button href="/link" text="Action">Action</app-button>
  </card-actions>
</app-card>
```

This new approach is much more user-friendly and eliminates all the JSON parsing issues! 