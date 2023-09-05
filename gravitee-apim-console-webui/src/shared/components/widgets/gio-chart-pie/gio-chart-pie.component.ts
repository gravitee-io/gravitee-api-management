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
  template: require('./gio-chart-pie.component.html'),
  styles: [require('./gio-chart-pie.component.scss')],
})
export class GioChartPieComponent implements OnInit {
  @Input()
  public input: GioChartPieInput[];

  @Input()
  public inputDescription: string;

  Highcharts: typeof Highcharts = Highcharts;
  chartOptions: Highcharts.Options;

  ngOnInit() {
    this.chartOptions = {
      title: {
        text: '',
      },
      credits: { enabled: false },
      chart: {
        height: '100%',
        backgroundColor: 'transparent',
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
