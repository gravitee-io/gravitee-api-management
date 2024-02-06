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

import { Event } from './event';

import { PagedResult } from '../pagedResult';

export function fakeEvent(modifier?: Partial<Event> | ((base: Event) => Event)): Event {
  const base: Event = {
    id: 'audit-id',
    type: 'PUBLISH_API',
    createdAt: new Date('2021-01-01T00:00:00Z'),
    environmentIds: ['env-id'],
    initiator: {
      id: 'user-id',
      displayName: 'John Doe',
    },
    properties: {
      API: 'api-id',
      USER: 'user-id',
    },
    payload: '{}',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeEventsResponse(
  modifier?: Partial<PagedResult<Event>> | ((base: PagedResult<Event>) => PagedResult<Event>),
): PagedResult<Event> {
  const base: any = {
    data: [fakeEvent()],
    pagination: {
      totalCount: 1,
      page: 1,
      pageCount: 1,
      pageItemsCount: 1,
      perPage: 10,
    },
    links: { self: 'self' },
  };

  const pageResult = new PagedResult();
  if (isFunction(modifier)) {
    pageResult.populate(modifier(base));
    return pageResult;
  }

  pageResult.populate({
    ...base,
    ...modifier,
  });
  return pageResult;
}
