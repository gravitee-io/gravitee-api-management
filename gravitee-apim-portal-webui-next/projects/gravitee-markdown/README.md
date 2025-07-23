# Gravitee Markdown Library

A comprehensive markdown library for Angular applications with viewer and editor components.

## Features

- **Markdown Viewer**: Renders markdown content with syntax highlighting
- **Markdown Editor**: Full-featured editor with live preview using Monaco Editor
- **Split View**: Edit on the left, preview on the right
- **Real-time Preview**: See changes as you type
- **Syntax Highlighting**: Code blocks with language-specific highlighting
- **GitHub Flavored Markdown**: Full GFM support

## Installation

```bash
yarn add @gravitee/gravitee-markdown
```

## Usage

### Markdown Editor

The markdown editor uses Monaco Editor directly for a better development experience:

```typescript
import { GraviteeMarkdownEditorComponent } from '@gravitee/gravitee-markdown';

@Component({
  selector: 'app-editor',
  template: `
    <gravitee-markdown-editor
      placeholder="Start writing your markdown..."
      [darkTheme]="false"
      (contentChange)="onContentChange($event)"
      (errorChange)="onErrorChange($event)">
    </gravitee-markdown-editor>
  `,
  imports: [GraviteeMarkdownEditorComponent]
})
export class EditorComponent {
  onContentChange(content: string) {
    console.log('Content changed:', content);
  }

  onErrorChange(error: string | null) {
    if (error) {
      console.error('Editor error:', error);
    }
  }
}
```

### Markdown Viewer

```typescript
import { GraviteeMarkdownViewerComponent } from '@gravitee/gravitee-markdown';

@Component({
  selector: 'app-viewer',
  template: `
    <gravitee-markdown-viewer
      [content]="markdownContent"
      baseUrl="https://example.com"
      pageBaseUrl="https://example.com/docs">
    </gravitee-markdown-viewer>
  `,
  imports: [GraviteeMarkdownViewerComponent]
})
export class ViewerComponent {
  markdownContent = `# Hello World

This is **bold** text and *italic* text.

\`\`\`javascript
console.log('Hello, World!');
\`\`\`
`;
}
```

## Editor Features

- **Monaco Editor Integration**: Uses Monaco Editor directly for better performance
- **Live Preview**: Real-time markdown rendering
- **Split View**: Edit and preview side by side
- **Theme Support**: Light and dark themes
- **Error Handling**: Content validation and error reporting
- **Responsive Design**: Works on desktop and mobile

## Dependencies

- `monaco-editor`: For the code editor functionality
- `marked`: For markdown parsing
- `highlight.js`: For syntax highlighting
- Angular 19+ components

## Migration from ngx-monaco-editor-v2

This library previously used `ngx-monaco-editor-v2` but has been migrated to use Monaco Editor directly for better performance and reliability. The API remains the same, so no changes are needed in your existing code.
