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
import { Component, effect, input, output, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CompactType, DisplayGrid, GridsterComponent, GridsterConfig, GridsterItemComponent, GridType } from 'angular-gridster2';
import { debounceTime, Subject } from 'rxjs';

import { CategoryChartComponent } from '../chart/category-chart/category-chart.component';
import { PieChartComponent } from '../chart/pie-chart/pie-chart.component';
import { TimeSeriesChartComponent } from '../chart/time-series-chart/time-series-chart.component';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { StatsComponent } from '../text/stats/stats.component';
import { isFacetsWidget, isMeasuresWidget, isTimeSeriesWidget, Widget } from '../widget/model/widget/widget.model';
import { WidgetBodyComponent, WidgetComponent, WidgetTitleComponent } from '../widget/widget.component';

@Component({
  selector: 'gd-grid',
  encapsulation: ViewEncapsulation.None,
  imports: [
    GridsterComponent,
    GridsterItemComponent,
    WidgetComponent,
    WidgetTitleComponent,
    WidgetBodyComponent,
    TimeSeriesChartComponent,
    CategoryChartComponent,
    PieChartComponent,
    StatsComponent,
    MatTooltipModule,
    MatProgressSpinner,
    EmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
  ],
  templateUrl: './grid.component.html',
  styleUrl: './grid.component.scss',
})
export class GridComponent {
  items = input<Widget[]>();
  margin = input<number>(32);
  editMode = input<boolean>(false);
  readonly widgetsChange = output<Widget[]>();
  readonly layoutChange = output<Widget[]>();

  protected readonly isMeasuresWidget = isMeasuresWidget;
  protected readonly isFacetsWidget = isFacetsWidget;
  protected readonly isTimeSeriesWidget = isTimeSeriesWidget;

  protected readonly gridsterOptions: GridsterConfig = this.buildOptions();

  // Deep-copied snapshot of widget positions taken each time the items signal changes.
  // Gridster mutates layout objects in-place, so this snapshot preserves pre-mutation values
  // that we can diff against when an itemChangeCallback fires.
  private readonly layoutSnapshot = new Map<string, { x: number; y: number; cols: number; rows: number }>();

  // Debounce channel for gridster's itemChangeCallback. Fires after all downstream
  // Gridster updates (makeDrag microtask + calculateLayout$ debounceTime(0) compaction)
  // have settled, so onUserLayoutChange() always reads the final committed positions.
  private readonly itemChangeSubject = new Subject<void>();

  constructor() {
    effect(() => {
      const currentIds = new Set<string>();
      for (const w of this.items() ?? []) {
        currentIds.add(w.id);
        this.layoutSnapshot.set(w.id, { x: w.layout.x, y: w.layout.y, cols: w.layout.cols, rows: w.layout.rows });
      }
      for (const id of [...this.layoutSnapshot.keys()]) {
        if (!currentIds.has(id)) this.layoutSnapshot.delete(id);
      }
    });

    effect(() => {
      const edit = this.editMode();
      this.gridsterOptions.draggable = {
        enabled: edit,
        ignoreContentClass: 'gridster-item-content',
      };
      this.gridsterOptions.resizable = {
        enabled: edit,
      };
      this.gridsterOptions.itemChangeCallback = edit ? () => this.itemChangeSubject.next() : undefined;
      this.gridsterOptions.displayGrid = DisplayGrid.None;
      this.gridsterOptions.api?.optionsChanged?.();
    });

    this.itemChangeSubject.pipe(debounceTime(50), takeUntilDestroyed()).subscribe(() => this.onUserLayoutChange());
  }

  removeWidget(widgetToRemove: Widget): void {
    const updated = (this.items() ?? []).filter(w => w.id !== widgetToRemove.id);
    this.widgetsChange.emit(updated);
  }

  private onUserLayoutChange(): void {
    const current = this.items() ?? [];
    const hasChanged = current.some(w => {
      const snap = this.layoutSnapshot.get(w.id);
      if (!snap) return true;
      return snap.x !== w.layout.x || snap.y !== w.layout.y || snap.cols !== w.layout.cols || snap.rows !== w.layout.rows;
    });

    if (!hasChanged) return;

    // Update snapshot immediately so back-to-back gestures compare against the right baseline.
    for (const w of current) {
      this.layoutSnapshot.set(w.id, { x: w.layout.x, y: w.layout.y, cols: w.layout.cols, rows: w.layout.rows });
    }

    const updated = current.map(w => ({ ...w, layout: { ...w.layout } }));
    this.layoutChange.emit(updated);
  }

  private buildOptions(): GridsterConfig {
    return {
      gridType: GridType.VerticalFixed,
      compactType: CompactType.CompactUp,
      displayGrid: DisplayGrid.None,
      pushItems: true,
      minCols: 4,
      maxCols: 5,
      draggable: {
        enabled: false,
        ignoreContentClass: 'gridster-item-content',
      },
      resizable: {
        enabled: false,
      },
      outerMargin: true,
      outerMarginRight: this.margin(),
      outerMarginLeft: this.margin(),
      setGridSize: true,
      fixedRowHeight: 125,
      initCallback: () => {
        this.gridsterOptions.api?.optionsChanged?.();
      },
    };
  }
}
