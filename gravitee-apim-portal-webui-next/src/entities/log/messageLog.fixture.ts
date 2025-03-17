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

import { AggregatedMessageLog, Message, AggregatedMessageLogsResponse } from './messageLog';

export function fakeMessage(modifier?: Partial<Message> | ((base: Message) => Message)): Message {
  const base: Message = {
    connectorId: 'mock',
    timestamp: '123',
    id: 'mock-connector',
    payload: 'a great message',
    isError: false,
    headers: {
      fake: ['header'],
    },
    metadata: {
      fake: 'metadata',
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

export function fakeAggregatedMessageLog(
  modifier?: Partial<AggregatedMessageLog> | ((base: AggregatedMessageLog) => AggregatedMessageLog),
): AggregatedMessageLog {
  const base: AggregatedMessageLog = {
    requestId: 'b62f6e82-c39b-4edb-af6e-82c39b9edb46',
    clientIdentifier: 'client-id',
    correlationId: 'correlation-id',
    parentCorrelationId: 'parent-correlation-id',
    operation: 'SUBSCRIBE',
    timestamp: '123',
    entrypoint: fakeMessage(),
    endpoint: fakeMessage(),
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeAggregatedMessageLogsResponse(
  modifier?: Partial<AggregatedMessageLogsResponse> | ((base: AggregatedMessageLogsResponse) => AggregatedMessageLogsResponse),
): AggregatedMessageLogsResponse {
  const base: AggregatedMessageLogsResponse = {
    data: [fakeAggregatedMessageLog()],
    metadata: {
      data: { total: 1 },
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
