# @gravitee/gravitee-markdown

A library for Gravitee Markdown, an enriched markdown language. This library provides components for editing and viewing Gravitee Markdown content, along with various components that can be used within the editor.

## Overview

Gravitee Markdown is an enriched markdown language that extends standard markdown with additional features and components. This library provides:

- **Editor Component**: A text editor for creating and editing Gravitee Markdown content
- **Viewer Component**: A component for rendering and displaying Gravitee Markdown content
- **Editor Components**: Various specialized Angular components that can be embedded within the editor and will be rendered in the viewer for enhanced functionality

## Development

### Code Scaffolding

To generate new components, directives, pipes, or other Angular artifacts, use the Angular CLI with the library prefix:

```bash
# Generate a new component
ng generate component component-name --project=gravitee-markdown

# Generate a new directive
ng generate directive directive-name --project=gravitee-markdown

# Generate a new pipe
ng generate pipe pipe-name --project=gravitee-markdown

# Generate a new service
ng generate service service-name --project=gravitee-markdown
```

**Important**: Always specify `--project=gravitee-markdown` to ensure the component is generated in the correct library project.

### Building the Library

To build the library, run:

```bash
# Build the library
ng build gravitee-markdown

# Build with production configuration
ng build gravitee-markdown --configuration=production
```

This command will compile the project, and the build artifacts will be placed in the `dist/gravitee-markdown/` directory.

**Console Configuration**

To build the library for the Console application, run:

```bash
# Build with console configuration
ng build gravitee-markdown --configuration=console
```

## Testing

**Note**: Tests are currently executed by the parent project. The library itself does not have its own test runner configured at this time.

For testing the library components, run the tests from the parent project root:

```bash
# From the parent project root
yarn test
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
