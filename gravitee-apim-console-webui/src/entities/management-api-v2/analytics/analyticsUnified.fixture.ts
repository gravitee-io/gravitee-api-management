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
import { AnalyticsCount, AnalyticsDateHisto, AnalyticsGroupBy, AnalyticsStats } from './analyticsUnified';

export const fakeAnalyticsCount = (modifier?: Partial<AnalyticsCount>): AnalyticsCount => ({
  type: 'COUNT',
  count: 0,
  ...modifier,
});

export const fakeAnalyticsStats = (modifier?: Partial<AnalyticsStats>): AnalyticsStats => ({
  type: 'STATS',
  count: 0,
  min: null,
  max: null,
  avg: null,
  sum: null,
  ...modifier,
});

export const fakeAnalyticsGroupBy = (modifier?: Partial<AnalyticsGroupBy>): AnalyticsGroupBy => ({
  type: 'GROUP_BY',
  values: {},
  metadata: {},
  ...modifier,
});

export const fakeAnalyticsDateHisto = (modifier?: Partial<AnalyticsDateHisto>): AnalyticsDateHisto => ({
  type: 'DATE_HISTO',
  timestamp: [],
  values: [],
  ...modifier,
});
