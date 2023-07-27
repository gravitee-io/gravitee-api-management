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
import {
  ApiType,
  ApiV4,
  CreateApiV4,
  DefinitionVersion,
  ExportApiV4,
  FlowExecution,
  FlowMode,
  HttpListener,
  ListenerType,
  SubscriptionListener,
} from '@gravitee/management-v2-webclient-sdk/src/lib';

export class MAPIV2ApisFaker {
  static version() {
    const major = faker.datatype.number({ min: 1, max: 5 });
    const minor = faker.datatype.number({ min: 1, max: 10 });
    const patch = faker.datatype.number({ min: 1, max: 30 });
    return `${major}.${minor}.${patch}`;
  }

  static newApi(attributes?: Partial<CreateApiV4>): CreateApiV4 {
    const name = faker.commerce.productName();
    const apiVersion = this.version();
    const description = faker.lorem.words(10);

    return {
      apiVersion,
      definitionVersion: DefinitionVersion.V4,
      description,
      name,
      endpointGroups: [
        {
          name: 'default-group',
          type: 'http-proxy',
          endpoints: [
            {
              name: 'default-endpoint',
              type: 'http-proxy',
            },
          ],
        },
      ],
      flowExecution: {
        flowMode: FlowMode.DEFAULT,
        matchRequired: false,
      } as FlowExecution,
      flows: [],
      groups: [],
      listeners: [],
      tags: [],
      type: ApiType.MESSAGE,
      ...attributes,
    };
  }

  static apiV4(attributes?: Partial<ApiV4>): ApiV4 {
    const name = faker.lorem.words(10);
    const apiVersion = this.version();
    const description = faker.lorem.words(10);

    return {
      apiVersion,
      definitionVersion: DefinitionVersion.V4,
      description,
      name,
      endpointGroups: [
        {
          name: 'default-group',
          type: 'http-proxy',
          endpoints: [
            {
              name: 'default-endpoint',
              type: 'http-proxy',
            },
          ],
        },
      ],
      flowExecution: {
        flowMode: FlowMode.DEFAULT,
        matchRequired: false,
      } as FlowExecution,
      flows: [],
      groups: [],
      listeners: [
        {
          type: ListenerType.HTTP,
          paths: [
            {
              path: `${faker.helpers.slugify(faker.lorem.words(3))}`,
            },
          ],
          entrypoints: [
            {
              type: 'http-proxy',
            },
          ],
        },
      ],
      tags: [],
      type: ApiType.PROXY,
      ...attributes,
    };
  }

  static apiImportV4(attributes?: Partial<ExportApiV4>): ExportApiV4 {
    return {
      api: this.apiV4(),
      members: [],
      metadata: [],
      pages: [],
      apiMedia: [],
      plans: [],
      apiPicture: null,
      apiBackground: null,
      ...attributes,
    };
  }

  static newHttpListener(attributes?: Partial<HttpListener>): HttpListener {
    return {
      // @ts-ignore
      type: 'HTTP',
      paths: [],
      pathMappings: [],
      entrypoints: [],
      ...attributes,
    };
  }

  static newSubscriptionListener(attributes?: Partial<SubscriptionListener>): SubscriptionListener {
    return {
      // @ts-ignore
      type: 'SUBSCRIPTION',
      entrypoints: [],
      ...attributes,
    };
  }
}
