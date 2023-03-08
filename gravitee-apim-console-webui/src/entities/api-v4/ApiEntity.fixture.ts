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

import { ApiEntity } from './ApiEntity';

export function fakeApiEntity(modifier?: Partial<ApiEntity> | ((baseApi: ApiEntity) => ApiEntity)): ApiEntity {
  const base: ApiEntity = {
    id: 'e2e86611-5af9-45fb-a866-115af925fb98',
    name: 'Event Consumption - SSE',
    apiVersion: '1.0',
    definitionVersion: '4.0.0',
    type: 'message',
    createdAt: '1674228285055',
    updatedAt: '1674228285055',
    description: 'Event Consumption - SSE',
    listeners: [
      {
        type: 'http',
        paths: [
          {
            path: '/demo/sse/kafka',
          },
        ],
        entrypoints: [
          {
            type: 'sse',
            qos: 'auto',
            configuration: { metadataAsComment: false, heartbeatIntervalInMs: 5000, headersAsComment: false },
          },
        ],
      },
    ],
    endpointGroups: [
      {
        name: 'default-group',
        type: 'kafka',
        loadBalancer: {
          type: 'round-robin',
        },
        endpoints: [
          {
            name: 'default',
            type: 'kafka',
            secondary: false,
            weight: 1,
            inheritConfiguration: false,
            configuration: {
              bootstrapServers: 'kafka:9092',
              topics: ['demo'],
              producer: { enabled: false },
              consumer: { encodeMessageId: true, enabled: true, autoOffsetReset: 'earliest' },
            },
          },
        ],
      },
    ],
    plans: [],
    flowMode: 'default',
    flows: [],
    groups: [],
    visibility: 'PRIVATE',
    state: 'STOPPED',
    primaryOwner: {
      id: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
      email: 'no-reply@graviteesource.com',
      displayName: 'Admin master',
      type: 'USER',
    },
    lifecycleState: 'CREATED',
    disableMembershipNotifications: false,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
