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

import { AnalyticsAverageResponseTimes, AnalyticsV4ResponseTimes, AnalyticsV4ResponseStatus } from './analytics';

export const fakeAnalyticsResponseTime = (modifier?: Partial<AnalyticsAverageResponseTimes>): AnalyticsAverageResponseTimes => {
  const base: AnalyticsAverageResponseTimes = {
    timestamp: {
      from: 1738919066000,
      to: 1738919128000,
      interval: 2000,
    },
    values: [
      {
        buckets: [
          {
            name: 'avg_response-time',
            data: [18, 65, 53, 0, 23, 19, 18, 30, 20, 29, 19, 0, 36, 20, 26, 0, 18, 28, 18, 29, 20, 41, 36, 0, 18, 0, 0, 0, 0, 0, 0, 0],
          },
        ],
        field: 'response-time',
        name: 'avg_response-time',
      },
      {
        buckets: [
          {
            name: 'avg_api-response-time',
            data: [18, 65, 53, 0, 23, 19, 18, 29, 20, 29, 18, 0, 35, 20, 26, 0, 18, 28, 18, 29, 20, 41, 36, 0, 18, 0, 0, 0, 0, 0, 0, 0],
          },
        ],
        field: 'api-response-time',
        name: 'avg_api-response-time',
      },
    ],
  };

  return {
    ...base,
    ...modifier,
  };
};

export const fakeV4AnalyticsResponseStatus = (modifier?: Partial<AnalyticsV4ResponseStatus>): AnalyticsV4ResponseStatus => {
  const base: AnalyticsV4ResponseStatus = {
    timeRange: {
      from: 1736330945180,
      to: 1738922945180,
      interval: 21600000,
    },
    data: { '500': [1, 4, 5], '400': [1, 2, 3] },
  };

  return {
    ...base,
    ...modifier,
  };
};

export const fakeV4AnalyticsResponseTime = (modifier?: Partial<AnalyticsV4ResponseTimes>): AnalyticsV4ResponseTimes => {
  const base: AnalyticsV4ResponseTimes = {
    timeRange: {
      from: 1736330945180,
      to: 1738922945180,
      interval: 21600000,
    },
    data: [
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12454694, 60528591, 0, 0, 2643597, 0, 0,
      0, 7422900, 1864090, 0, 0, 0, 1508851, 0, 0, 5169, 164, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0,
    ],
  };

  return {
    ...base,
    ...modifier,
  };
};
