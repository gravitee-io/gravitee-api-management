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
import * as emojiData from '@emoji-mart/data';
import { Emoji } from '@emoji-mart/data';
import hljs from 'highlight.js';
import { marked, Renderer, RendererObject } from 'marked';
import markedAlert from 'marked-alert';
import { markedEmoji } from 'marked-emoji';
// marked-extended-tables does not support proper typescript import so we need to import it like this pointing to the src/index file.
// @ts-expect-error Please do not change this import before checking if the issue is fixed in the library.
import markedExtendedTables from 'marked-extended-tables/src/index';
import { gfmHeadingId } from 'marked-gfm-heading-id';
import { markedHighlight } from 'marked-highlight';

import { Page } from '../entities/page/page';

@Injectable({
  providedIn: 'root',
})
export class MarkdownService {
  constructor() {
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
    marked.use(markedEmojiExtension());
    marked.use(markedExtendedTables());
    marked.use(markedAlertExtension());
  }

  public renderer(baseUrl: string, pageBaseUrl: string, pages: Page[]): RendererObject {
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
        const parsedRelativeDocApiUrl = /\/#!\/documentation\/api\/(.*)#([MARKDOWN|SWAGGER|OPENAPI|ASYNCAPI|ASCIIDOC]+)/g.exec(href);
        const parsedRelativeDocEnvUrl = /\/#!\/documentation\/environment\/(.*)#([MARKDOWN|SWAGGER|OPENAPI|ASYNCAPI|ASCIIDOC]+)/g.exec(
          href,
        );

        let pageId: string | undefined;

        if (parsedSettingsUrl) {
          pageId = parsedSettingsUrl[1];
        } else if (parsedApisUrl) {
          pageId = parsedApisUrl[1];
        } else if (parsedRelativeDocApiUrl) {
          pageId = pageIdFromParsedRelativeDocUrl(parsedRelativeDocApiUrl, pages);
        } else if (parsedRelativeDocEnvUrl) {
          pageId = pageIdFromParsedRelativeDocUrl(parsedRelativeDocEnvUrl, pages);
        }

        if (pageBaseUrl && pageId) {
          return `<a class="${INTERNAL_LINK_CLASSNAME}" href="${pageBaseUrl}/${pageId}">${text}</a>`;
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
    };
  }

  public render(content: string, baseUrl: string, pageBaseUrl: string, pages: Page[]): string {
    marked.use({ renderer: this.renderer(baseUrl, pageBaseUrl, pages) });
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

/**
 * Find the page ID from the parsed url.
 * If the page is not found, the page name is returned.
 *
 * @param parsedDocUrl - ex. ['/#!/documentation/api/my/doc%20page#MARKDOWN', 'my/doc%20page', 'MARKDOWN']
 * @param pages - pages to search for locating the relative documentation
 */
const pageIdFromParsedRelativeDocUrl = (parsedDocUrl: RegExpExecArray, pages: Page[]) => {
  const pagePath = parsedDocUrl[1];
  const pathWithSpaces = pagePath.replace(/%20/g, ' ');
  const splitPath = pathWithSpaces.split('/');
  const pageName = splitPath[splitPath.length - 1];

  const pageType = parsedDocUrl[2] === 'OPENAPI' ? 'SWAGGER' : parsedDocUrl[2];

  const pageId = findPageId(pageType, undefined, 0, splitPath, pages);
  return pageId ?? pageName;
};

/**
 * Find the page id given the path and name of the page.
 *
 * @param finalChildPageType - i.e. MARKDOWN
 * @param parentId
 * @param index
 * @param path
 * @param pages
 */
const findPageId = (
  finalChildPageType: string,
  parentId: string | undefined,
  index: number,
  path: string[],
  pages: Page[],
): string | undefined => {
  const findingFinalChildPage = index === path.length - 1;
  const typeToFind: string = findingFinalChildPage ? finalChildPageType : 'FOLDER';

  const page = pages.find(
    p => p.name.toLowerCase() === path[index].toLowerCase() && p.type.toString() === typeToFind && p.parent === parentId,
  );

  // If page not found
  if (!page) {
    return undefined;
  }

  return findingFinalChildPage ? page?.id : findPageId(finalChildPageType, page?.id, index + 1, path, pages);
};

const markedEmojiExtension = () => {
  const nameToEmoji: Record<string, string> = {};
  const data = emojiData as { emojis: Record<string, Emoji> };
  Object.values(data.emojis).forEach(emoji => {
    nameToEmoji[emoji.id] = emoji.skins[0].native;
  });

  return markedEmoji({
    emojis: nameToEmoji,
    renderer: token => token.emoji,
  });
};

const markedAlertExtension = () => {
  const icon = (icon: string, type: string) => `<span class="mat-icon marked-alert-icons marked-alert-icons--${type}">${icon}</span>`;
  return markedAlert({
    variants: [
      {
        type: 'note',
        icon: icon('info', 'note'),
      },
      {
        type: 'tip',
        icon: icon('lightbulb', 'tip'),
      },
      {
        type: 'important',
        icon: icon('error', 'important'),
      },
      {
        type: 'warning',
        icon: icon('warning', 'warning'),
      },
      {
        type: 'caution',
        icon: icon('report', 'caution'),
      },
    ],
  });
};
