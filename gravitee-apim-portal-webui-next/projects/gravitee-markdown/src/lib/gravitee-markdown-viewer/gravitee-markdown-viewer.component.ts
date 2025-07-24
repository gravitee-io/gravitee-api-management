import { CommonModule } from '@angular/common';
import { Component, input, ViewEncapsulation, effect, signal } from '@angular/core';
import { DynamicHooksComponent, ParseOptions } from 'ngx-dynamic-hooks';

import { appPrefixStripperParsers } from '../component-library/app-prefix-stripper.parser';
import { MarkdownService } from '../services/markdown.service';

@Component({
  selector: 'gravitee-markdown-viewer',
  standalone: true,
  imports: [CommonModule, DynamicHooksComponent],
  template: `
    <div class="gravitee-markdown-viewer" [class.dark-theme]="darkTheme()">
      <ngx-dynamic-hooks
        [content]="renderedContent"
        [parsers]="parsers"
        [options]="options"
      ></ngx-dynamic-hooks>
    </div>
  `,
  styleUrls: ['./gravitee-markdown-viewer.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class GraviteeMarkdownViewerComponent {
  content = input<string>('');
  darkTheme = input<boolean>(false);
  highlightTheme = input<string>('github');
  baseUrl = input<string>('');
  pageBaseUrl = input<string>('');

  renderedContent!: string;
  parsers = appPrefixStripperParsers;
  dynamicContext = signal<any>({});
  options: ParseOptions | null = {
    sanitize: false,
  }

  constructor(
    private markdownService: MarkdownService,
  ) {
    effect(() => {
      const renderedContent = this.markdownService.render(this.content(), this.baseUrl(), this.pageBaseUrl());
      const parser = new DOMParser();
      const document = parser.parseFromString(renderedContent, 'text/html');
      this.renderedContent = document.body.outerHTML;
    });
  }
}
