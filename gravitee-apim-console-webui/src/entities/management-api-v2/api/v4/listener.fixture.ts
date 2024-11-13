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

import { HttpListener } from './httpListener';
import { SubscriptionListener } from './subscriptionListener';
import { KafkaListener } from './kafkaListener';

export function fakeHttpListener(modifier?: Partial<HttpListener> | ((baseListener: HttpListener) => HttpListener)): HttpListener {
  const base: HttpListener = {
    type: 'HTTP',
    paths: [{ path: '/context-path' }],
    entrypoints: [
      {
        type: 'http-get',
        configuration: {
          messagesLimitCount: 111,
          headersInPayload: false,
          metadataInPayload: false,
          messagesLimitDurationMs: 5000,
        },
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

export function fakeSubscriptionListener(
  modifier?: Partial<SubscriptionListener> | ((baseListener: SubscriptionListener) => SubscriptionListener),
): SubscriptionListener {
  const base: SubscriptionListener = {
    type: 'SUBSCRIPTION',
    entrypoints: [
      {
        type: 'webhook',
        qos: 'AUTO',
        configuration: {
          proxy: {
            useSystemProxy: false,
            enabled: false,
          },
          http: {
            readTimeout: 10000,
            idleTimeout: 60000,
            connectTimeout: 3000,
            maxConcurrentConnections: 5,
          },
        },
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
export function fakeKafkaListener(modifier?: Partial<KafkaListener> | ((baseListener: KafkaListener) => KafkaListener)): KafkaListener {
  const base: KafkaListener = {
    type: 'KAFKA',
    host: 'kafka-host',
    port: 1000,
    entrypoints: [
      {
        type: 'native-kafka',
        configuration: {},
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
