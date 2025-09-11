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
import { Injectable } from '@angular/core';
import hljs from 'highlight.js';
import { marked, Renderer, RendererObject } from 'marked';
import { gfmHeadingId } from 'marked-gfm-heading-id';
import { markedHighlight } from 'marked-highlight';

const ANCHOR_CLASSNAME = 'anchor';

@Injectable()
export class GraviteeMarkdownViewerService {
  constructor() {
    marked.use(gfmHeadingId());
    marked.use(
      markedHighlight({
        langPrefix: 'hljs language-',
        highlight(_code, language) {
          const validLanguage = hljs.getLanguage(language) ? language : 'plaintext';
          return hljs.highlight(validLanguage, { language: validLanguage }).value;
        },
      }),
    );
    marked.setOptions({
      breaks: true,
      gfm: true,
    });
  }

  public getRenderer(): RendererObject {
    const defaultRenderer = new Renderer();
    return {
      image(href, title, text) {
        return defaultRenderer.image(href, title, text);
      },
      link(href, title, text) {
        if (href.startsWith('#')) {
          return `<a class="${ANCHOR_CLASSNAME}" href="${href}">${text}</a>`;
        }

        if (href?.startsWith('/#!/')) {
          const trimmedHref = href.substring(3);
          return defaultRenderer.link(trimmedHref, title, text);
        }

        return defaultRenderer.link(href, title, text);
      },
    };
  }

  public render(content: string): string {
    marked.use({ renderer: this.getRenderer() });
    return marked(content) as string;
  }
}
