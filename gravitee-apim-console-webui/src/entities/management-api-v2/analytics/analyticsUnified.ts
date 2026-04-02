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

/** Query type discriminator — mirrors the backend AnalyticsType enum. */
export type AnalyticsQueryType = 'COUNT' | 'STATS' | 'GROUP_BY' | 'DATE_HISTO';

// ---------------------------------------------------------------------------
// Response shapes — one interface per type, matching the backend JSON contract
// ---------------------------------------------------------------------------

export interface AnalyticsCount {
  type: 'COUNT';
  count: number;
}

export interface AnalyticsStats {
  type: 'STATS';
  count: number;
  min: number | null;
  max: number | null;
  avg: number | null;
  sum: number | null;
}

export interface AnalyticsGroupBy {
  type: 'GROUP_BY';
  values: Record<string, number>;
  metadata: Record<string, Record<string, string>>;
}

export interface AnalyticsDateHistoBucket {
  field: string;
  buckets: number[];
  metadata: Record<string, string>;
}

export interface AnalyticsDateHisto {
  type: 'DATE_HISTO';
  timestamp: number[];
  values: AnalyticsDateHistoBucket[];
}

/** Union of all possible unified analytics responses. */
export type AnalyticsResponse = AnalyticsCount | AnalyticsStats | AnalyticsGroupBy | AnalyticsDateHisto;

// ---------------------------------------------------------------------------
// Query params — passed to getAnalytics()
// ---------------------------------------------------------------------------

export interface AnalyticsQueryParams {
  type: AnalyticsQueryType;
  from: number;
  to: number;
  field?: string;
  interval?: number;
  size?: number;
}
