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
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { PieConverterService } from '../../converters/pie-converter/pie-converter.service';
import { MetricsResponse } from '../../widget/widget';

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
  data = input.required<MetricsResponse>();

  converter = inject(PieConverterService);

  public dataFormated = computed(() => {
    return this.converter.convert<T>(this.data());
  });

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
