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

export type WidgetType = PieType | 'stats' | 'top';
export type RequestType = 'measures' | 'facets' | 'time-series';

export type MetricsRequest = MeasuresRequest | FacetsRequest | TimeSeriesRequest;
export type MetricsResponse = MeasuresResponse | FacetsResponse | TimeSeriesResponse;

export interface Widget<R extends MetricsRequest = MetricsRequest> {
  id: string;
  title: string;
  type: WidgetType;
  layout: WidgetLayout;
  request?: R;
  response?: Extract<MetricsResponse, { type: R['type'] }>;
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
