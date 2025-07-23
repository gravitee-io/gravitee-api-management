# Gravitee Markdown Library

A comprehensive markdown library for Angular applications with support for dynamic component rendering.

## Features

- **Markdown Viewer**: Render markdown content with syntax highlighting
- **Markdown Editor**: Edit markdown content with Monaco editor integration
- **Dynamic Components**: Embed custom Angular components in markdown content
- **Dark Theme Support**: Toggle between light and dark themes
- **Custom Components**: Register your own components for dynamic rendering

## Installation

```bash
npm install @gravitee/gravitee-markdown
```

## Basic Usage

### Markdown Viewer

```typescript
import { GraviteeMarkdownViewerComponent } from '@gravitee/gravitee-markdown';

@Component({
  selector: 'app-example',
  template: `
    <gravitee-markdown-viewer
      [content]="markdownContent"
      [darkTheme]="false"
      [baseUrl]="'/api'"
      [pageBaseUrl]="'/pages'"
    ></gravitee-markdown-viewer>
  `,
  imports: [GraviteeMarkdownViewerComponent],
  standalone: true
})
export class ExampleComponent {
  markdownContent = `
# Hello World

This is some **markdown** content with <app-copy-code text="console.log('Hello World')"></app-copy-code>.
  `;
}
```

## Dynamic Components

The library supports rendering custom Angular components within markdown content. You can register components that will be dynamically rendered when referenced in the markdown.

### Registering Components

```typescript
import { GraviteeMarkdownViewerRegistryService, CopyCodeComponent } from '@gravitee/gravitee-markdown';

@Component({
  // ... your component
})
export class AppComponent {
  constructor(private registryService: GraviteeMarkdownViewerRegistryService) {
    // Register the copy-code component
    this.registryService.registerComponent({
      selector: 'app-copy-code',
      component: CopyCodeComponent,
      inputs: {
        text: 'default-text'
      }
    });
  }
}
```

### Using Dynamic Components in Markdown

Once registered, you can use your components in markdown content:

```markdown
# Example

Here's some code you can copy:

<app-copy-code text="console.log('Hello from dynamic component!')"></app-copy-code>

This will render the CopyCodeComponent with the specified text.
```

### Creating Custom Components

You can create your own components for dynamic rendering:

```typescript
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-custom-component',
  standalone: true,
  template: `
    <div class="custom-component">
      <h3>{{ title() }}</h3>
      <p>{{ content() }}</p>
    </div>
  `
})
export class CustomComponent {
  title = input<string>('');
  content = input<string>('');
}
```

Then register it:

```typescript
this.registryService.registerComponent({
  selector: 'app-custom-component',
  component: CustomComponent
});
```

And use it in markdown:

```markdown
<app-custom-component title="My Title" content="My content"></app-custom-component>
```

## API Reference

### GraviteeMarkdownViewerComponent

#### Inputs

- `content: string` - The markdown content to render
- `darkTheme: boolean` - Enable dark theme (default: false)
- `highlightTheme: string` - Syntax highlighting theme (default: 'github')
- `baseUrl: string` - Base URL for media links
- `pageBaseUrl: string` - Base URL for page links

### GraviteeMarkdownViewerRegistryService

#### Methods

- `registerComponent(config: DynamicComponentConfig): void` - Register a component for dynamic rendering
- `registerComponents(configs: DynamicComponentConfig[]): void` - Register multiple components
- `getComponent(selector: string): DynamicComponentConfig | undefined` - Get a registered component
- `getAllComponents(): DynamicComponentConfig[]` - Get all registered components
- `hasComponent(selector: string): boolean` - Check if a component is registered
- `clear(): void` - Clear all registered components

### DynamicComponentConfig

```typescript
interface DynamicComponentConfig {
  selector: string;           // The HTML tag name to match
  component: Type<any>;       // The Angular component class
  inputs?: Record<string, any>; // Default input values
}
```

## Examples

### Complete Setup Example

```typescript
import { Component, OnInit } from '@angular/core';
import { 
  GraviteeMarkdownViewerComponent,
  GraviteeMarkdownViewerRegistryService,
  CopyCodeComponent
} from '@gravitee/gravitee-markdown';

@Component({
  selector: 'app-documentation',
  template: `
    <gravitee-markdown-viewer
      [content]="documentationContent"
      [darkTheme]="isDarkTheme"
    ></gravitee-markdown-viewer>
  `,
  imports: [GraviteeMarkdownViewerComponent],
  standalone: true
})
export class DocumentationComponent implements OnInit {
  documentationContent = `
# API Documentation

Here's how to make a request:

<app-copy-code text="curl -X GET https://api.example.com/v1/users"></app-copy-code>

## Response Format

The API returns JSON responses.
  `;

  isDarkTheme = false;

  constructor(private registryService: GraviteeMarkdownViewerRegistryService) {}

  ngOnInit() {
    // Register the copy-code component
    this.registryService.registerComponent({
      selector: 'app-copy-code',
      component: CopyCodeComponent
    });
  }
}
```

## License

Apache License 2.0
