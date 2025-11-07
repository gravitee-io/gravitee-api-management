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

export type WidgetType = PieType | 'stats' | 'top';

export type MeasuresType = 'AVG' | 'MIN' | 'MAX' | 'P50' | 'P90' | 'P95' | 'P99' | 'COUNT' | 'RPS';

export interface Widget2 {
  id: string;
  label: string;
  type: WidgetType;
  filter?: string;
  layout: GridsterItem;
}

export interface Widget {
  id: string;
  title: string;
  type: WidgetType;
  layout: WidgetLayout;

  request?: MetricsRequest;
  data?: MetricsResponse;
}

export interface MetricsResponse {
  interval?: string;
  metrics?: Metric[]; //remove ? after remove mock
}

export interface Metric {
  name: string;
  buckets?: Bucket[]; //Used by Facets and time-series
  measures?: Measure[]; // Used by KPI/STATS
  status?: 'pending' | 'success' | 'failed';
}

export interface Bucket {
  key: string;
  timestamp?: Date; //used by time-series
  measures?: Measure[];
  buckets?: Bucket[];
}
export interface Measure {
  name: string;
  value: number;
}

export interface WidgetLayout {
  cols: number;
  rows: number;
  x: number;
  y: number;
}

//Metrics

export interface MetricsRequest {
  type: 'measures' | 'facets' | 'time-series';
  by?: string[];
  timeRange: TimeRange;
  filters?: Filter[];
  metrics: WidgetMetric[];
}

export interface WidgetMetric {
  name: string; //Change by type ?
  filters?: Filter[];
  measures?: MeasuresType[];
  by?: string[];
  order?: Order;
  limit?: number;
}

export interface Order {
  measure: string;
  direction: 'ASC' | 'DESC';
}

export interface TimeRange {
  from: string;
  to: string;
}

export interface Filter {
  name: string;
  operator: 'EQ' | 'IN' | 'LTE' | 'GTE';
  value: string;
}
