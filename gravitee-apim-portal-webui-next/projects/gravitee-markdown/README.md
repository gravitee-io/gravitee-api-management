# @gravitee/gravitee-markdown

A library for Gravitee Markdown, an enriched markdown language. This library provides components for editing and viewing Gravitee Markdown content, along with various components that can be used within the editor.

## Overview

Gravitee Markdown is an enriched markdown language that extends standard markdown with additional features and components. This library provides:

- **Editor Component**: A text editor for creating and editing Gravitee Markdown content
- **Viewer Component**: A component for rendering and displaying Gravitee Markdown content
- **Editor Components**: Various specialized Angular components that can be embedded within the editor and will be rendered in the viewer for enhanced functionality

## Development

### Prerequisites

- Node.js (version specified in project requirements)
- Angular CLI 19.2.0 or later
- Yarn package manager

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

For a complete list of available schematics, run:

```bash
ng generate --help
```

### Building the Library

To build the library, run:

```bash
# Build the library
ng build gravitee-markdown

# Build with production configuration
ng build gravitee-markdown --configuration=production
```

This command will compile the project, and the build artifacts will be placed in the `dist/gravitee-markdown/` directory.

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

## Usage

After building the library, you can import and use the components in other Angular projects:

```typescript
import { GraviteeMarkdownComponent } from '@gravitee/gravitee-markdown';

@Component({
  imports: [GraviteeMarkdownComponent],
  // ...
})
export class MyComponent { }
```

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

For information about building Angular libraries, see the [Angular Library Guide](https://angular.dev/guide/libraries).
