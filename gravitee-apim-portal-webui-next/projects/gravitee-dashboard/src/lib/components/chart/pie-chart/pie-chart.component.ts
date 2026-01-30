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
import { Component, computed, inject, input, InputSignal } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { PieConverterService } from './converter/pie-converter.service';
import { FacetsResponse } from '../../widget/model/response/facets-response';
import { CHART_COLORS } from '../shared/chart-colors';

export type PieType = 'doughnut' | 'pie' | 'polarArea';

@Component({
  selector: 'gd-pie-chart',
  imports: [BaseChartDirective],
  templateUrl: './pie-chart.component.html',
  styleUrl: './pie-chart.component.scss',
})
export class PieChartComponent {
  type = input.required<PieType>();
  option: InputSignal<ChartConfiguration<PieType>['options']> = input(this.getDefaultOptions());
  data = input.required<FacetsResponse>();

  converter = inject(PieConverterService);

  public dataFormatted = computed(() => {
    const chartData = this.converter.convert(this.data());

    chartData.datasets.forEach(dataset => {
      dataset.backgroundColor = CHART_COLORS;
    });

    return chartData;
  });

  private getDefaultOptions(): ChartConfiguration<PieType>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'bottom',
          labels: {
            usePointStyle: true,
            pointStyle: 'rectRounded',
          },
        },
      },
    } satisfies ChartConfiguration<PieType>['options'];
  }
}
