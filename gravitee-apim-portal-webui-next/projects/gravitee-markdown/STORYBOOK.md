# Storybook for Gravitee Markdown Library

This document describes how to use Storybook with the Gravitee Markdown library components.

## Overview

Storybook provides an interactive development environment for the markdown components, allowing you to:

- View components in isolation
- Test different props and states
- Document component usage
- Develop components interactively

## Available Stories

### Gravitee Markdown Viewer

The viewer component stories demonstrate:

- **Default**: Basic markdown rendering
- **WithCodeBlock**: Syntax highlighting for code blocks
- **WithTable**: Table rendering capabilities
- **DarkTheme**: Dark theme appearance
- **LongContent**: Handling of large content
- **EmptyContent**: Empty state handling

### Gravitee Markdown Editor

The editor component stories demonstrate:

- **Default**: Basic editor functionality
- **WithInitialContent**: Editor with pre-filled content
- **DarkTheme**: Dark theme editor
- **CustomPlaceholder**: Custom placeholder text
- **EmptyEditor**: Empty editor state
- **WithComplexContent**: Complex markdown features

## Running Storybook

### For Viewer Component

```bash
# Start Storybook for the viewer component
yarn storybook:viewer

# Build static Storybook for the viewer component
yarn storybook:viewer:build
```

### For Editor Component

```bash
# Start Storybook for the editor component
yarn storybook:editor

# Build static Storybook for the editor component
yarn storybook:editor:build
```

### For Main Application

```bash
# Start Storybook for the main application
yarn storybook

# Build static Storybook for the main application
yarn storybook:build
```

## Story Configuration

### Viewer Stories

Each viewer story includes:

- `content`: The markdown content to render
- `darkTheme`: Boolean to toggle dark/light theme
- `highlightTheme`: Syntax highlighting theme selection

### Editor Stories

Each editor story includes:

- `darkTheme`: Boolean to toggle dark/light theme
- `placeholder`: Custom placeholder text
- `content`: Initial content for the editor
- `contentChange`: Event handler for content changes

## Interactive Controls

Storybook provides interactive controls for:

- **Text inputs**: For content and placeholder text
- **Boolean toggles**: For theme settings
- **Select dropdowns**: For syntax highlighting themes
- **Action logging**: For event handlers

## Development Workflow

1. **Start Storybook**: Run `yarn storybook:viewer` or `yarn storybook:editor`
2. **Navigate to stories**: Browse the component stories in the sidebar
3. **Modify props**: Use the controls panel to adjust component properties
4. **View changes**: See real-time updates in the preview area
5. **Test interactions**: Use the actions panel to monitor events

## Adding New Stories

To add a new story:

1. Create a new story file: `component-name.stories.ts`
2. Import the component and Storybook types
3. Define the meta configuration
4. Export story objects with different args

Example:

```typescript
import type { Meta, StoryObj } from '@storybook/angular';
import { MyComponent } from './my.component';

const meta: Meta<MyComponent> = {
  title: 'Category/MyComponent',
  component: MyComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    // Define your props here
  }
};

export default meta;
type Story = StoryObj<MyComponent>;

export const Default: Story = {
  args: {
    // Your default props
  }
};
```

## Storybook Configuration

The Storybook configuration is located in `.storybook/main.ts` and includes:

- Story file patterns for both libraries
- Addons configuration
- Framework settings
- Documentation settings

## Ports

- **Main Storybook**: Port 6006
- **Viewer Storybook**: Port 6006
- **Editor Storybook**: Port 6007

## Build Output

Static Storybook builds are generated in:

- **Main**: `storybook-static/`
- **Viewer**: `storybook-static-viewer/`
- **Editor**: `storybook-static-editor/`

## Troubleshooting

### Common Issues

1. **Port conflicts**: If a port is already in use, Storybook will automatically try the next available port
2. **Build errors**: Ensure all dependencies are installed with `yarn install`
3. **Component not loading**: Check that the component is properly exported from its module

### Debugging

- Use the browser's developer tools to inspect the Storybook iframe
- Check the Storybook console for error messages
- Verify component imports and dependencies

## Best Practices

1. **Keep stories focused**: Each story should demonstrate a specific use case
2. **Use meaningful names**: Story names should clearly describe what they demonstrate
3. **Include documentation**: Use the `tags: ['autodocs']` to generate automatic documentation
4. **Test edge cases**: Include stories for empty states, error conditions, and boundary cases
5. **Maintain consistency**: Use consistent naming and structure across all stories 