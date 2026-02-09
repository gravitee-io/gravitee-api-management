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
import { selfClosingComponentNames } from '../components/component-name-selectors';

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
          tokenizer: src => {
            const openingGmdTagMatch = src.match(/^<gmd-([a-z0-9-]*)\b[^>]*>/i);
            if (!openingGmdTagMatch) {
              return;
            }

            const gmdComponentName = this.extractGmdComponentNameFromTag(openingGmdTagMatch[0]);
            if (!gmdComponentName) {
              return;
            }

            // Find the first complete component
            const domParser = new DOMParser();
            const doc = domParser.parseFromString(src, 'text/html');
            const foundComponent = doc.body.querySelector(gmdComponentName);

            if (foundComponent) {
              return {
                type: 'gmd-any',
                raw: foundComponent.outerHTML,
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

    // Minimal normalization: DOMParser (and HTML parsing in general) is flaky with self-closing custom elements.
    // Convert ONLY known self-closing form components to opening/closing format.
    const normalizedContent = this.normalizeSelfClosingFormComponents(content);

    const processed = this.preprocessGmdBlocks(normalizedContent);
    const parsed = new DOMParser().parseFromString(processed, 'text/html');
    const decoded = this.decodeMarkdown(parsed.body.innerHTML);
    return marked(decoded) as string;
  }

  /**
   * Preprocess the HTML content to parse and transform the markdown inside gmd-block tags
   * @param html The HTML content to preprocess
   * @returns The HTML content with gmd-blocks transformed
   */
  public preprocessGmdBlocks(html: string): string {
    return html.replace(/<gmd-md(?:\s+[^>]+)?>([\s\S]*?)<\/gmd-md>/gi, (match, rawContent) => {
      const attributes = this.extractAttributesFromGmdMdTag(match);
      const normalized = normalizeIndentation(rawContent);
      const rendered = marked(normalized) as string;
      return `<gmd-md${attributes}>${rendered}</gmd-md>`;
    });
  }

  /**
   * Convert self-closing form component tags to opening/closing format.
   * This keeps the renderer stable when content contains `<gmd-xxx />` for custom elements.
   *
   * Examples:
   *  - <gmd-input ... />
   *  - <gmd-textarea .../>
   */
  private normalizeSelfClosingFormComponents(content: string): string {
    let result = content;

    for (const componentName of selfClosingComponentNames) {
      // NOTE: use [^>]* so we never "jump" across multiple tags until a later "/>"
      const re = new RegExp(`<gmd-${componentName}\\b([^>]*)\\/\\s*>`, 'gi');
      result = result.replace(re, `<gmd-${componentName}$1></gmd-${componentName}>`);
    }

    return result;
  }

  /**
   * Extract attributes from a gmd-md opening tag
   * @param tag The opening tag string (e.g., '<gmd-md style="color: red;" class="my-class">')
   * @returns The attributes string with leading space, or empty string if no attributes
   */
  private extractAttributesFromGmdMdTag(tag: string): string {
    const attributesMatch = tag.match(/<gmd-md\s+(.+?)>/);
    return attributesMatch ? ` ${attributesMatch[1]}` : '';
  }

  /**
   * Extract the gmd component name from an opening tag string
   *
   * @param openingTag extracted from dom, e.g. <gmd-card title="My Card">
   * @private
   */
  private extractGmdComponentNameFromTag(openingTag: string): string | null {
    const componentNameMatch = openingTag.match(/<gmd-([a-z0-9-]*)\b[^>]*>/i);
    if (!componentNameMatch) {
      return null;
    }
    const componentName = componentNameMatch[1]; // Group found in the regex
    return `gmd-${componentName}`;
  }

  /**
   * Decodes specific markdown elements by replacing encoded entities.
   *
   * @param {string} content - The markdown content as a string.
   * @return {string} The decoded markdown content with replacements applied.
   */
  private decodeMarkdown(content: string): string {
    // Replace greater than for blockquote
    return content.replace(/^&gt;/gm, '>');
  }
}
