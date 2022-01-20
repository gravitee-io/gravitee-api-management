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

import { UpdateApi } from './UpdateApi';

export function fakeUpdateApi(modifier?: Partial<UpdateApi> | ((baseApi: UpdateApi) => UpdateApi)): UpdateApi {
  const base: UpdateApi = {
    version: '1.0',
    description: 'The whole universe in your hand. s',
    proxy: {
      virtual_hosts: [{ path: '/planets', override_entrypoint: true }],
      strip_context_path: false,
      preserve_host: false,
      logging: { mode: 'PROXY', content: 'PAYLOADS', scope: 'REQUEST_RESPONSE' },
      groups: [
        {
          name: 'default-group',
          endpoints: [
            { name: 'default', target: 'https://api.le-systeme-solaire.net/rest/', weight: 1, backup: false, type: 'HTTP', inherit: true },
          ],
          load_balancing: { type: 'ROUND_ROBIN' },
          http: {
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
    flows: [
      {
        name: '',
        'path-operator': { path: '/', operator: 'STARTS_WITH' },
        condition: '',
        consumers: [],
        methods: [],
        pre: [
          { name: 'Latency', policy: 'latency', enabled: true, configuration: { time: 100, timeUnit: 'MILLISECONDS' } },
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
    plans: [
      {
        id: '6ba87328-4f79-45b2-a873-284f7935b2f5',
        name: 'Spaceshuttle',
        security: 'KEY_LESS',
        paths: {},
        api: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
        flows: [],
        status: 'PUBLISHED',
      },
      {
        id: '5d603bb8-6aef-4f09-a03b-b86aef6f0926',
        name: 'Paid Plan',
        security: 'API_KEY',
        securityDefinition: '{"propagateApiKey":true}',
        paths: {},
        flows: [],
        status: 'PUBLISHED',
      },
      {
        id: '45ff00ef-8256-3218-bf0d-b289735d84bb',
        name: 'Free Spaceshuttle',
        security: 'KEY_LESS',
        securityDefinition: '{}',
        paths: {},
        flows: [],
        status: 'PUBLISHED',
      },
    ],
    visibility: 'PUBLIC',
    name: '🪐 Planets',
    services: {
      'health-check': {
        enabled: true,
        schedule: '0 */1 * * * *',
        steps: [
          {
            name: 'default-step',
            request: { path: '/', method: 'GET', fromRoot: true },
            response: { assertions: ['#response.status == 200'] },
          },
        ],
      },
    },
    properties: [],
    tags: [],
    picture_url:
      'https://master-apim-api.cloud.gravitee.io/management/organizations/DEFAULT/environments/DEFAULT/apis/aee23b1e-34b1-4551-a23b-1e34b165516a/picture?hash=1642675655553',
    background_url:
      'https://master-apim-api.cloud.gravitee.io/management/organizations/DEFAULT/environments/DEFAULT/apis/aee23b1e-34b1-4551-a23b-1e34b165516a/background?hash=1642675655553',
    resources: [
      {
        name: 'my-cache',
        type: 'cache',
        enabled: true,
        configuration: { timeToIdleSeconds: 60, timeToLiveSeconds: 60, maxEntriesLocalHeap: 1000 },
      },
    ],
    groups: ['f1194262-9157-4986-9942-629157f98682'],
    labels: [],
    path_mappings: ['/product/:id'],
    response_templates: {
      testhhh: { '*/*': { status: 400, body: '' } },
      DEFAULT: { '*/*': { status: 400, body: '' }, test: { status: 400, body: '' } },
    },
    lifecycle_state: 'PUBLISHED',
    disable_membership_notifications: false,
    flow_mode: 'DEFAULT',
    gravitee: '2.0.0',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
