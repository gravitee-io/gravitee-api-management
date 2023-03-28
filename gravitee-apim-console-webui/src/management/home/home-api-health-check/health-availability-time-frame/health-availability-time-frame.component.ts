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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import * as Highcharts from 'highcharts';

export interface HealthAvailabilityTimeFrameOption {
  timestamp: {
    start: number;
    interval: number;
  };
  data: number[];
}

@Component({
  selector: 'health-availability-time-frame',
  template: require('./health-availability-time-frame.component.html'),
  styles: [require('./health-availability-time-frame.component.scss')],
})
export class HealthAvailabilityTimeFrameComponent implements OnChanges {
  @Input()
  public option: HealthAvailabilityTimeFrameOption;

  Highcharts: typeof Highcharts = Highcharts;
  chartOptions: Highcharts.Options;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.option) {
      this.chartOptions = {
        chart: {
          type: 'column',
          backgroundColor: 'transparent',
          marginBottom: 20,
        },
        title: undefined,
        plotOptions: {
          series: {
            pointStart: this.option.timestamp.start,
            pointInterval: this.option.timestamp.interval,
            marker: {
              enabled: false,
            },
          },
        },
        series: [
          {
            name: 'Availability',
            data: [...this.option.data],
            color: '#5CB85C',
            type: 'column',
            label: {
              format: '{value} %',
            },
            zones: [
              {
                value: 80,
                color: '#D9534F',
              },
              {
                value: 95,
                color: '#F0AD4E',
              },
              {
                color: '#5CB85C',
              },
            ],
          },
        ],
        xAxis: {
          type: 'datetime',
          dateTimeLabelFormats: {
            month: '%e. %b',
            year: '%b',
          },
          crosshair: true,
        },
        yAxis: {
          visible: false,
          max: 100,
        },

        tooltip: {
          pointFormat: '<tr><td>{series.name}: </td><td><b>{point.y:.1f} %</b></td></tr>',
          shared: true,
          useHTML: true,
          style: {
            zIndex: 1000,
          },
        },
        legend: {
          enabled: false,
        },
        credits: {
          enabled: false,
        },
      };
    }
  }
}
