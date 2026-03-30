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

/** Matches Management API v2 `ApiUnifiedAnalyticsQueryType`. */
export const AnalyticsUnifiedQueryType = {
  COUNT: 'COUNT',
  STATS: 'STATS',
  GROUP_BY: 'GROUP_BY',
  DATE_HISTO: 'DATE_HISTO',
} as const;

export type AnalyticsUnifiedQueryType = (typeof AnalyticsUnifiedQueryType)[keyof typeof AnalyticsUnifiedQueryType];

/** Matches `ApiUnifiedAnalyticsGroupByOrderParam`. */
export type AnalyticsUnifiedGroupByOrder = 'COUNT_DESC' | 'COUNT_ASC';

/**
 * Query parameters for `GET .../apis/{apiId}/analytics` (excluding `from` / `to`, which come from
 * {@link ApiAnalyticsV2Service#setTimeRangeFilter}).
 */
export interface AnalyticsUnifiedRequestParams {
  type: AnalyticsUnifiedQueryType;
  /** PRD field name; required when `type` is STATS or GROUP_BY; optional for DATE_HISTO. */
  field?: string;
  /** Bucket width in milliseconds; required when `type` is DATE_HISTO. */
  interval?: number;
  /** Max buckets for GROUP_BY; defaults to 10 on the server when omitted. */
  size?: number;
  order?: AnalyticsUnifiedGroupByOrder;
}
