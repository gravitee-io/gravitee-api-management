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
import { utc } from 'moment/moment';

import { TimeRangeParams } from '../../../../../../shared/utils/timeFrameRanges';

const NB_VALUES_BY_BUCKET = 30;

const V4_DURATION_MS: Record<string, number> = {
  '5m': 5 * 60 * 1000,
  '1h': 60 * 60 * 1000,
  '24h': 24 * 60 * 60 * 1000,
  '7d': 7 * 24 * 60 * 60 * 1000,
  '30d': 30 * 24 * 60 * 60 * 1000,
};

export const buildV4TimeRangeParams = (id: string): TimeRangeParams => {
  const duration = V4_DURATION_MS[id];
  const nowUtc = utc().valueOf();
  return {
    id,
    from: nowUtc - duration,
    to: nowUtc,
    interval: duration / NB_VALUES_BY_BUCKET,
  };
};

export interface V4AnalyticsTimeFrame {
  label: string;
  id: string;
  timeFrameRangesParams: () => TimeRangeParams;
}

export const v4AnalyticsTimeFrames: V4AnalyticsTimeFrame[] = [
  { label: 'Last 5 minutes', id: '5m', timeFrameRangesParams: () => buildV4TimeRangeParams('5m') },
  { label: 'Last 1 hour', id: '1h', timeFrameRangesParams: () => buildV4TimeRangeParams('1h') },
  { label: 'Last 24 hours', id: '24h', timeFrameRangesParams: () => buildV4TimeRangeParams('24h') },
  { label: 'Last 7 days', id: '7d', timeFrameRangesParams: () => buildV4TimeRangeParams('7d') },
  { label: 'Last 30 days', id: '30d', timeFrameRangesParams: () => buildV4TimeRangeParams('30d') },
];

export interface ApiAnalyticsFilters {
  period: string;
  from: number | null;
  to: number | null;
}
