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
import { Component, input } from '@angular/core';
import { ChartConfiguration, ChartData, ChartEvent } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

export type PieType = 'doughnut' | 'pie' | 'polarArea';

@Component({
  selector: 'gd-pie-chart',
  imports: [BaseChartDirective],
  templateUrl: './pie-chart.component.html',
  styleUrl: './pie-chart.component.scss',
})
export class PieChartComponent<T extends PieType> {
  type = input.required<T>();
  option = input<ChartConfiguration<T>['options']>(this.getDefaultOptions());
  data = input<ChartData<T>>(this.getDataMock());

  public chartClicked({ event, active }: { event: ChartEvent; active: object[] }): void {
    console.log(event, active);
  }

  public chartHovered({ event, active }: { event: ChartEvent; active: object[] }): void {
    console.log(event, active);
  }

  private getDataMock(): ChartData<T> {
    return {
      labels: ['North America', 'Europe', 'Asia Pacific', 'South America', 'Africa', 'Middle East', 'Oceania'],
      datasets: [{ data: [35, 28, 20, 8, 5, 3, 1] }],
    } as ChartData<T>;
  }

  private getDefaultOptions(): ChartConfiguration<T>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'bottom',
        },
      },
    } as ChartConfiguration<T>['options'];
  }
}
