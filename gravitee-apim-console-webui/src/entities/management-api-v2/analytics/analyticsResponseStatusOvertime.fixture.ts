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

import { AnalyticsResponseStatusOvertime } from './analyticsResponseStatusOvertime';

export const fakeAnalyticsResponseStatusOvertime = (
  modifier?: Partial<AnalyticsResponseStatusOvertime>,
): AnalyticsResponseStatusOvertime => {
  const base: AnalyticsResponseStatusOvertime = {
    timeRange: {
      from: 0,
      to: 0,
      interval: 0,
    },
    data: {
      one: [
        16, 13, 16, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13,
        16, 10, 2, 10, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13,
        16, 13, 16, 13, 16, 10, 7, 30, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 16, 13,
        16, 13, 16, 13, 16, 13, 16, 13, 16, 10, 7, 45, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16,
        13, 16, 13, 16, 13, 16, 13, 16, 10, 70, 20, 16, 13, 16, 13, 16,
      ],
    },
  };

  return {
    ...base,
    ...modifier,
  };
};
