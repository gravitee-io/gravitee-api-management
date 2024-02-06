/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { markedHighlight } from 'marked-highlight';
import hljs from 'highlight.js';

@Injectable({
  providedIn: 'root',
})
export class MarkdownService {
  private readonly ANCHOR_CLASSNAME = 'anchor';
  private readonly INTERNAL_LINK_CLASSNAME = 'internal-link';

  public renderer(baseUrl: string, pageBaseUrl: string): RendererObject {
    const defaultRenderer = new Renderer();

    // Add to local scope in order to access in return object
    const anchorClassName = this.ANCHOR_CLASSNAME;
    const internalLinkClassName = this.INTERNAL_LINK_CLASSNAME;

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    return {
      image(href, title, text) {
        // is it a portal media ?
        let parsedURL = /\/environments\/(?:[\w-]+)\/portal\/media\/([\w-]+)/g.exec(href);
        if (parsedURL) {
          const portalHref = `${baseUrl}/media/${parsedURL[1]}`;
          return `<img alt="${text != null ? text : ''}" title="${title != null ? title : ''}" src="${portalHref}" />`;
        } else {
          // is it a API media ?
          parsedURL = /\/environments\/(?:[\w-]+)\/apis\/([\w-]+)\/media\/([\w-]+)/g.exec(href);
          if (parsedURL) {
            const portalHref = `${baseUrl}/apis/${parsedURL[1]}/media/${parsedURL[2]}`;
            return `<img alt="${text != null ? text : ''}" title="${title != null ? title : ''}" src="${portalHref}" />`;
          }
        }
        return defaultRenderer.image(href, title, text);
      },
      link(href, title, text) {
        // is it a portal page URL ?
        let parsedURL = /\/#!\/settings\/pages\/([\w-]+)/g.exec(href);
        if (!parsedURL) {
          // is it a API page URL ?
          parsedURL = /\/#!\/apis\/(?:[\w-]+)\/documentation\/([\w-]+)/g.exec(href);
        }

        if (parsedURL && pageBaseUrl) {
          const pageId = parsedURL[1];
          return `<a class="${internalLinkClassName}" href="${pageBaseUrl}?page=${pageId}">${text}</a>`;
        }

        if (href.startsWith('#')) {
          return `<a class="${anchorClassName}" href="${href}">${text}</a>`;
        }

        return defaultRenderer.link(href, title, text);
      },
    };
  }
  public render(content: string, baseUrl: string, pageBaseUrl: string): string {
    marked.use({ renderer: this.renderer(baseUrl, pageBaseUrl) });
    marked.use(gfmHeadingId());
    marked.use(
      markedHighlight({
        langPrefix: 'hljs language-',
        highlight(code, language) {
          const validLanguage = hljs.getLanguage(language) ? language : 'plaintext';
          return hljs.highlight(validLanguage, code).value;
        },
      }),
    );

    return marked(content) as string;
  }

  public getInternalClassName(): string {
    return this.INTERNAL_LINK_CLASSNAME;
  }

  public getAnchorClassName(): string {
    return this.ANCHOR_CLASSNAME;
  }
}
