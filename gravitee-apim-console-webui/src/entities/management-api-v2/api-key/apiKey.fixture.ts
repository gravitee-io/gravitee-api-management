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

import { ApiKey } from './apiKey';

import { fakeBaseSubscription } from '../subscription';
import { fakeBaseApplication } from '../application';

export function fakeApiKey(modifier?: Partial<ApiKey> | ((baseApiKey: ApiKey) => ApiKey)): ApiKey {
  const base: ApiKey = {
    id: '9a3825da-64a8-43d4-b825-da64a8e3d42e',
    key: '49765a30-659b-4284-b65a-30659be28431',
    createdAt: new Date('2020-01-01T00:00:00.000Z'),
    updatedAt: new Date('2020-01-02T00:00:00.000Z'),
    expireAt: new Date('2022-01-01T00:00:00.000Z'),
    expired: false,
    paused: false,
    revoked: false,
    revokedAt: undefined,
    application: fakeBaseApplication(),
    daysToExpirationOnLastNotification: undefined,
    subscriptions: [fakeBaseSubscription()],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
