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

import { NewApiEntity } from './NewApiEntity';

export function fakeNewApiEntity(modifier?: Partial<NewApiEntity> | ((baseApi: NewApiEntity) => NewApiEntity)): NewApiEntity {
  const base: NewApiEntity = {
    name: 'Event Consumption - SSE',
    apiVersion: '1.0',
    definitionVersion: '4.0.0',
    type: 'message',
    description: 'Event Consumption - SSE',
    listeners: [
      {
        type: 'http' as const,
        paths: [
          {
            path: '/demo/sse/kafka',
          },
        ],
        entrypoints: [
          {
            type: 'sse',
            configuration: {
              heartbeatIntervalInMs: 5000,
              metadataAsComment: false,
              headersAsComment: false,
            },
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
              bootstrapServers: 'kafka:9092',
              topics: ['demo'],
              consumer: {
                autoOffsetReset: 'earliest',
              },
              producer: {
                enabled: false,
              },
            },
          },
        ],
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
