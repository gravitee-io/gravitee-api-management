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
import { faker } from '@faker-js/faker';
import {
  ApiType,
  ApiV4,
  CreateApiV4,
  DefinitionVersion,
  ExportApiV4,
  EndpointGroupV4,
  FlowExecution,
  FlowMode,
  HttpListener as GeneratedHttpListener,
  ListenerType,
  SubscriptionListener as GeneratedSubscriptionListener,
  FlowV4,
  TcpListener as GeneratedTcpListener,
} from '@gravitee/management-v2-webclient-sdk/src/lib';

/**
 * Aliases type for listeners based on their type
 */
export type SubscriptionListener = { type: 'SUBSCRIPTION' } & GeneratedSubscriptionListener;
export type HttpListener = { type: 'HTTP' } & GeneratedHttpListener;
export type TcpListener = { type: 'TCP' } & GeneratedTcpListener;

export class MAPIV2ApisFaker {
  static version() {
    const major = faker.number.int({ min: 1, max: 5 });
    const minor = faker.number.int({ min: 1, max: 10 });
    const patch = faker.number.int({ min: 1, max: 30 });
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

  static apiV4Proxy(attributes?: Partial<ApiV4>): ApiV4 {
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

  /**
   * A MESSAGE v4 API configured with HTTP-GET entrypoint and MOCK endpoint
   * @param attributes
   */
  static apiV4Message(attributes?: Partial<ApiV4>): ApiV4 {
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
          name: 'default-mock-group',
          type: 'mock',
          sharedConfiguration: {},
          endpoints: [
            {
              name: 'default-mock-endpoint',
              type: 'mock',
              inheritConfiguration: true,
              configuration: {
                messageContent: 'Mock message',
                messageInterval: 20,
                headers: [
                  {
                    name: 'X-Header',
                    value: 'header-value',
                  },
                ],
                metadata: [
                  {
                    name: 'Metadata',
                    value: 'metadata-value',
                  },
                ],
              },
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
              type: 'http-get',
              qos: 'AUTO',
              configuration: {
                headersInPayload: true,
                messagesLimitCount: 40,
                messagesLimitDurationMs: 2000,
                metadataInPayload: true,
              },
            },
          ],
        },
      ],
      tags: [],
      type: ApiType.MESSAGE,
      ...attributes,
    };
  }

  static apiV4NativeKafka(attributes?: Partial<ApiV4>): ApiV4 {
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
          name: 'Default Kafka Group',
          type: 'native-kafka',
          loadBalancer: { type: 'ROUND_ROBIN' },
          sharedConfiguration: {
            security: {
              protocol: 'SASL_SSL',
              sasl: {
                mechanism: {
                  type: 'PLAIN',
                  password: 'password',
                  username: 'username',
                },
              },
              ssl: {
                keyStore: {
                  type: '',
                },
                hostnameVerifier: true,
                trustStore: {
                  type: '',
                },
                trustAll: false,
              },
            },
          },
          endpoints: [
            {
              name: 'Kafka',
              type: 'native-kafka',
              inheritConfiguration: true,
              configuration: {
                bootstrapServers: 'bootstrap-server:9092',
              },
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
          type: ListenerType.KAFKA,
          host: '127.0.0.1',
          port: 9092,
          entrypoints: [
            {
              type: 'native-kafka',
              configuration: {},
            },
          ],
        },
      ],
      tags: [],
      type: ApiType.NATIVE,
      ...attributes,
    };
  }

  static apiImportV4(attributes?: Partial<ExportApiV4>): ExportApiV4 {
    return {
      api: this.apiV4Proxy(),
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
      type: 'HTTP',
      paths: [{ path: `/${faker.lorem.word()}-${faker.string.uuid()}-${Math.floor(Date.now() / 1000)}` }],
      pathMappings: [],
      entrypoints: [{ type: 'http-proxy' }],
      ...attributes,
    };
  }

  static newSubscriptionListener(attributes?: Partial<SubscriptionListener>): SubscriptionListener {
    return {
      type: 'SUBSCRIPTION',
      entrypoints: [],
      ...attributes,
    };
  }

  static newTcpListener(attributes?: Partial<TcpListener>): TcpListener {
    return {
      type: 'TCP',
      hosts: [],
      ...attributes,
    };
  }

  static newHttpEndpointGroup(attributes?: Partial<EndpointGroupV4>): EndpointGroupV4 {
    return {
      name: 'Default HTTP proxy group',
      type: 'http-proxy',
      loadBalancer: {
        type: 'ROUND_ROBIN',
      },
      endpoints: [
        {
          name: 'Default HTTP proxy',
          type: 'http-proxy',
          configuration: {
            target: `${Cypress.env('wiremockUrl')}/hello`,
          },
        },
      ],
      ...attributes,
    };
  }

  static newFlow(attributes?: Partial<FlowV4>): FlowV4 {
    return {
      name: `${faker.lorem.word()} flow`,
      enabled: true,
      selectors: [
        {
          type: 'HTTP',
          path: '/',
          pathOperator: 'EQUALS',
          methods: [],
        },
      ],
      request: [],
      response: [
        {
          name: 'Transform Headers',
          enabled: true,
          policy: 'transform-headers',
          configuration: {
            whitelistHeaders: [],
            addHeaders: [
              {
                name: 'x-header-added-by-common-flow',
                value: faker.lorem.word(),
              },
            ],
          },
        },
      ],
      subscribe: [],
      publish: [],
      entrypointConnect: [],
      interact: [],
      tags: [],
      ...attributes,
    };
  }
}
