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

import { CategoryConverterService } from './converter/category-converter.service';
import { FacetsResponse } from '../../widget/model/response/facets-response';
import { CategoryType } from '../../widget/model/widget/widget.model';
import { CHART_COLORS, CHART_BORDER_COLORS } from '../shared/chart-colors';

@Component({
  selector: 'gd-category-chart',
  imports: [BaseChartDirective],
  template: `<canvas baseChart [data]="dataFormatted()" type="bar" [options]="chartOptions()"></canvas>`,
  styles: `
    :host {
      position: relative;
      display: flex;
      width: 100%;
      height: 100%;
    }
  `,
})
export class CategoryChartComponent {
  type = input.required<CategoryType>();
  data = input.required<FacetsResponse>();

  private readonly converter = inject(CategoryConverterService);

  public readonly dataFormatted = computed(() => {
    const chartData = this.converter.convert(this.data());
    chartData.datasets.forEach(dataset => {
      dataset.backgroundColor = (dataset.data as number[]).map((_, i) => CHART_COLORS[i % CHART_COLORS.length]);
      dataset.borderColor = (dataset.data as number[]).map((_, i) => CHART_BORDER_COLORS[i % CHART_BORDER_COLORS.length]);
      dataset.borderWidth = 1;
    });
    return chartData;
  });

  public readonly chartOptions = computed((): ChartConfiguration<'bar'>['options'] => {
    const isHorizontal = this.type() === 'horizontal-bar';
    return {
      indexAxis: isHorizontal ? 'y' : 'x',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false,
        },
        tooltip: {
          mode: 'index',
          intersect: false,
        },
      },
      scales: {
        x: {
          type: 'category',
          display: true,
        },
        y: {
          display: true,
          beginAtZero: true,
        },
      },
    };
  });
}
