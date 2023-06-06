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

import { UpdateBaseApi } from './updateBaseApi';
import { UpdateApiV4 } from './updateApiV4';

export function fakeUpdateBaseApi(modifier?: Partial<UpdateBaseApi> | ((base: UpdateBaseApi) => UpdateBaseApi)): UpdateBaseApi {
  const base: UpdateBaseApi = {
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

export function fakeUpdateApiV4(modifier?: Partial<UpdateApiV4> | ((base: UpdateApiV4) => UpdateApiV4)): UpdateApiV4 {
  const base: UpdateApiV4 = {
    ...fakeUpdateBaseApi({ ...modifier }),
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
