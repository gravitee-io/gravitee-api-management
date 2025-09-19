/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { AfterViewInit, Component, ElementRef, inject, input, ViewChild } from '@angular/core';

import { GraviteeMarkdownRendererService } from '../../services/gravitee-markdown-renderer.service';

@Component({
  selector: 'gmd-card',
  templateUrl: 'gravitee-markdown-card.component.html',
  styleUrls: ['gravitee-markdown-card.component.scss'],
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
  preserveWhitespaces: true,
})
export class GraviteeMarkdownCardComponent implements AfterViewInit {
  @ViewChild('markdownSource', { static: true }) private mdContent!: ElementRef<HTMLElement>;
  parsedContent = '';

  backgroundColor = input(null);
  textColor = input(null);

  private readonly mdViewerService = inject(GraviteeMarkdownRendererService);
  private readonly host = inject(ElementRef<HTMLElement>);

  ngAfterViewInit(): void {
    this.applyCssInputsOverrides();

    if (this.mdContent) {
      const rawMarkdown = this.mdContent.nativeElement.textContent ?? '';
      const sanitizedMarkdown = this.removeIndentation(rawMarkdown);
      const parsed = this.mdViewerService.render(sanitizedMarkdown);
      const doc = new DOMParser().parseFromString(parsed, 'text/html');
      this.parsedContent = doc.body.outerHTML;
    }
  }

  private applyCssInputsOverrides() {
    const hostEl = this.host.nativeElement;
    if (this.backgroundColor()) {
      hostEl.style.setProperty('--gmd-card-container-color', this.backgroundColor()!);
    }
    if (this.textColor()) {
      hostEl.style.setProperty('--gmd-card-font-color', this.textColor()!);
    }
  }

  private removeIndentation(str: string): string {
    const lines = str.split('\n');
    const minIndent = lines.reduce((min, line) => {
      if (line.trim().length === 0) return min;
      const leadingSpaces = line.match(/^\s*/)?.[0].length ?? 0;
      return Math.min(min, leadingSpaces);
    }, Infinity);

    if (minIndent === Infinity) return str.trim();

    return lines
      .map(line => line.substring(minIndent))
      .join('\n')
      .trim();
  }
}
