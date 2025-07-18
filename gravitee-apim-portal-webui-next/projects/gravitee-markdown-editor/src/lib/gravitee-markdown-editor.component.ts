import { Component, Input, Output, EventEmitter, forwardRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { marked } from 'marked';
import { markedHighlight } from 'marked-highlight';
import hljs from 'highlight.js';

@Component({
  selector: 'gravitee-markdown-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GraviteeMarkdownEditorComponent),
      multi: true
    }
  ],
  templateUrl: './gravitee-markdown-editor.component.html',
  styleUrls: ['./gravitee-markdown-editor.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class GraviteeMarkdownEditorComponent implements ControlValueAccessor {
  @Input() placeholder: string = 'Enter your markdown content here...';
  @Output() contentChange = new EventEmitter<string>();

  content: string = '';
  renderedContent: string = '';
  
  private onChange = (value: string) => {};
  private onTouched = () => {};

  constructor() {
    this.setupMarked();
  }

  writeValue(value: string): void {
    this.content = value || '';
    this.renderContent();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    // Handle disabled state if needed
  }

  onContentChange(value: string): void {
    this.content = value;
    this.renderContent();
    this.onChange(value);
    this.onTouched();
    this.contentChange.emit(value);
  }

  private setupMarked(): void {
    marked.use(markedHighlight({
      langPrefix: 'hljs language-',
      highlight(code, lang) {
        const language = hljs.getLanguage(lang) ? lang : 'plaintext';
        return hljs.highlight(code, { language }).value;
      }
    }));

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