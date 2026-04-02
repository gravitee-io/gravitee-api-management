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
  /** null when no documents matched (backend returns null, not 0) */
  min: number | null;
  max: number | null;
  /** Primary metric displayed in the Traffic Overview cards */
  avg: number | null;
  sum: number | null;
}

export interface AnalyticsGroupBy {
  type: 'GROUP_BY';
  /** Keyed by the field value (e.g. HTTP status code "200"), value is document count */
  values: Record<string, number>;
  /** Optional display metadata per field value (e.g. human-readable label) */
  metadata: Record<string, Record<string, string>>;
}

export interface AnalyticsDateHistoBucket {
  /** The distinct field value this series represents (e.g. "200", "404") */
  field: string;
  /** Counts parallel-aligned to the parent {@link AnalyticsDateHisto.timestamp} array */
  buckets: number[];
  metadata: Record<string, string>;
}

export interface AnalyticsDateHisto {
  type: 'DATE_HISTO';
  /** Epoch-ms start of each time bucket, ascending */
  timestamp: number[];
  /** One entry per distinct field value; each Bucket.buckets array aligns with timestamp */
  values: AnalyticsDateHistoBucket[];
}

/** Union of all possible unified analytics responses. */
export type AnalyticsResponse = AnalyticsCount | AnalyticsStats | AnalyticsGroupBy | AnalyticsDateHisto;

// ---------------------------------------------------------------------------
// Query params — passed to getAnalytics()
// ---------------------------------------------------------------------------

export interface AnalyticsQueryParams {
  type: AnalyticsQueryType;
  /**
   * Epoch-ms start of the query window.
   * Optional — when omitted, `getAnalytics()` injects the value from the shared time-range filter.
   */
  from?: number;
  /**
   * Epoch-ms end of the query window.
   * Optional — when omitted, `getAnalytics()` injects the value from the shared time-range filter.
   */
  to?: number;
  /** Required for STATS, GROUP_BY, and DATE_HISTO; ignored for COUNT */
  field?: string;
  /** Bucket width in ms — required for DATE_HISTO */
  interval?: number;
  /** Top-N limit for GROUP_BY (backend default: 10) */
  size?: number;
}
