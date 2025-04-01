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

export interface GioChartPieInput {
  label: string;
  value: number;
  color: string;
}

@Component({
  selector: 'gio-chart-pie',
  templateUrl: './gio-chart-pie.component.html',
  styleUrls: ['./gio-chart-pie.component.scss'],
  standalone: false,
})
export class GioChartPieComponent implements OnInit {
  @Input()
  public input: GioChartPieInput[];

  @Input()
  public inputDescription = 'Nb hits';

  @Input()
  public totalInputDescription = 'Total';

  Highcharts: typeof Highcharts = Highcharts;
  chartOptions: Highcharts.Options;

  callbackFunction: Highcharts.ChartCallbackFunction = function (chart) {
    // Redraw the chart after the component is loaded. to fix the issue of the chart display with bad size
    setTimeout(() => {
      chart?.reflow();
    }, 0);
  };

  ngOnInit() {
    const totalInputDescription = this.totalInputDescription;

    this.chartOptions = {
      title: {
        text: '',
      },
      credits: { enabled: false },
      chart: {
        height: '100%',
        backgroundColor: 'transparent',
        spacing: [0, 0, 0, 0],
        // Add bottom left total
        events: {
          load: function () {
            const total = this.series[0].data[0].total;

            this.setSubtitle({
              text: totalInputDescription + ': ' + total,
              align: 'left',
              verticalAlign: 'bottom',
              style: {
                color: 'default',
              },
            });
          },
        },
      },
      tooltip: {
        pointFormat: '{series.name}: {point.y} ({point.percentage:.1f} %)',
      },
      plotOptions: {
        pie: {
          dataLabels: {
            enabled: true,
            format: '<b>{point.name}</b>: {point.y} ({point.percentage:.1f} %)',
          },
        },
      },
      series: [
        {
          data: this.input?.map((d) => [d.label, d.value]),
          name: this.inputDescription,
          colors: this.input?.map((d) => d.color),
          type: 'pie',
        },
      ],
    };
  }
}
