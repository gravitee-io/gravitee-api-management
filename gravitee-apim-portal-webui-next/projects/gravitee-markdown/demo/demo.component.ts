import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GraviteeMarkdownViewerComponent } from '../../gravitee-markdown-viewer/src/lib/gravitee-markdown-viewer.component';
import { GraviteeMarkdownEditorComponent } from '../../gravitee-markdown-editor/src/lib/gravitee-markdown-editor.component';

@Component({
  selector: 'gravitee-markdown-demo',
  standalone: true,
  imports: [CommonModule, FormsModule, GraviteeMarkdownViewerComponent, GraviteeMarkdownEditorComponent],
  template: `
    <div class="demo-container">
      <h1>Gravitee Markdown Library Demo</h1>
      
      <div class="demo-section">
        <h2>Markdown Viewer</h2>
        <gravitee-markdown-viewer 
          [content]="sampleMarkdown"
          [darkTheme]="darkTheme">
        </gravitee-markdown-viewer>
      </div>
      
      <div class="demo-section">
        <h2>Markdown Editor</h2>
        <gravitee-markdown-editor 
          [(ngModel)]="editableContent"
          [darkTheme]="darkTheme"
          (contentChange)="onContentChange($event)">
        </gravitee-markdown-editor>
      </div>
      
      <div class="demo-controls">
        <button (click)="toggleTheme()">
          Toggle {{ darkTheme ? 'Light' : 'Dark' }} Theme
        </button>
      </div>
    </div>
  `,
  styles: [`
    .demo-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
    }
    
    .demo-section {
      margin-bottom: 40px;
      padding: 20px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
    }
    
    .demo-controls {
      text-align: center;
      margin-top: 20px;
    }
    
    button {
      padding: 10px 20px;
      background-color: #1976d2;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
    }
    
    button:hover {
      background-color: #1565c0;
    }
  `]
})
export class GraviteeMarkdownDemoComponent {
  darkTheme = false;
  editableContent = '# Welcome to Gravitee Markdown Editor\n\nThis is a **powerful** markdown editor with live preview.\n\n## Features\n\n- **Syntax highlighting** for code blocks\n- **Live preview** in real-time\n- **Dark/Light theme** support\n- **GitHub Flavored Markdown** support\n\n```javascript\n// Example code block\nfunction hello() {\n  console.log("Hello, World!");\n}\n```\n\n> This is a blockquote example.\n\n| Feature | Status |\n|---------|--------|\n| Viewer | ✅ |\n| Editor | ✅ |\n| Themes | ✅ |';
  
  sampleMarkdown = `# Gravitee Markdown Library

A comprehensive Angular library for markdown viewing and editing capabilities.

## Features

- **Syntax Highlighting**: Code blocks with language-specific highlighting
- **GitHub Flavored Markdown**: Support for tables, strikethrough, and more
- **Customizable Themes**: Multiple theme options for both viewer and editor
- **Accessibility**: WCAG compliant components
- **TypeScript Support**: Full TypeScript definitions

## Code Example

\`\`\`typescript
import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown/viewer';

@NgModule({
  imports: [GraviteeMarkdownViewerModule],
  // ...
})
export class AppModule { }
\`\`\`

## Table Example

| Component | Description |
|-----------|-------------|
| Viewer | Renders markdown content |
| Editor | Edits markdown with preview |

> **Note**: This library is designed to be lightweight and performant.

### Installation

\`\`\`bash
npm install @gravitee/gravitee-markdown
\`\`\``;

  onContentChange(content: string): void {
    console.log('Content changed:', content);
  }

  toggleTheme(): void {
    this.darkTheme = !this.darkTheme;
  }
} 