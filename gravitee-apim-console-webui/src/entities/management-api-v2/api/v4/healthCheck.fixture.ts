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

import { ApiAvailability, ApiAverageResponseTime, ApiHealthResponseTimeOvertime } from './healthCheck';

export function fakeApiHealthResponseTimeOvertime(attribute?: Partial<ApiHealthResponseTimeOvertime>): ApiHealthResponseTimeOvertime {
  const base: ApiHealthResponseTimeOvertime = {
    timeRange: {
      from: 1,
      to: 100,
      interval: 10,
    },
    data: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
  };

  return {
    ...base,
    ...attribute,
  };
}

export function fakeApiHealthAvailability(attribute?: Partial<ApiAvailability>): ApiAvailability {
  const base: ApiAvailability = {
    global: 0.9876,
    group: {
      example: 100,
    },
  };

  return {
    ...base,
    ...attribute,
  };
}

export function fakeApiHealthAverageResponseTime(attribute?: Partial<ApiAverageResponseTime>): ApiAverageResponseTime {
  const base: ApiAverageResponseTime = {
    global: 100,
    group: {
      example: 100,
    },
  };

  return {
    ...base,
    ...attribute,
  };
}
