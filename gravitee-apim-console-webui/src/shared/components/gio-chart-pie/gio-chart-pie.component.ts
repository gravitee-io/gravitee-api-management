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

import { GioChartAbstractComponent } from '../gio-chart-abstract/gio-chart-abstract.component';

const defaultLabelFormatter = function () {
  const name = this.point.name;
  const value = this.point.y;
  const percentage = this.point.percentage;

  // Split long names into multiple lines (max 15 characters per line)
  const maxCharsPerLine = 15;
  let formattedName = name;

  if (name.length > maxCharsPerLine) {
    const words = name.split(' ');
    const lines = [];
    let currentLine = '';

    for (const word of words) {
      if ((currentLine + word).length <= maxCharsPerLine) {
        currentLine += (currentLine ? ' ' : '') + word;
      } else {
        if (currentLine) lines.push(currentLine);
        currentLine = word;
      }
    }
    if (currentLine) lines.push(currentLine);

    formattedName = lines.join('<br/>');
  }

  return `<div style="text-align: center;">
                <div style="font-weight: bold; margin-bottom: 2px;">${formattedName}</div>
                <div style="font-size: 11px; color: #666;">${value} (${percentage.toFixed(1)}%)</div>
              </div>`;
};

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
export class GioChartPieComponent extends GioChartAbstractComponent implements OnInit {
  @Input()
  public input: GioChartPieInput[];

  @Input()
  public inputDescription = 'Nb hits';

  @Input()
  public totalInputDescription = 'Total';

  @Input()
  public showLegend = false;

  @Input()
  public labelFormatter = defaultLabelFormatter;

  @Input()
  public height = '100%';

  chartOptions: Highcharts.Options;

  ngOnInit() {
    const totalInputDescription = this.totalInputDescription;

    this.chartOptions = {
      title: {
        text: '',
      },
      credits: { enabled: false },
      chart: {
        height: this.height,
        plotBackgroundColor: '#F7F7F8',
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
          showInLegend: this.showLegend,
          dataLabels: {
            enabled: true,
            distance: 25,
            style: {
              textAlign: 'center',
              textOutline: 'none',
              fontSize: '12px',
              fontFamily: 'Manrope, sans-serif',
            },
            formatter: this.labelFormatter,
            useHTML: true,
          },
        },
      },
      series: [
        {
          data: this.input?.map(d => [d.label, d.value]),
          name: this.inputDescription,
          colors: this.input?.map(d => d.color),
          type: 'pie',
        },
      ],
    };
  }
}
