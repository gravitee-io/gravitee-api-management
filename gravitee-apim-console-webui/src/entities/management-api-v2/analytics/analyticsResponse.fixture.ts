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
import { CountResponse, DateHistoResponse, GroupByResponse, StatsResponse } from './analyticsResponse';

export const fakeCountResponse = (modifier?: Partial<CountResponse>): CountResponse => ({
  type: 'COUNT',
  count: 123,
  ...modifier,
});

export const fakeStatsResponse = (modifier?: Partial<StatsResponse>): StatsResponse => ({
  type: 'STATS',
  count: 50,
  min: 10,
  max: 500,
  avg: 120,
  sum: 6000,
  ...modifier,
});

export const fakeGroupByResponse = (modifier?: Partial<GroupByResponse>): GroupByResponse => ({
  type: 'GROUP_BY',
  values: { '200': 80, '404': 15, '500': 5 },
  metadata: { '200': { name: '200' }, '404': { name: '404' }, '500': { name: '500' } },
  ...modifier,
});

export const fakeDateHistoResponse = (modifier?: Partial<DateHistoResponse>): DateHistoResponse => ({
  type: 'DATE_HISTO',
  timestamp: [1000000, 2000000, 3000000],
  values: [
    { field: '200', buckets: [10, 20, 30], metadata: { name: '200' } },
    { field: '500', buckets: [1, 0, 2], metadata: { name: '500' } },
  ],
  ...modifier,
});
