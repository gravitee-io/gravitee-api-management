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
import { Component, effect, inject, input, model } from '@angular/core';
import { forkJoin, of, switchMap } from 'rxjs';

import { GridComponent } from './components/grid/grid.component';
import { Widget } from './components/widget/model/widget/widget';
import { GraviteeDashboardService } from './gravitee-dashboard.service';

@Component({
  selector: 'gd-dashboard',
  imports: [GridComponent],
  template: `<gd-grid [items]="widgets()" />`,
  styles: ``,
})
export class GraviteeDashboardComponent {
  dashboardService = inject(GraviteeDashboardService);
  baseURL = input.required<string>();
  widgets = model.required<Widget[]>();

  constructor() {
    effect(() => {
      const widgetsToLoad = this.widgets().filter(w => !w.response && w.request);
      if (widgetsToLoad.length > 0) {
        const loadObservables = widgetsToLoad.map(w => this.loadWidgetData(w));
        forkJoin(loadObservables).subscribe(loadedWidgets => {
          this.widgets.update(currentWidgets => {
            const loadedWidgetsMap = new Map(loadedWidgets.map(w => [w.id, w]));
            return currentWidgets.map(w => loadedWidgetsMap.get(w.id) ?? w);
          });
        });
      }
    });
  }

  private loadWidgetData(widget: Widget) {
    if (!widget.request) return of(widget);

    const metrics$ = this.dashboardService.getMetrics(this.baseURL(), widget.request.type, widget.request);
    return metrics$.pipe(switchMap(response => of({ ...widget, response: response } satisfies Widget)));
  }
}
