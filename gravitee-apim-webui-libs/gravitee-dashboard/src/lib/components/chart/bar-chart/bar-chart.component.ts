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
import { Chart, ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import 'chartjs-adapter-date-fns';

import { BarConverterService } from './converter/bar-converter.service';
import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';
import { assignChartColors } from '../shared/chart-colors';

export type BarType = 'bar';

@Component({
  selector: 'gd-bar-chart',
  imports: [BaseChartDirective],
  templateUrl: './bar-chart.component.html',
  styleUrl: './bar-chart.component.scss',
})
export class BarChartComponent {
  type = input<BarType>('bar');
  option: InputSignal<ChartConfiguration<BarType>['options']> = input(this.getDefaultOptions());
  data = input.required<TimeSeriesResponse>();

  public readonly dataFormatted = computed(() => {
    const chartData = this.converter.convert(this.data());
    assignChartColors(chartData.datasets);
    return chartData;
  });
  private readonly converter = inject(BarConverterService);

  private getDefaultOptions(): ChartConfiguration<BarType>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      elements: {
        bar: {
          borderWidth: 1,
        },
      },
      plugins: {
        legend: {
          display: true,
          position: 'bottom',
          labels: {
            usePointStyle: true,
            pointStyle: 'rectRounded',
            generateLabels: chart => {
              const defaults = Chart.defaults.plugins.legend.labels.generateLabels(chart);
              return defaults.map(label => ({
                ...label,
                lineWidth: 1,
              }));
            },
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
  }
}
