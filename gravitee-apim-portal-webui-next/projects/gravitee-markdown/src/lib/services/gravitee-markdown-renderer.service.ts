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
import { marked, Renderer, RendererObject } from 'marked';
import { gfmHeadingId } from 'marked-gfm-heading-id';

import { normalizeIndentation } from './utils/indentation-normalizer';

const ANCHOR_CLASSNAME = 'anchor';

@Injectable({
  providedIn: 'root',
})
export class GraviteeMarkdownRendererService {
  constructor() {
    marked.use(gfmHeadingId());
    marked.setOptions({
      breaks: true,
      gfm: true,
    });

    /*
     * This extension has been created to ignore any component stating with `<gmd` selector with marked.
     * If a component needs to parse its content, it should be implemented as preprocessing mechanism in the renderer method.
     */
    marked.use({
      extensions: [
        {
          name: 'gmd-any',
          level: 'block',
          start(src) {
            return src.match(/<gmd-[a-z0-9-]*\b/i)?.index;
          },
          tokenizer(src) {
            // Match opening <gmd-xxx ...> until matching closing </gmd-xxx>
            const rule = /^<gmd-[a-z0-9-]*\b[^>]*>[\s\S]*?<\/gmd-[a-z0-9-]*>/i;
            const match = rule.exec(src.trim());
            if (match) {
              return {
                type: 'gmd-any',
                raw: src,
              };
            }
            return;
          },
          renderer(token) {
            return token.raw;
          },
        },
      ],
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
    const processed = this.preprocessGmdBlocks(content);
    return marked(processed) as string;
  }

  /**
   * Preprocess the HTML content to parse and transform the markdown inside gmd-block tags
   * @param html The HTML content to preprocess
   * @returns The HTML content with gmd-blocks transformed
   */
  public preprocessGmdBlocks(html: string): string {
    return html.replace(/<gmd-md>([\s\S]*?)<\/gmd-md>/gi, (_, rawContent) => {
      const normalized = normalizeIndentation(rawContent);
      const rendered = marked(normalized) as string;
      return `<gmd-md>${rendered}</gmd-md>`;
    });
  }
}
