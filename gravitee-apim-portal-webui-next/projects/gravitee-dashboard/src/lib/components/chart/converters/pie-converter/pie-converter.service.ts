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
import { Injectable } from '@angular/core';
import { ChartData } from 'chart.js';

import { MetricsResponse } from '../../../widget/widget';
import { PieType } from '../../pie-chart/pie-chart.component';

@Injectable({
  providedIn: 'root',
})
export class PieConverterService {
  constructor() {}

  public convert<T extends PieType>(data: MetricsResponse): ChartData<T> {
    const labels: string[] = [];
    const dataValues: number[] = [];

    if (!data?.metrics || data.metrics.length === 0) {
      return {
        labels: [] as string[],
        datasets: [{ data: [] as number[] }],
      } as ChartData<T>;
    }

    for (const metric of data.metrics) {
      for (const bucket of metric.buckets ?? []) {
        if (bucket?.measures && bucket.measures.length > 0) {
          labels.push(bucket.key);
          dataValues.push(bucket.measures[0].value);
        }
      }
    }

    return {
      labels: labels,
      datasets: [{ data: dataValues }],
    } as ChartData<T>;
  }
}
