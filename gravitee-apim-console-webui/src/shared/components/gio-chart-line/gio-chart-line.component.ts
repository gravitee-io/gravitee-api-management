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

@Component({
  selector: 'gio-chart-line',
  templateUrl: './gio-chart-line.component.html',
  styleUrls: ['./gio-chart-line.component.scss'],
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
        backgroundColor: 'transparent',
        type: 'spline',
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
          pointStart: this.options.pointStart,
          pointInterval: this.options.pointInterval,
        },
        line: {},
      },

      series: this.data.map((item) => ({
        name: item.name,
        data: item.values,
        type: 'spline',
      })),
    };
  }
}
