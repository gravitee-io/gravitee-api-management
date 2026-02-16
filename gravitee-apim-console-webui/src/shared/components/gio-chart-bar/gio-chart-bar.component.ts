/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { Component, Input, OnInit } from '@angular/core';
import { HighchartsChartModule } from 'highcharts-angular';
import * as Highcharts from 'highcharts';
import { round } from 'lodash';

import { GioChartAbstractComponent } from '../gio-chart-abstract/gio-chart-abstract.component';

export interface GioChartBarData {
  name: string;
  values: number[];
  color?: string;
}

export interface GioChartBarOptions {
  categories?: string[];
  stacked?: boolean;
  pointStart?: number;
  pointInterval?: number;
  reverseStack?: boolean;
  customTooltip?: {
    formatter?: () => string;
  };
}

export const defineBarColors = (code: string | number) => {
  const colors = ['#7B61FF', '#00D4AA', '#FF6B6B', '#FF8C00', '#9370DB', '#20B2AA', '#FFD700', '#FF69B4'];
  return colors[Math.floor(+code / 100) - 1] || colors[0];
};

@Component({
  selector: 'gio-chart-bar',
  templateUrl: './gio-chart-bar.component.html',
  styleUrls: ['./gio-chart-bar.component.scss'],
  standalone: true,
  imports: [HighchartsChartModule],
})
export class GioChartBarComponent extends GioChartAbstractComponent implements OnInit {
  @Input()
  public data: GioChartBarData[];

  @Input()
  public options: GioChartBarOptions;

  chartOptions: Highcharts.Options;

  ngOnInit() {
    this.chartOptions = {
      credits: { enabled: false },
      chart: {
        plotBackgroundColor: '#F7F7F8',
        type: 'column',
        marginTop: 32,
      },

      title: {
        text: '',
      },

      xAxis: {
        categories: this.options?.categories || [],
        type: this.options?.categories ? 'category' : 'datetime',
      },

      yAxis: {
        title: {
          text: '',
        },
      },

      legend: {
        align: 'center',
        verticalAlign: 'bottom',
      },

      tooltip: this.options?.customTooltip?.formatter
        ? {
            formatter: this.options.customTooltip.formatter,
            useHTML: true,
          }
        : {
            shared: true,
          },

      plotOptions: {
        series: {
          pointStart: this.options?.pointStart,
          pointInterval: this.options?.pointInterval,
        },
        column: {
          stacking: this.options?.stacked ? 'normal' : undefined,
        },
        bar: {
          stacking: this.options?.stacked ? 'normal' : undefined,
        },
      },

      series: (this.options?.reverseStack ? [...this.data].reverse() : this.data)?.map(item => ({
        name: item.name,
        data: item.values?.map(value => (value === null ? null : round(value, 2))),
        type: 'column',
        color: item.color || defineBarColors(item.name),
      })),
    };
  }
}
