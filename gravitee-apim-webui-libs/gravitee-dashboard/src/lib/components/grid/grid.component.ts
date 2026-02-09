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
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CompactType, DisplayGrid, GridsterComponent, GridsterConfig, GridsterItemComponent, GridType } from 'angular-gridster2';

import { BarChartComponent } from '../chart/bar-chart/bar-chart.component';
import { LineChartComponent } from '../chart/line-chart/line-chart.component';
import { PieChartComponent } from '../chart/pie-chart/pie-chart.component';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { StatsComponent } from '../text/stats/stats.component';
import { isFacetsWidget, isMeasuresWidget, isTimeSeriesWidget, Widget } from '../widget/model/widget/widget.model';
import { WidgetBodyComponent, WidgetComponent, WidgetTitleComponent } from '../widget/widget.component';

@Component({
  selector: 'gd-grid',
  imports: [
    GridsterComponent,
    GridsterItemComponent,
    WidgetComponent,
    WidgetTitleComponent,
    WidgetBodyComponent,
    BarChartComponent,
    LineChartComponent,
    PieChartComponent,
    StatsComponent,
    MatTooltipModule,
    MatProgressSpinner,
    EmptyStateComponent,
  ],
  templateUrl: './grid.component.html',
  styleUrl: './grid.component.scss',
})
export class GridComponent {
  items = input<Widget[]>();
  margin = input<number>(32);
  options = input<GridsterConfig>(this.getGridsterOptions());

  protected readonly isMeasuresWidget = isMeasuresWidget;
  protected readonly isFacetsWidget = isFacetsWidget;
  protected readonly isTimeSeriesWidget = isTimeSeriesWidget;

  private getGridsterOptions(): GridsterConfig {
    return {
      gridType: GridType.VerticalFixed,
      compactType: CompactType.None,
      displayGrid: DisplayGrid.None,
      itemAspectRatio: 4 / 3,
      pushItems: true,
      draggable: {
        enabled: false,
        dragHandleClass: '.widget-title-container',
      },
      resizable: {
        enabled: false,
      },
      outerMargin: true,
      outerMarginRight: this.margin(),
      outerMarginLeft: this.margin(),
      setGridSize: true,
      fixedRowHeight: 125,
    };
  }
}
