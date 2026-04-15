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
 * Discriminated-union response for the unified analytics endpoint.
 * Mirrors the OpenAPI response schemas added in US-01/03/04.
 * The `type` discriminator matches the `AnalyticsQueryParams.type` used to request the data.
 */

export interface CountResponse {
  type: 'COUNT';
  count: number;
}

export interface StatsResponse {
  type: 'STATS';
  count: number;
  min: number;
  max: number;
  avg: number;
  sum: number;
}

export interface GroupByValue {
  name: string;
}

export interface GroupByResponse {
  type: 'GROUP_BY';
  /** Bucket key → count. */
  values: Record<string, number>;
  /** Human-readable label for each bucket key. */
  metadata: Record<string, GroupByValue>;
}

export interface DateHistoSeries {
  field: string;
  /** Count per timestamp bucket, aligned with the top-level `timestamp` array. */
  buckets: number[];
  metadata: { name: string };
}

export interface DateHistoResponse {
  type: 'DATE_HISTO';
  /** Epoch-millisecond bucket start times. */
  timestamp: number[];
  values: DateHistoSeries[];
}

export type AnalyticsResponse = CountResponse | StatsResponse | GroupByResponse | DateHistoResponse;
