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
import * as Highcharts from 'highcharts';

export interface GioChartLineData {
  name: string;
  values: number[];
}
export interface GioChartLineOptions {
  pointStart: number;
  pointInterval: number;
}

export const colors = ['#2B72FB', '#64BDC6', '#EECA34', '#FA4B42', '#FE6A35'];

export const defineLineColors = (code: string) => {
  return colors[Math.floor(+code / 100) - 1];
};

export const names = {
  'avg_gateway-response-time-ms': 'Gateway Response Time',
};

export const formatName = (name: string) => {
  return names[name] || name;
};

@Component({
  selector: 'gio-chart-line',
  templateUrl: './gio-chart-line.component.html',
  styleUrls: ['./gio-chart-line.component.scss'],
  standalone: false,
})
export class GioChartLineComponent implements OnInit {
  @Input()
  public data: GioChartLineData[];

  @Input()
  public options: GioChartLineOptions;

  Highcharts: typeof Highcharts = Highcharts;
  chartOptions: Highcharts.Options;

  callbackFunction: Highcharts.ChartCallbackFunction = function (chart) {
    // Redraw the chart after the component is loaded. to fix the issue of the chart display with bad size
    setTimeout(() => {
      chart?.reflow();
    }, 0);
  };

  ngOnInit() {
    this.chartOptions = {
      credits: { enabled: false },
      chart: {
        plotBackgroundColor: '#F7F7F8',
        type: 'spline',
        marginTop: 32,
      },

      title: {
        text: '',
      },

      yAxis: {
        title: {
          text: '',
        },
      },

      xAxis: {
        type: 'datetime',
      },

      legend: {
        align: 'center',
        verticalAlign: 'bottom',
      },

      plotOptions: {
        series: {
          marker: { enabled: false },
          pointStart: Math.floor(this.options?.pointStart / this.options?.pointInterval) * this.options?.pointInterval,
          pointInterval: this.options?.pointInterval,
        },
        line: {},
      },

      series: this.data?.map((item) => ({
        name: formatName(item.name),
        data: item.values,
        type: 'spline',
        color: defineLineColors(item.name),
      })),
    };
  }
}
