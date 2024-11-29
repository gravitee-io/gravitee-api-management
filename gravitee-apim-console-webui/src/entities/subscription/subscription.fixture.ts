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

import { Subscription, SubscriptionPage } from './subscription';

import { fakeUser } from '../user/user.fixture';
import { ApiKeyMode } from '../application/Application';

export function fakeSubscriptionPage(
  modifier?: Partial<SubscriptionPage> | ((baseApi: SubscriptionPage) => SubscriptionPage),
): SubscriptionPage {
  const date = new Date();
  const base: SubscriptionPage = {
    id: '45ff00ef-8256-3218-bf0d-b289735d84bb',
    api: 'api-id',
    plan: 'plan-id',
    application: 'application-id',
    status: 'ACCEPTED',
    processed_at: date,
    processed_by: 'me',
    subscribed_by: fakeUser(),
    starting_at: date,
    created_at: date,
    updated_at: date,
    client_id: 'client_id',
    origin: 'MANAGEMENT',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeSubscription(modifier?: Partial<Subscription> | ((baseApi: Subscription) => Subscription)): Subscription {
  const date = new Date();

  const base: Subscription = {
    id: '45ff00ef-8256-3218-bf0d-b289735d84bb',
    api: {
      id: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
      name: '\uD83E\uDE90 Planets',
      version: '1.0',
      definitionVersion: 'V2',
      owner: {
        id: 'user-id',
        displayName: 'John Doe',
      },
    },
    plan: {
      id: '45ff00ef-8256-3218-bf0d-b289735d84bb',
      name: 'Free Spaceshuttle',
      security: 'KEY_LESS',
    },
    application: {
      apiKeyMode: ApiKeyMode.UNSPECIFIED,
      description: 'My default application',
      id: '61840ad7-7a93-4b5b-840a-d77a937b5bff',
      name: 'Default application',
      type: 'SIMPLE',
      domain: 'http://example.com',
      owner: {
        id: 'user-id',
        displayName: 'John Doe',
      },
    },
    status: 'ACCEPTED',
    processed_at: date,
    processed_by: 'me',
    subscribed_by: fakeUser(),
    starting_at: date,
    created_at: date,
    updated_at: date,
    client_id: 'client_id',
    origin: 'MANAGEMENT',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
