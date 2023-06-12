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

import { Subscription } from './subscription';

export function fakeSubscription(modifier?: Partial<Subscription> | ((baseApi: Subscription) => Subscription)): Subscription {
  const base: Subscription = {
    id: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
    api: {
      id: 'bee23b1e-34b1-4551-a23b-1e34b165516a',
      name: 'My API',
      apiVersion: 'v1.2',
    },
    application: {
      id: 'cee23b1e-34b1-4551-a23b-1e34b165516a',
      name: 'My Application',
    },
    plan: {
      id: 'dee23b1e-34b1-4551-a23b-1e34b165516a',
      name: 'My Plan',
    },
    consumerMessage: 'My consumer message',
    publisherMessage: 'My publisher message',
    metadata: {
      key1: 'value1',
      key2: 'value2',
    },
    daysToExpirationOnLastNotification: 10,
    consumerConfiguration: {
      entrypointId: 'eee23b1e-34b1-4551-a23b-1e34b165516a',
    },
    failureCause: 'My failure cause',
    status: 'ACCEPTED',
    consumerStatus: undefined,
    processedBy: {
      id: 'fee23b1e-34b1-4551-a23b-1e34b165516a',
      displayName: 'My publisher',
    },
    subscribedBy: {
      id: 'gee23b1e-34b1-4551-a23b-1e34b165516a',
      displayName: 'My subscriber',
    },
    processedAt: new Date('2020-01-01T00:00:00.000Z'),
    startingAt: new Date('2020-01-01T00:00:00.000Z'),
    endingAt: undefined,
    commentMessage: undefined,
    commentRequired: false,
    createdAt: new Date('2020-01-01T00:00:00.000Z'),
    updatedAt: new Date('2020-01-01T00:00:00.000Z'),
    closedAt: undefined,
    pausedAt: undefined,
    consumerPausedAt: undefined,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
