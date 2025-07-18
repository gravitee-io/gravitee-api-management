import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GraviteeMarkdownEditorComponent } from './gravitee-markdown-editor.component';

@Component({
  selector: 'gravitee-markdown-editor-demo',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    GraviteeMarkdownEditorComponent
  ],
  template: `
    <div class="demo-container">
      <h1>Gravitee Markdown Editor Demo</h1>
      
      <div class="demo-section">
        <h2>Split View Editor</h2>
        <p>Edit markdown on the left, see the preview on the right.</p>
        <gravitee-markdown-editor
          [(ngModel)]="content"
          [placeholder]="'Start typing your markdown content here...'"
          (contentChange)="onContentChange($event)">
        </gravitee-markdown-editor>
      </div>
      
      <div class="demo-section">
        <h2>Current Content</h2>
        <pre>{{ content }}</pre>
      </div>
    </div>
  `,
  styles: [`
    .demo-container {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }
    
    .demo-section {
      margin-bottom: 40px;
    }
    
    .demo-section h2 {
      margin-bottom: 16px;
      color: #333;
    }
    
    .demo-section p {
      margin-bottom: 16px;
      color: #666;
    }
    
    pre {
      background-color: #f5f5f5;
      padding: 16px;
      border-radius: 4px;
      overflow-x: auto;
      font-family: 'DM Mono', monospace;
      font-size: 12px;
    }
  `]
})
export class GraviteeMarkdownEditorDemoComponent {
  content: string = `# Welcome to the Markdown Editor

This is a **bold** text and this is *italic* text.

## Features

- Split view: edit on the left, preview on the right
- Real-time preview with syntax highlighting
- Support for all standard markdown syntax
- Clean, simple interface

### Code Example

\`\`\`javascript
function hello() {
  console.log("Hello, World!");
}
\`\`\`

### Lists

1. First item
2. Second item
3. Third item

> This is a blockquote

[Link to Gravitee](https://gravitee.io)
`;

  onContentChange(value: string): void {
    console.log('Content changed:', value);
  }
} 