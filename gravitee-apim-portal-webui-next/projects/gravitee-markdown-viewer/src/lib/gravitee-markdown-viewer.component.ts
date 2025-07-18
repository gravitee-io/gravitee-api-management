import { Component, Input, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { marked } from 'marked';
import { markedHighlight } from 'marked-highlight';
import hljs from 'highlight.js';

@Component({
  selector: 'gravitee-markdown-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div 
      class="gravitee-markdown-viewer"
      [class.dark-theme]="darkTheme"
      [innerHTML]="renderedContent">
    </div>
  `,
  styleUrls: ['./gravitee-markdown-viewer.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class GraviteeMarkdownViewerComponent implements OnChanges {
  @Input() content: string = '';
  @Input() darkTheme: boolean = false;
  @Input() highlightTheme: string = 'github';

  renderedContent: string = '';

  constructor() {
    this.setupMarked();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content'] || changes['highlightTheme']) {
      this.renderContent();
    }
  }

  private setupMarked(): void {
    // Configure marked with syntax highlighting
    marked.use(markedHighlight({
      langPrefix: 'hljs language-',
      highlight(code, lang) {
        const language = hljs.getLanguage(lang) ? lang : 'plaintext';
        return hljs.highlight(code, { language }).value;
      }
    }));

    // Set marked options
    marked.setOptions({
      breaks: true,
      gfm: true
    });
  }

  private async renderContent(): Promise<void> {
    if (!this.content) {
      this.renderedContent = '';
      return;
    }

    try {
      this.renderedContent = await marked(this.content);
    } catch (error) {
      console.error('Error rendering markdown:', error);
      this.renderedContent = '<p>Error rendering markdown content</p>';
    }
  }
} 