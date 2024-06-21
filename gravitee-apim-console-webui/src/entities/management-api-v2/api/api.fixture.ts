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
import { isFunction } from 'lodash';

import { BaseApi, ApiV2, ApiV4, ApiV1, ManagementContext, GenericApi, ApiFederated } from '.';

export function fakeBaseApi(modifier?: Partial<BaseApi> | ((baseApi: BaseApi) => BaseApi)): BaseApi {
  const base: BaseApi = {
    id: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
    name: '\uD83E\uDE90 Planets',
    description: 'The whole universe in your hand.',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeGenericApi(modifier?: Partial<GenericApi> | ((baseApi: GenericApi) => GenericApi)): GenericApi {
  const base: GenericApi = {
    ...fakeBaseApi({ ...modifier }),
    apiVersion: '1.0',
    definitionVersion: 'V2',
    deployedAt: new Date(),
    createdAt: new Date(),
    updatedAt: new Date(),
    disableMembershipNotifications: false,
    groups: ['f1194262-9157-4986-9942-629157f98682'],
    state: 'STARTED',
    visibility: 'PUBLIC',
    tags: [],
    labels: [],
    definitionContext: {
      origin: 'MANAGEMENT',
    },
    originContext: new ManagementContext(),
    responseTemplates: {
      customKey: {
        '*/*': {
          statusCode: 400,
          body: '',
        },
      },
      DEFAULT: {
        '*/*': {
          statusCode: 400,
          body: '',
        },
        test: {
          statusCode: 400,
          body: '',
        },
      },
    },
    resources: [
      {
        name: 'my-cache',
        type: 'cache',
        enabled: true,
        configuration: '{ timeToIdleSeconds: 60, timeToLiveSeconds: 60, maxEntriesLocalHeap: 1000 }',
      },
    ],
    properties: [],
    primaryOwner: {
      id: 'f1194262-9157-4986-9942-629157f98682',
      displayName: 'admin',
      email: 'admin@gio.com',
    },
    lifecycleState: 'PUBLISHED',
    _links: {
      pictureUrl: 'https://api.company.com/environment/default/apis/aee23b1e-34b1-4551-a23b-1e34b165516a/picture',
      backgroundUrl: 'https://api.company.com/environment/default/apis/aee23b1e-34b1-4551-a23b-1e34b165516a/background',
    },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeApiV1(modifier?: Partial<ApiV1> | ((baseApi: ApiV1) => ApiV1)): ApiV1 {
  const base: ApiV1 = {
    ...fakeGenericApi({ ...modifier }),
    definitionVersion: 'V1',
    environmentId: 'my-environment',
    entrypoints: [
      {
        target: 'https://api.company.com/planets',
      },
    ],
    contextPath: '/planets',
    proxy: {
      virtualHosts: [
        {
          path: '/planets',
          overrideEntrypoint: true,
        },
      ],
      stripContextPath: false,
      preserveHost: false,
      logging: {
        mode: 'PROXY',
        content: 'PAYLOADS',
        scope: 'REQUEST_RESPONSE',
      },
      groups: [
        {
          name: 'default-group',
          endpoints: [
            {
              name: 'default',
              target: 'https://api.le-systeme-solaire.net/rest/',
              weight: 1,
              backup: false,
              type: 'HTTP',
              inherit: true,
            },
          ],
          loadBalancer: {
            type: 'ROUND_ROBIN',
          },
          httpClientOptions: {
            connectTimeout: 5000,
            idleTimeout: 60000,
            keepAlive: true,
            readTimeout: 10000,
            pipelining: false,
            maxConcurrentConnections: 100,
            useCompression: true,
            followRedirects: false,
          },
        },
      ],
    },
    paths: [],
    pathMappings: [],
    executionMode: 'V3',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeApiV2(modifier?: Partial<ApiV2> | ((baseApi: ApiV2) => ApiV2)): ApiV2 {
  const base: ApiV2 = {
    ...fakeGenericApi({ ...modifier }),
    definitionVersion: 'V2',
    entrypoints: [
      {
        target: 'https://api.company.com/planets',
      },
    ],
    contextPath: '/planets',
    proxy: {
      virtualHosts: [
        {
          path: '/planets',
          overrideEntrypoint: true,
        },
      ],
      stripContextPath: false,
      preserveHost: false,
      logging: {
        mode: 'PROXY',
        content: 'PAYLOADS',
        scope: 'REQUEST_RESPONSE',
      },
      groups: [
        {
          name: 'default-group',
          endpoints: [
            {
              name: 'default',
              target: 'https://api.le-systeme-solaire.net/rest/',
              weight: 1,
              backup: false,
              type: 'HTTP',
              inherit: true,
            },
          ],
          loadBalancer: {
            type: 'ROUND_ROBIN',
          },
          httpClientOptions: {
            connectTimeout: 5000,
            idleTimeout: 60000,
            keepAlive: true,
            readTimeout: 10000,
            pipelining: false,
            maxConcurrentConnections: 100,
            useCompression: true,
            followRedirects: false,
          },
        },
      ],
    },
    flowMode: 'DEFAULT',
    flows: [
      {
        id: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
        name: '',
        pathOperator: {
          path: '/',
          operator: 'STARTS_WITH',
        },
        condition: '',
        consumers: [],
        methods: [],
        pre: [
          {
            name: 'Mock',
            description: 'Saying hello to the world',
            enabled: true,
            policy: 'mock',
            configuration: { content: 'Hello world', status: '200' },
          },
        ],
        post: [],
        enabled: true,
      },
    ],
    services: {
      healthCheck: {
        enabled: true,
        schedule: '0 */1 * * * *',
        steps: [
          {
            name: 'default-step',
            request: {
              path: '/',
              method: 'GET',
              fromRoot: true,
            },
            response: {
              assertions: ['#response.status == 200'],
            },
          },
        ],
      },
    },
    pathMappings: ['/product/:id'],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeApiV4(modifier?: Partial<ApiV4> | ((baseApi: ApiV4) => ApiV4)): ApiV4 {
  const base: ApiV4 = {
    ...fakeGenericApi({ ...modifier }),
    definitionVersion: 'V4',
    type: 'MESSAGE',
    listeners: [
      {
        type: 'SUBSCRIPTION',
        entrypoints: [
          {
            type: 'webhook',
          },
        ],
      },
    ],
    endpointGroups: [
      {
        name: 'default-group',
        type: 'kafka',
        loadBalancer: {
          type: 'ROUND_ROBIN',
        },
        endpoints: [
          {
            name: 'default',
            type: 'kafka',
            weight: 1,
            inheritConfiguration: false,
            configuration: {
              bootstrapServers: 'localhost:9092',
            },
          },
        ],
      },
    ],
    flows: [
      {
        name: '',
        selectors: [],
        request: [],
        response: [],
        subscribe: [],
        publish: [],
        enabled: true,
      },
    ],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeProxyApiV4(modifier?: Partial<ApiV4> | ((baseApi: ApiV4) => ApiV4)): ApiV4 {
  const base: ApiV4 = {
    ...fakeGenericApi({ ...modifier }),
    definitionVersion: 'V4',
    type: 'PROXY',
    listeners: [
      {
        type: 'HTTP',
        entrypoints: [
          {
            type: 'http-proxy',
          },
        ],
      },
    ],
    endpointGroups: [
      {
        name: 'Default Endpoint HTTP proxy group',
        type: 'http-proxy',
        loadBalancer: {
          type: 'ROUND_ROBIN',
        },
        sharedConfiguration: {
          proxy: {
            useSystemProxy: false,
            enabled: false,
          },
          http: {
            keepAlive: true,
            followRedirects: false,
            readTimeout: 10000,
            idleTimeout: 60000,
            connectTimeout: 3000,
            useCompression: true,
            maxConcurrentConnections: 20,
            version: 'HTTP_1_1',
            pipelining: false,
          },
          ssl: {
            hostnameVerifier: true,
            trustAll: false,
            truststore: {
              type: '',
            },
            keystore: {
              type: '',
            },
          },
        },
        endpoints: [
          {
            name: 'Default Endpoint HTTP proxy',
            type: 'http-proxy',
            weight: 1,
            inheritConfiguration: true,
            configuration: {
              target: 'https://api.gravitee.io/echo',
            },
            services: {},
            secondary: false,
          },
        ],
        services: {},
      },
    ],
    flows: [
      {
        name: '',
        selectors: [],
        request: [],
        response: [],
        subscribe: [],
        publish: [],
        enabled: true,
      },
    ],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeProxyTcpApiV4(modifier?: Partial<ApiV4> | ((baseApi: ApiV4) => ApiV4)): ApiV4 {
  const base: ApiV4 = {
    ...fakeGenericApi({ ...modifier }),
    definitionVersion: 'V4',
    type: 'PROXY',
    listeners: [
      {
        type: 'TCP',
        entrypoints: [
          {
            type: 'tcp-proxy',
          },
        ],
        hosts: ['a-tcp-api'],
      },
    ],
    endpointGroups: [
      {
        name: 'Default Endpoint TCP proxy group',
        type: 'tcp-proxy',
        loadBalancer: {
          type: 'ROUND_ROBIN',
        },
        sharedConfiguration: {
          tcp: {
            reconnectAttempts: 3,
            readIdleTimeout: 0,
            idleTimeout: 0,
            connectTimeout: 3000,
            reconnectInterval: 1000,
            writeIdleTimeout: 0,
          },
          ssl: {
            hostnameVerifier: true,
            trustAll: false,
            truststore: {
              type: '',
            },
            keystore: {
              type: '',
            },
          },
        },
        endpoints: [
          {
            name: 'Default TCP proxy',
            type: 'tcp-proxy',
            secondary: false,
            weight: 1,
            inheritConfiguration: true,
            configuration: {
              target: {
                port: 80,
                host: 'backend.host',
                secured: false,
              },
            },
            services: {},
          },
        ],
        services: {},
      },
    ],
    flows: [
      {
        name: '',
        selectors: [],
        request: [],
        response: [],
        subscribe: [],
        publish: [],
        enabled: true,
      },
    ],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeApiFederated(modifier?: Partial<ApiFederated> | ((baseApi: ApiFederated) => ApiFederated)): ApiFederated {
  const base: ApiFederated = {
    apiVersion: '1.0.0',
    id: 'myId',
    name: 'myName',
    definitionVersion: 'FEDERATED',
    originContext: {
      origin: 'INTEGRATION',
      integrationId: 'integration-id',
      integrationName: 'name of integration',
      provider: 'provider name',
    },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
