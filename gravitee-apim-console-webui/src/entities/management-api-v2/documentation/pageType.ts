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
const PageTypeEnum = {
  ASCIIDOC: 'ASCIIDOC',
  ASYNCAPI: 'ASYNCAPI',
  MARKDOWN: 'MARKDOWN',
  MARKDOWN_TEMPLATE: 'MARKDOWN_TEMPLATE',
  SWAGGER: 'SWAGGER',
  FOLDER: 'FOLDER',
  LINK: 'LINK',
  ROOT: 'ROOT',
  SYSTEM_FOLDER: 'SYSTEM_FOLDER',
  TRANSLATION: 'TRANSLATION',
} as const;
export type PageType = (typeof PageTypeEnum)[keyof typeof PageTypeEnum];
export const SUPPORTED_FOR_EDIT: PageType[] = ['MARKDOWN', 'SWAGGER', 'ASYNCAPI', 'ASCIIDOC'];

export function getTooltipForPageType(pageType: PageType) {
  switch (pageType) {
    case 'ASCIIDOC':
      return 'AsciiDoc';
    case 'ASYNCAPI':
      return 'AsyncAPI';
    case 'SWAGGER':
      return 'OpenAPI';
    case 'MARKDOWN':
      return 'Markdown';
  }
}

export function getTitleForPageType(pageType: PageType) {
  switch (pageType) {
    case 'ASCIIDOC':
      return 'AsciiDoc';
    case 'ASYNCAPI':
      return 'AsyncAPI';
    case 'SWAGGER':
      return 'OpenAPI';
    case 'MARKDOWN':
      return 'Markdown';
  }
}

export const getLogoForPageType = (pageType: PageType) => {
  if (!pageType) {
    return undefined;
  }
  if (pageType === 'SWAGGER') {
    return `assets/logo_openapi.svg`;
  }
  return `assets/logo_${pageType.toLowerCase()}.svg`;
};
