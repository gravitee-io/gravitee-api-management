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

import { Log, LogListItem, LogsResponse } from './log';

export function fakeLogListItem(modifier?: Partial<LogListItem> | ((baseApplication: LogListItem) => LogListItem)): LogListItem {
  const base: LogListItem = {
    id: 'b62f6e82-c39b-4edb-af6e-82c39b9edb46',
    api: 'my-api',
    plan: 'my-plan',
    path: '/my-path',
    status: 200,
    metadata: {},
    method: 'GET',
    timestamp: 123,
    responseTime: 23,
    transactionId: 'transaction-123',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
export function fakeLogsResponse(modifier?: Partial<LogsResponse> | ((baseApplication: LogsResponse) => LogsResponse)): LogsResponse {
  const base: LogsResponse = {
    data: [fakeLogListItem()],
    metadata: {
      'my-plan': { name: 'plan' },
      'my-api': { name: 'my api', version: '1' },
      data: { total: 1 },
    },
    links: {},
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeLog(modifier?: Partial<Log> | ((baseApplication: Log) => Log)): Log {
  const base: Log = {
    ...fakeLogListItem(),
    metadata: {},
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
