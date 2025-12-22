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
import { Component, computed, inject, input } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
// eslint-disable-next-line import/no-unresolved
import 'chartjs-adapter-date-fns';

import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';
import { assignChartColors } from '../shared/chart-colors';
import { TimeSeriesConverterService } from '../shared/time-series-converter.service';

export type BarType = 'bar';

@Component({
  selector: 'gd-bar-chart',
  imports: [BaseChartDirective],
  templateUrl: './bar-chart.component.html',
  styleUrl: './bar-chart.component.scss',
})
export class BarChartComponent {
  type = input<BarType>('bar');
  data = input.required<TimeSeriesResponse>();

  public readonly dataFormatted = computed(() => {
    const chartData = this.converter.convert<'bar'>(this.data(), 'bar');

    assignChartColors(chartData.datasets);

    chartData.datasets.forEach(dataset => {
      dataset.borderWidth = 1;
    });

    return chartData;
  });

  public readonly chartOptions: ChartConfiguration<BarType>['options'] = {
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
        mode: 'index',
        intersect: false,
      },
    },
    scales: {
      x: {
        type: 'time',
        display: true,
        stacked: true,
        time: {
          tooltipFormat: 'PPpp',
          displayFormats: {
            second: 'HH:mm:ss',
            minute: 'HH:mm',
            hour: 'HH:mm',
            day: 'EEE d',
            week: 'd LLL',
            month: 'LLL yyyy',
            year: 'yyyy',
          },
        },
      },
      y: {
        display: true,
        beginAtZero: true,
        stacked: true,
      },
    },
  };

  private readonly converter = inject(TimeSeriesConverterService);
}
