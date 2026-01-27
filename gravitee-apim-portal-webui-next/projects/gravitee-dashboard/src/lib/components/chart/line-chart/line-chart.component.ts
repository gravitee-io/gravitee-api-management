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
import 'chartjs-adapter-date-fns';

import { LineConverterService } from './converter/line-converter.service';
import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';
import { assignChartColors } from '../shared/chart-colors';

export type LineType = 'line';

@Component({
  selector: 'gd-line-chart',
  imports: [BaseChartDirective],
  templateUrl: './line-chart.component.html',
  styleUrl: './line-chart.component.scss',
})
export class LineChartComponent {
  type = input<LineType>('line');
  option: InputSignal<ChartConfiguration<LineType>['options']> = input(this.getDefaultOptions());
  data = input.required<TimeSeriesResponse>();

  public readonly dataFormatted = computed(() => {
    const chartData = this.converter.convert(this.data());
    assignChartColors(chartData.datasets);
    chartData.datasets.forEach(dataset => {
      dataset.tension = 0.4;
      dataset.fill = 'start';
      dataset.borderWidth = 1;
    });

    return chartData;
  });
  private readonly converter = inject(LineConverterService);

  private getDefaultOptions(): ChartConfiguration<LineType>['options'] {
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
        tooltip: {
          mode: 'nearest',
          intersect: false,
        },
      },
      elements: {
        point: {
          radius: 0,
          hitRadius: 15,
          hoverRadius: 8,
        },
      },
      scales: {
        x: {
          type: 'time',
          display: true,
          time: {
            unit: 'day',
            tooltipFormat: 'PPpp',
            displayFormats: {
              day: 'd LLL',
            },
          },
        },
        y: {
          display: true,
          beginAtZero: true,
          stacked: true,
        },
      },
    } satisfies ChartConfiguration<LineType>['options'];
  }
}
