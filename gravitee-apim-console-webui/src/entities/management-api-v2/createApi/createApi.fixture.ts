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

import { CreateApiV4 } from './createApiV4';
import { CreateBaseApi } from './createBaseApi';

export function fakeCreateBaseApi(modifier?: Partial<CreateBaseApi> | ((baseApi: CreateBaseApi) => CreateBaseApi)): CreateBaseApi {
  const base: CreateBaseApi = {
    name: '\uD83E\uDE90 Planets',
    description: 'The whole universe in your hand.',
    apiVersion: '1.0',
    definitionVersion: 'V2',
    groups: ['f1194262-9157-4986-9942-629157f98682'],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeCreateApiV4(modifier?: Partial<CreateApiV4> | ((baseApi: CreateApiV4) => CreateApiV4)): CreateApiV4 {
  const base: CreateApiV4 = {
    ...fakeCreateBaseApi({ ...modifier, definitionVersion: 'V4' }),
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
