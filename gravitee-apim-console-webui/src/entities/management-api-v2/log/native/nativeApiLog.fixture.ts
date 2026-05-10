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

import { NativeApiLog } from './nativeApiLog';

export function fakeNativeApiLog(modifier?: Partial<NativeApiLog> | ((base: NativeApiLog) => NativeApiLog)): NativeApiLog {
  const base: NativeApiLog = {
    timestamp: '2026-01-01T00:00:00.000Z',
    apiId: '117e79a3-6023-4b72-be79-a36023ab72f9',
    requestId: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
    transactionId: 'tx-aee23b1e-34b1-4551-a23b-1e34b165516a',
    applicationId: 'app-fc1bff13-6e25-4c4c-9bff-136e251c4cdf',
    planId: 'plan-a30f6fab-be08-4717-8f6f-abbe089717b1',
    clientIdentifier: 'kafka-client-1',
    subscriptionId: 'sub-b94e0b4a-a92b-46ba-8e0b-4aa92b06ba7d',
    entrypointId: 'native-kafka',
    gateway: 'gateway-instance-1',
    remoteAddress: '10.0.0.1',
    localAddress: '10.0.0.2',
    host: 'kafka.example.com',
    connectionStatus: 'CONNECTED',
    connectionDurationMs: 4200,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
