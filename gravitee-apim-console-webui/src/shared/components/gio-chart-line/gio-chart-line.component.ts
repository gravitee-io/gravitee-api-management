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
import { round } from 'lodash';

import { GioChartAbstractComponent } from '../gio-chart-abstract/gio-chart-abstract.component';

export interface GioChartLineData {
  name: string;
  values: number[];
}
export interface GioChartLineOptions {
  pointStart: number;
  pointInterval: number;
  enableMarkers?: boolean;
  useSharpCorners?: boolean;
}

export const colors = ['#2B72FB', '#64BDC6', '#EECA34', '#FA4B42', '#FE6A35'];

export const defineLineColors = (code: string) => {
  return colors[Math.floor(+code / 100) - 1];
};

@Component({
  selector: 'gio-chart-line',
  templateUrl: './gio-chart-line.component.html',
  styleUrls: ['./gio-chart-line.component.scss'],
  standalone: false,
})
export class GioChartLineComponent extends GioChartAbstractComponent implements OnInit {
  @Input()
  public data: GioChartLineData[];

  @Input()
  public options: GioChartLineOptions;

  chartOptions: Highcharts.Options;

  ngOnInit() {
    const markersEnabled = this.options?.enableMarkers ?? false;

    this.chartOptions = {
      credits: { enabled: false },
      time: { useUTC: false },
      chart: {
        plotBackgroundColor: '#F7F7F8',
        type: this.options?.useSharpCorners ? 'line' : 'spline',
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
          marker: {
            enabled: markersEnabled,
            radius: markersEnabled ? 4 : 0,
            symbol: 'circle',
          },
          pointStart: Math.floor(this.options?.pointStart / this.options?.pointInterval) * this.options?.pointInterval,
          pointInterval: this.options?.pointInterval,
        },
        line: {
          marker: {
            enabled: markersEnabled,
            radius: markersEnabled ? 4 : 0,
            symbol: 'circle',
          },
        },
        spline: {
          marker: {
            enabled: markersEnabled,
            radius: markersEnabled ? 4 : 0,
            symbol: 'circle',
          },
        },
      },

      series: this.data?.map(item => ({
        name: item.name,
        data: item.values?.map(value => (value === null ? null : round(value, 2))),
        type: this.options?.useSharpCorners ? 'line' : 'spline',
        color: defineLineColors(item.name),
        marker: {
          enabled: markersEnabled,
          radius: markersEnabled ? 4 : 0,
          symbol: 'circle',
        },
      })),
    };
  }
}
