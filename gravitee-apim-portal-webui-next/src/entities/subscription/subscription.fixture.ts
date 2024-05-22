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

import { Subscription, SubscriptionData } from './subscription';

export function fakeSubscription(
  modifier?: Partial<SubscriptionData> | ((baseSubscription: SubscriptionData) => SubscriptionData),
): SubscriptionData {
  const base: SubscriptionData = {
    id: '5ac5ca94-160f-4acd-85ca-94160fcacd7d',
    application: '99c6cbe6-eead-414d-86cb-e6eeadc14db3',
    plan: 'b6f88a31-777f-45aa-b88a-31777fa5aace',
    request: '',
    reason: 'Subscription has been closed.',
    created_at: '2024-04-17T10:33:29Z',
    closed_at: '2024-04-17T10:34:05.598Z',
    subscribed_by: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
    status: 'REJECTED',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeSubscriptionResponse(
  modifier?: Partial<Subscription> | ((baseSubscription: Subscription) => Subscription),
): Subscription {
  const base: Subscription = {
    data: [fakeSubscription()],
    links: {
      self: 'test',
    },
    metadata: {
      'b6f88a31-777f-45aa-b88a-31777fa5aace': {
        name: 'testPLan',
      },
      '99c6cbe6-eead-414d-86cb-e6eeadc14db3': {
        name: 'testApplication',
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
