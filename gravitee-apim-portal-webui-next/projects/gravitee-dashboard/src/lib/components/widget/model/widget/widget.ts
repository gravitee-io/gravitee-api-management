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

import { PieType } from '../../../chart/pie-chart/pie-chart.component';
import { MeasureName } from '../request/enum/measure-name';
import { FacetsRequest } from '../request/facets-request';
import { MeasuresRequest } from '../request/measures-request';
import { TimeSeriesRequest } from '../request/time-series-request';
import { FacetsResponse } from '../response/facets-response';
import { MeasuresResponse } from '../response/measures-response';
import { TimeSeriesResponse } from '../response/time-series-response';

export type WidgetType = PieType | 'stats' | 'top' | 'bar' | 'line';
export type RequestType = keyof RequestResponseMap;

export type Request = MeasuresRequest | FacetsRequest | TimeSeriesRequest;
export type MetricsResponse = MeasuresResponse | FacetsResponse | TimeSeriesResponse;

interface RequestResponseMap {
  measures: MeasuresResponse;
  facets: FacetsResponse;
  'time-series': TimeSeriesResponse;
}

export interface Widget<R extends Request = Request> {
  id: string;
  title: string;
  type: WidgetType;
  description?: string;
  layout: WidgetLayout;
  request?: R;
  response?: RequestResponseMap[R['type']];
}

export interface Measure {
  name: MeasureName;
  value: number;
}

export interface WidgetLayout {
  cols: number;
  rows: number;
  x: number;
  y: number;
}

export function isMeasuresWidget(widget: Widget): widget is Widget<MeasuresRequest> {
  return widget.request?.type === 'measures';
}

export function isFacetsWidget(widget: Widget): widget is Widget<FacetsRequest> {
  return widget.request?.type === 'facets';
}

export function isTimeSeriesWidget(widget: Widget): widget is Widget<TimeSeriesRequest> {
  return widget.request?.type === 'time-series';
}
