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
import { PageType, Visibility } from '../../management-webclient-sdk/src/lib/models';
import faker from '@faker-js/faker';
import { Page } from '@gravitee/management-v2-webclient-sdk/src/lib';

export class MAPIV2PagesFaker {
  static page(attributes?: Partial<Page>): Page {
    const content = faker.lorem.paragraph(3);
    const name = faker.lorem.words(4);

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
}
