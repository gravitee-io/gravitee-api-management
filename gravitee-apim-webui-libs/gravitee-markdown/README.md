# @gravitee/gravitee-markdown

A library for Gravitee Markdown, an enriched markdown language. This library provides components for editing and viewing Gravitee Markdown content, along with various components that can be used within the editor.

## Overview

Gravitee Markdown is an enriched markdown language that extends standard markdown with additional features and components. This library provides:

- **Editor Component**: A text editor for creating and editing Gravitee Markdown content
- **Viewer Component**: A component for rendering and displaying Gravitee Markdown content
- **Editor Components**: Various specialized Angular components that can be embedded within the editor and will be rendered in the viewer for enhanced functionality

## Development

**NOTE**: This library is now part of an **Nx workspace** at the repository root. All commands must be run from the repository root.

### Code Scaffolding

To generate new components, directives, pipes, or other Angular artifacts, use Nx:

```bash
# From repository root

# Generate a new component
nx generate component component-name --project=markdown

# Generate a new directive
nx generate directive directive-name --project=markdown

# Generate a new pipe
nx generate pipe pipe-name --project=markdown

# Generate a new service
nx generate service service-name --project=markdown
```

**Important**: Always specify `--project=markdown` to ensure the component is generated in the correct library project.

### Building the Library

To build the library from the repository root:

```bash
# Build the library
nx build markdown

# Build with production configuration
nx build markdown --configuration=production

# Build and watch for changes (useful during development)
nx build markdown --watch
```

This command will compile the project, and the build artifacts will be placed in the `dist/@gravitee/gravitee-markdown/` directory.

## Testing

**Note**: Tests are currently executed by the parent project. The library itself does not have its own test runner configured at this time.

For testing the library components, run the tests from the parent project root:

```bash
# From the parent project root
yarn test markdown
```

## Project Structure

```
src/
├── lib/                    # Library source code
│   ├── components/         # Reusable components
│   ├── directives/         # Custom directives
│   ├── pipes/             # Custom pipes
│   ├── services/          # Shared services
│   └── types/             # TypeScript type definitions
├── public-api.ts          # Public API exports
└── index.ts               # Library entry point
```

## Theming

The library supports theming through CSS variables. You can customize the appearance of the components by overriding the default CSS variables in your application's global styles.

### Editor Theming

To customize the editor's appearance, you can override components using the pertinent mixin.

For example, to change the outline color of the editor containers, you can add the following CSS to your global styles:

```css
@use '@gravitee/gravitee-markdown' as gmd;

.my-container {
  @include gmd.editor-overrides((container-outline-color: red));
}
```
