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
import * as faker from 'faker';
import { PageEntity, Visibility } from '../../management-webclient-sdk/src/lib';

export enum PageType {
  ASCIIDOC = 'ASCIIDOC',
  ASYNCAPI = 'ASYNCAPI',
  MARKDOWN = 'MARKDOWN',
  MARKDOWN_TEMPLATE = 'MARKDOWN_TEMPLATE',
  SWAGGER = 'SWAGGER',
  FOLDER = 'FOLDER',
  LINK = 'LINK',
  ROOT = 'ROOT',
  SYSTEM_FOLDER = 'SYSTEM_FOLDER',
  TRANSLATION = 'TRANSLATION',
}

export class PagesFaker {
  static page(attributes?: Partial<PageEntity>): PageEntity {
    const content = faker.lorem.paragraph(3);
    const name = faker.commerce.productName();

    return {
      name,
      type: PageType.MARKDOWN,
      content,
      order: 1,
      published: false,
      visibility: Visibility.PUBLIC,
      contentType: 'application/json',
      homepage: false,
      parentPath: '',
      excludedAccessControls: false,
      accessControls: [],
      ...attributes,
    };
  }

  static folder(attributes?: Partial<PageEntity>): PageEntity {
    return this.page({ ...attributes, content: null, type: PageType.FOLDER });
  }
}
