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

import { AnalyticsStatsResponse } from './analyticsStats';

export const fakeAnalyticsStatsResponse = (modifier?: Partial<AnalyticsStatsResponse>): AnalyticsStatsResponse => {
  const base: AnalyticsStatsResponse = {
    count: 100,
    min: 11,
    max: 12,
    avg: 13,
    sum: 14,
    rps: 15,
    rpm: 16,
    rph: 17,
  };

  return {
    ...base,
    ...modifier,
  };
};
