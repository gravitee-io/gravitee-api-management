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

import { PlatformLogsResponse } from './platformLogs';

export function fakePlatformLogsResponse(attributes?: Partial<PlatformLogsResponse>): PlatformLogsResponse {
  const base: PlatformLogsResponse = {
    logs: [
      {
        id: 'test',
        timestamp: 10000000,
        transactionId: 'test',
        path: 'test',
        method: 'test',
        status: 1,
        responseTime: 3132123131,
        api: 'test',
        plan: 'test',
        application: 'test',
        endpoint: false,
      },
    ],
    total: 1,
  };

  return {
    ...base,
    ...attributes,
  };
}
