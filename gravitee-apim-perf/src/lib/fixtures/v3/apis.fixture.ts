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
import { randomString, randomVersion } from '@helpers/random.helper';
import { NewApiEntity } from '@models/v3/NewApiEntity';
import { k6Options } from '@env/environment';

export class ApisFixture {
  static randomPath(): string {
    return `/${randomString()}-${Math.floor(Date.now() / 1000)}`;
  }
  static newApi(attributes?: Partial<NewApiEntity>): NewApiEntity {
    const name = randomString();
    const version = randomVersion();
    const description = `Description: ${randomString()}`;

    return {
      contextPath: this.randomPath(),
      name,
      description,
      version,
      endpoint: `${k6Options.apim.apiEndpointUrl}`,
      gravitee: '2.0.0',
      ...attributes,
    };
  }
}
