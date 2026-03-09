/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { isFunction } from 'rxjs/internal/util/isFunction';

import { SubscriptionConsumerConfiguration } from './subscription-consumer-configuration';

export function fakeSubscriptionConsumerConfiguration(
  modifier?:
    | Partial<SubscriptionConsumerConfiguration>
    | ((baseSubscriptionConsumerConfiguration: SubscriptionConsumerConfiguration) => SubscriptionConsumerConfiguration),
): SubscriptionConsumerConfiguration {
  const base: SubscriptionConsumerConfiguration = {
    entrypointId: 'webhook',
    channel: 'test',
    entrypointConfiguration: {
      callbackUrl: 'https://webhook.example/1234',
      headers: [
        {
          name: 'Content-Type',
          value: 'application/json',
        },
        {
          name: 'X-Custom-Key',
          value: '1234',
        },
      ],
      auth: {
        type: 'none',
      },
      ssl: {
        keyStore: {
          type: '',
        },
        hostnameVerifier: false,
        trustStore: {
          type: '',
        },
        trustAll: true,
      },
      retry: {
        retryOption: 'No Retry',
      },
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
