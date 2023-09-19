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
import { isFunction, sample } from 'lodash';
import { uuid } from '@gravitee/ui-components/src/lib/utils';

import { MessageLog } from './messageLog';

export function fakeMessageLog(modifier?: Partial<MessageLog> | ((base: MessageLog) => MessageLog)): MessageLog {
  const base: MessageLog = {
    requestId: uuid(),
    timestamp: Date.now().toString(),
    clientIdentifier: uuid(),
    connectorId: sample(['mock', 'mqtt5', 'kafka', 'solace', 'rabbitmq']),
    connectorType: sample(['ENDPOINT', 'ENTRYPOINT']),
    correlationId: uuid(),
    parentCorrelationId: uuid(),
    operation: sample(['SUBSCRIBE', 'PUBLISH']),
    message: {
      id: '0',
      headers: {
        'X-Mock': ['value'],
      },
      metadata: {
        MessageMetadata: 'value',
      },
      payload: 'mock message',
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
