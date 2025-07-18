# Gravitee Markdown Library

A comprehensive Angular library for markdown viewing and editing capabilities, designed specifically for the Gravitee API Management platform.

## Overview

This library provides two main components:
- **Gravitee Markdown Viewer**: A component for rendering markdown content with syntax highlighting
- **Gravitee Markdown Editor**: A component for editing markdown content with live preview

## Installation

```bash
npm install @gravitee/gravitee-markdown
```

## Quick Start

### Markdown Viewer

```typescript
import { GraviteeMarkdownViewerComponent } from '@gravitee/gravitee-markdown/viewer';

@Component({
  selector: 'app-example',
  template: `
    <gravitee-markdown-viewer 
      [content]="markdownContent"
      [darkTheme]="false">
    </gravitee-markdown-viewer>
  `,
  imports: [GraviteeMarkdownViewerComponent]
})
export class ExampleComponent {
  markdownContent = '# Hello World\n\nThis is **bold** text.';
}
```

### Markdown Editor

```typescript
import { GraviteeMarkdownEditorComponent } from '@gravitee/gravitee-markdown/editor';

@Component({
  selector: 'app-example',
  template: `
    <gravitee-markdown-editor 
      [(ngModel)]="content"
      [darkTheme]="false"
      (contentChange)="onContentChange($event)">
    </gravitee-markdown-editor>
  `,
  imports: [GraviteeMarkdownEditorComponent, FormsModule]
})
export class ExampleComponent {
  content = '# Hello World\n\nThis is **bold** text.';
  
  onContentChange(newContent: string): void {
    console.log('Content changed:', newContent);
  }
}
```

## Features

### Markdown Viewer
- **Syntax Highlighting**: Code blocks with language-specific highlighting using highlight.js
- **GitHub Flavored Markdown**: Support for tables, strikethrough, and more
- **Dark/Light Theme**: Toggle between light and dark themes
- **Responsive Design**: Adapts to different screen sizes
- **Accessibility**: WCAG compliant with proper ARIA attributes

### Markdown Editor
- **Live Preview**: Real-time preview as you type
- **Three Modes**: Edit, Preview, and Split view
- **Form Integration**: Implements ControlValueAccessor for seamless form integration
- **Material Design**: Built with Angular Material components
- **Customizable**: Configurable placeholder and theme options

## API Reference

### GraviteeMarkdownViewerComponent

#### Inputs
- `content: string` - The markdown content to render
- `darkTheme: boolean` - Enable dark theme (default: false)
- `highlightTheme: string` - Syntax highlighting theme (default: 'github')

#### Example
```html
<gravitee-markdown-viewer 
  [content]="markdownContent"
  [darkTheme]="true"
  [highlightTheme]="'monokai'">
</gravitee-markdown-viewer>
```

### GraviteeMarkdownEditorComponent

#### Inputs
- `darkTheme: boolean` - Enable dark theme (default: false)
- `placeholder: string` - Placeholder text for the editor (default: 'Enter your markdown content here...')

#### Outputs
- `contentChange: EventEmitter<string>` - Emitted when content changes

#### Example
```html
<gravitee-markdown-editor 
  [(ngModel)]="content"
  [darkTheme]="true"
  [placeholder]="'Start writing...'"
  (contentChange)="onContentChange($event)">
</gravitee-markdown-editor>
```

## Styling

The components use SCSS with CSS custom properties for easy theming. You can customize the appearance by overriding CSS variables:

```scss
.gravitee-markdown-viewer {
  --markdown-text-color: #333;
  --markdown-link-color: #1976d2;
  --markdown-code-bg: #f6f8fa;
  --markdown-border-color: #dfe2e5;
}
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Dependencies

- Angular 19+
- Angular Material 19+
- marked 12+
- highlight.js 11+

## Development

### Building the Library

```bash
# Build both projects
ng build gravitee-markdown-viewer
ng build gravitee-markdown-editor

# Build with watch mode
ng build gravitee-markdown-viewer --watch
ng build gravitee-markdown-editor --watch
```

### Testing

```bash
# Run tests for viewer
ng test gravitee-markdown-viewer

# Run tests for editor
ng test gravitee-markdown-editor
```

### Linting

```bash
# Lint viewer project
ng lint gravitee-markdown-viewer

# Lint editor project
ng lint gravitee-markdown-editor
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

Apache License 2.0

## Changelog

### v0.0.0
- Initial release
- Markdown viewer component with syntax highlighting
- Markdown editor component with live preview
- Dark/light theme support
- Angular Material integration 