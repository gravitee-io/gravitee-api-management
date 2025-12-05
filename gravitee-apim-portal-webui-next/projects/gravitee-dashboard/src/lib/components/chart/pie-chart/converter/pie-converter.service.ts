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

import { Converter } from '../../../converter';
import { FacetsResponse } from '../../../widget/model/response/facets-response';
import { PieType } from '../pie-chart.component';

@Injectable({
  providedIn: 'root',
})
export class PieConverterService implements Converter {
  public convert(data: FacetsResponse): ChartData<PieType, number[], string> {
    const labels: string[] = [];
    const dataValues: number[] = [];

    if (data?.metrics?.length) {
      for (const bucket of data.metrics[0].buckets) {
        if (bucket.measures?.length) {
          labels.push(bucket.name);
          dataValues.push(bucket.measures[0].value);
        }
      }
    }
    return {
      labels: labels,
      datasets: [{ data: dataValues }],
    } satisfies ChartData<PieType, number[], string>;
  }
}
