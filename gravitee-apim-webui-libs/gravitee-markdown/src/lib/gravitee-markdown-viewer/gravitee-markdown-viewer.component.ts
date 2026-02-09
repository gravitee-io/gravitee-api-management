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
import { Component, effect, input, ViewEncapsulation } from '@angular/core';
import DOMPurify from 'dompurify';
import { HookParserEntry } from 'ngx-dynamic-hooks';

import { componentAttributeNames } from '../components/component-attribute-selectors';
import { prefixStripperParser } from '../components/prefix-stripper.parser';
import { ComponentSelector } from '../models/componentSelector';
import { GraviteeMarkdownRendererService } from '../services/gravitee-markdown-renderer.service';

@Component({
  selector: 'gmd-viewer',
  templateUrl: './gravitee-markdown-viewer.component.html',
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
  encapsulation: ViewEncapsulation.ShadowDom,
})
export class GraviteeMarkdownViewerComponent {
  content = input<string>('');
  renderedContent!: string;
  parsers: HookParserEntry[] = prefixStripperParser;

  constructor(private readonly markdownService: GraviteeMarkdownRendererService) {
    effect(() => {
      const parser = new DOMParser();
      const parsedContent = this.markdownService.render(this.content().trim());
      const document = parser.parseFromString(parsedContent, 'text/html');

      // DO NOT CHANGE
      // This performs necessary security checks against XSS attacks
      this.renderedContent = DOMPurify.sanitize(document.body.outerHTML, {
        ADD_TAGS: [...Object.values(ComponentSelector)],
        ADD_ATTR: componentAttributeNames,
      });
    });
  }
}
