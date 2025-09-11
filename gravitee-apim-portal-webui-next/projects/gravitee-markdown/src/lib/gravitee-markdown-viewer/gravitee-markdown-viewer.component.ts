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
import { Component, effect } from '@angular/core';
import { HookParserEntry } from 'ngx-dynamic-hooks';

@Component({
  selector: 'gmd-viewer',
  templateUrl: './gravitee-markdown-viewer.component.html',
  styleUrl: './gravitee-markdown-viewer.component.scss',
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
})
export class GraviteeMarkdownViewerComponent {
  renderedContent!: string;
  parsers: HookParserEntry[] = [];

  constructor() {
    effect(() => {
      const renderedContent = 'TODO: PARSE CONTENT WITH DEDICATED SERVICE IN NEXT COMMIT';
      const parser = new DOMParser();
      const document = parser.parseFromString(renderedContent, 'text/html');
      this.renderedContent = document.body.outerHTML;
    });
  }
}
