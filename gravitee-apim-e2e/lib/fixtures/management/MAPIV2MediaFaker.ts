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
import faker from '@faker-js/faker';
import { Media } from '@gravitee/management-v2-webclient-sdk/src/lib';

export class MAPIV2MediaFaker {
  static media(attributes?: Partial<Media>): Media {
    return {
      hash: faker.internet.password(),
      type: 'image',
      subType: 'jpeg',
      fileName: faker.system.fileName(),
      size: faker.datatype.number({ min: 100, max: 50000 }),
      ...attributes,
    };
  }
}
