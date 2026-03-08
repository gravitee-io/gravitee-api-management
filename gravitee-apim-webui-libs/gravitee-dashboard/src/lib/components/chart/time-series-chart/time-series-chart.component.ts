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
import { Chart, ChartConfiguration, ChartDataset } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import 'chartjs-adapter-date-fns';

import { TimeSeriesConverterService } from './converter/time-series-converter.service';
import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';
import { TimeSeriesType } from '../../widget/model/widget/widget.model';
import { assignChartColors } from '../shared/chart-colors';

type ChartJsTimeSeriesType = 'line' | 'bar';

function toChartJsType(type: TimeSeriesType): ChartJsTimeSeriesType {
  return type === 'time-series-line' ? 'line' : 'bar';
}

@Component({
  selector: 'gd-time-series-chart',
  imports: [BaseChartDirective],
  templateUrl: './time-series-chart.component.html',
  styleUrl: './time-series-chart.component.scss',
})
export class TimeSeriesChartComponent {
  type = input.required<TimeSeriesType>();
  data = input.required<TimeSeriesResponse>();

  readonly chartJsType = computed(() => toChartJsType(this.type()));

  private readonly converter = inject(TimeSeriesConverterService);

  public readonly dataFormatted = computed(() => {
    const chartData = this.converter.convert(this.data());
    assignChartColors(chartData.datasets);

    if (this.chartJsType() === 'line') {
      chartData.datasets.forEach(dataset => {
        const lineDataset = dataset as ChartDataset<'line', number[]>;
        lineDataset.tension = 0.4;
        lineDataset.fill = 'start';
        lineDataset.borderWidth = 1;
      });
    }

    return chartData;
  });

  public readonly chartOptions = computed((): ChartConfiguration<ChartJsTimeSeriesType>['options'] => {
    return this.chartJsType() === 'line' ? this.getLineOptions() : this.getBarOptions();
  });

  private getLineOptions(): ChartConfiguration<'line'>['options'] {
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
    };
  }

  private getBarOptions(): ChartConfiguration<'bar'>['options'] {
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
