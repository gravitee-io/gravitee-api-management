/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

@Injectable({
  providedIn: 'root',
})
export class MarkdownService {
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

  public renderer(baseUrl: string, pageBaseUrl: string): RendererObject {
    const defaultRenderer = new Renderer();

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
        const parsedSettingsUrl = /\/#!\/settings\/pages\/([\w-]+)/g.exec(href);
        const parsedApisUrl = /\/#!\/apis\/(?:[\w-]+)\/documentation\/([\w-]+)/g.exec(href);

        let pageId: string | undefined;

        if (parsedSettingsUrl) {
          pageId = parsedSettingsUrl[1];
        } else if (parsedApisUrl) {
          pageId = parsedApisUrl[1];
        }

        if (pageBaseUrl && pageId) {
          return `<a class="${INTERNAL_LINK_CLASSNAME}" href="${pageBaseUrl}?page=${pageId}">${text}</a>`;
        }

        if (href.startsWith('#')) {
          return `<a class="${ANCHOR_CLASSNAME}" href="${href}">${text}</a>`;
        }

        if (href?.startsWith('/#!/')) {
          const trimmedHref = href.substring(3);
          return defaultRenderer.link(trimmedHref, title, text);
        }

        return defaultRenderer.link(href, title, text);
      },
      html(html, _block) {
        // Hack to make the markdown work in the components...
        return html;
      },
    };
  }

  public render(content: string, baseUrl: string, pageBaseUrl: string): string {
    marked.use({ renderer: this.renderer(baseUrl, pageBaseUrl) });
    return marked(content) as string;
  }

  public getInternalClassName(): string {
    return INTERNAL_LINK_CLASSNAME;
  }

  public getAnchorClassName(): string {
    return ANCHOR_CLASSNAME;
  }
}

const ANCHOR_CLASSNAME = 'anchor';
const INTERNAL_LINK_CLASSNAME = 'internal-link';
