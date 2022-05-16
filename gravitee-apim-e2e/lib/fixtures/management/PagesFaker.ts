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
import { PageEntity } from '@management-models/PageEntity';
import { Visibility } from '@management-models/Visibility';
import faker from '@faker-js/faker';
import { NewPageEntity } from '@management-models/NewPageEntity';
import { PageType } from '@management-models/PageType';

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

  static newPage(attributes?: Partial<NewPageEntity>): NewPageEntity {
    return {
      name: 'Unpublished Page',
      type: PageType.SWAGGER,
      published: false,
      ...attributes,
    };
  }
}
