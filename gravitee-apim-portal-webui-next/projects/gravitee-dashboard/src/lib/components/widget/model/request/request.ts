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
import { RequestType } from '../widget/widget.model';
import { FilterName } from './enum/filter-name';
import { MeasureName } from './enum/measure-name';
import { MetricName } from './enum/metric-name';

export interface GlobalRequest<E extends RequestType> {
  type: E;
  timeRange?: TimeRange;
  filters?: RequestFilter[];
  metrics: MetricRequest[];
}

export interface MetricRequest {
  name: MetricName;
  measures: MeasureName[];
  filters?: RequestFilter[];
}

export interface Order {
  measure: MeasureName;
  direction: 'ASC' | 'DESC';
}

export interface TimeRange {
  from: string;
  to: string;
}

export interface RequestFilter {
  name: FilterName;
  operator: 'EQ' | 'IN' | 'LTE' | 'GTE';
  value: string | string[];
}
