import { CommonModule } from '@angular/common';
import { Component, input, ViewEncapsulation, effect } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { GraviteeMarkdownViewerService } from './gravitee-markdown-viewer.service';

@Component({
  selector: 'gravitee-markdown-viewer',
  standalone: true,
  imports: [CommonModule],
  template: ` <div class="gravitee-markdown-viewer" [class.dark-theme]="darkTheme()" [innerHTML]="renderedContent"></div> `,
  styleUrls: ['./gravitee-markdown-viewer.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class GraviteeMarkdownViewerComponent {
  content = input<string>('');
  darkTheme = input<boolean>(false);
  highlightTheme = input<string>('github');
  baseUrl = input<string>('');
  pageBaseUrl = input<string>('');

  renderedContent!: SafeHtml;

  constructor(
    private graviteeMarkdownViewerService: GraviteeMarkdownViewerService,
    private domSanitizer: DomSanitizer,
  ) {
    effect(() => {
      const renderedContent = this.graviteeMarkdownViewerService.render(this.content(), this.baseUrl(), this.pageBaseUrl());
      const parser = new DOMParser();
      const document = parser.parseFromString(renderedContent, 'text/html');
      this.renderedContent = this.domSanitizer.bypassSecurityTrustHtml(document.body.outerHTML);
    });
  }
}
