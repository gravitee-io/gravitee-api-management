/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { GridsterItem } from 'angular-gridster2';

import { PieType } from '../chart/pie-chart/pie-chart.component';

export type WidgetType = PieType | 'kpi' | 'top';

export interface Widget {
  id: string;
  label: string;
  type: WidgetType;
  filter?: string;
  layout: GridsterItem;
}

export type ApiAnalyticsDashboardWidgetConfig = WidgetDisplayConfig & WidgetDataConfig;

type WidgetDisplayConfig = {
  title: string;
  statsKey?: Stats;
  statsUnit?: StatsUnitType;
  tooltip: string;
  shouldSortBuckets?: boolean;
  type: ApiAnalyticsWidgetType;
  isClickable?: boolean;
  relativePath?: string;
  minHeight?: 'small' | 'medium' | 'large';
};

type WidgetDataConfig = {
  apiId: string;
  analyticsType: 'STATS' | 'GROUP_BY' | 'HISTOGRAM';
  aggregations?: AnalyticsHistogramAggregation[];
  groupByField?: GroupByField;
  statsField?: StatsField;
  ranges?: Range[];
  orderBy?: string;
  tableData?: {
    columns: WidgetDataConfigColumn[];
  };
};

export interface WidgetDataConfigColumn {
  label: string;
  dataType: 'string' | 'number';
}

interface Range {
  label: string;
  value: string;
  color?: string;
}

export interface AnalyticsStatsResponse {
  count: number;
  min: number;
  max: number;
  avg: number;
  sum: number;
  rps: number;
  rpm: number;
  rph: number;
}

export type StatsField =
  | 'response-time'
  | 'status'
  | 'gateway-latency-ms'
  | 'gateway-response-time-ms'
  | 'request-content-length'
  | 'endpoint-response-time-ms';

export type Stats = 'avg' | 'count' | 'min' | 'max' | 'rps';

// other file
export type GroupByField = 'host' | 'status' | 'application-id' | 'plan-id' | 'path-info.keyword';

//
export type StatsUnitType = 'ms';

//
export type ApiAnalyticsWidgetType = 'pie' | 'line' | 'bar' | 'table' | 'stats' | 'multi-stats';

//
export interface AnalyticsHistogramAggregation {
  type: AggregationTypes;
  field: AggregationFields;
  label?: string;
}
export enum AggregationTypes {
  MAX = 'MAX',
  MIN = 'MIN',
  AVG = 'AVG',
  FIELD = 'FIELD',
  VALUE = 'VALUE',
  DELTA = 'DELTA',
  TREND = 'TREND',
}

export enum AggregationFields {
  APPLICATION_ID = 'application-id',
  STATUS = 'status',
  GATEWAY_RESPONSE_TIME_MS = 'gateway-response-time-ms',
  ENDPOINT_RESPONSE_TIME_MS = 'endpoint-response-time-ms',
  DOWNSTREAM_ACTIVE_CONNECTIONS = 'downstream-active-connections',
  UPSTREAM_ACTIVE_CONNECTIONS = 'upstream-active-connections',
  DOWNSTREAM_AUTHENTICATION_FAILURES_TOTAL = 'downstream-authentication-failures-total',
  DOWNSTREAM_AUTHENTICATION_SUCCESSES_TOTAL = 'downstream-authentication-successes-total',
  UPSTREAM_AUTHENTICATION_FAILURES_TOTAL = 'upstream-authentication-failures-total',
  UPSTREAM_AUTHENTICATION_SUCCESSES_TOTAL = 'upstream-authentication-successes-total',
  DOWNSTREAM_PUBLISH_MESSAGES_TOTAL = 'downstream-publish-messages-total',
  UPSTREAM_PUBLISH_MESSAGES_TOTAL = 'upstream-publish-messages-total',
  DOWNSTREAM_PUBLISH_MESSAGE_BYTES = 'downstream-publish-message-bytes',
  UPSTREAM_PUBLISH_MESSAGE_BYTES = 'upstream-publish-message-bytes',
  UPSTREAM_SUBSCRIBE_MESSAGES_TOTAL = 'upstream-subscribe-messages-total',
  DOWNSTREAM_SUBSCRIBE_MESSAGES_TOTAL = 'downstream-subscribe-messages-total',
  UPSTREAM_SUBSCRIBE_MESSAGE_BYTES = 'upstream-subscribe-message-bytes',
  DOWNSTREAM_SUBSCRIBE_MESSAGE_BYTES = 'downstream-subscribe-message-bytes',
}
