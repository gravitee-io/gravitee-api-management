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

/**
 * Discriminated-union query params for the unified analytics endpoint.
 * TypeScript narrows to the correct shape based on `type`, so callers cannot
 * accidentally omit `field` on STATS or `interval` on DATE_HISTO.
 *
 * Maps 1-to-1 with `AnalyticsType` on the backend (US-01).
 */

export interface CountQueryParams {
  type: 'COUNT';
  from: number;
  to: number;
}

export interface StatsQueryParams {
  type: 'STATS';
  from: number;
  to: number;
  /** Required — one of the allowed STATS fields (e.g. gateway-response-time-ms). */
  field: string;
}

export interface GroupByQueryParams {
  type: 'GROUP_BY';
  from: number;
  to: number;
  /** Required — field to group by (e.g. status). */
  field: string;
  /** Max number of buckets. Default 10, max 100. */
  size?: number;
  /** Bucket sort order. Default DESC. */
  order?: 'ASC' | 'DESC';
}

export interface DateHistoQueryParams {
  type: 'DATE_HISTO';
  from: number;
  to: number;
  /** Required — field to aggregate over time (e.g. status). */
  field: string;
  /** Required — bucket size in epoch milliseconds. */
  interval: number;
}

export type AnalyticsQueryParams = CountQueryParams | StatsQueryParams | GroupByQueryParams | DateHistoQueryParams;
