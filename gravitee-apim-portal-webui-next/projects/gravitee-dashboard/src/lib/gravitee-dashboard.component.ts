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
import { Component, effect, inject, model } from '@angular/core';
import { map, of } from 'rxjs';

import { GridComponent } from './components/grid/grid.component';
import { Widget } from './components/widget/widget';
import { GraviteeDashboardService } from './gravitee-dashboard.service';

@Component({
  selector: 'gd-dashboard',
  imports: [GridComponent],
  template: `<gd-grid [items]="widgets()" />`,
  styles: ``,
})
export class GraviteeDashboardComponent {
  dashboardService = inject(GraviteeDashboardService);
  widgets = model<Widget[]>(this.dashboardService.getWidgets());

  constructor() {
    effect(() => {
      for (const widget of this.widgets()) {
        if (!widget.data && widget.request) {
          this.loadWidgetData(widget).subscribe(updatedWidget => {
            this.updateWidgetInSignal(updatedWidget);
          });
        }
      }
    });
  }

  addWidget(widget: Widget) {
    this.widgets.update(widgets => [...widgets, widget]);
  }

  removeWidget(id: string) {
    this.widgets.update(widgets => widgets.filter(w => w.id !== id));
  }

  private loadWidgetData(widget: Widget) {
    if (!widget.request) return of(widget);

    return this.dashboardService.getMetricsMock('basePath', widget.request.type, widget.request).pipe(map(data => ({ ...widget, data })));
  }

  private updateWidgetInSignal(updatedWidget: Widget) {
    this.widgets.update(widgets => widgets.map(w => (w.id === updatedWidget.id ? updatedWidget : w)));
  }
}
