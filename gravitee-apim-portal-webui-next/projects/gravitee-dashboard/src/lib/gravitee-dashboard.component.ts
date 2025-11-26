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
import { forkJoin, map, of, switchMap } from 'rxjs';

import { DropdownSearchComponent } from './components/filter/dropdown-search/dropdown-search.component';
import { GridComponent } from './components/grid/grid.component';
import { Widget } from './components/widget/model/widget/widget';
import { GraviteeDashboardService } from './gravitee-dashboard.service';
import { Filter, GenericFilterBarComponent, SelectedFilter } from './components/filter/generic-filter-bar/generic-filter-bar.component';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'gd-dashboard',
  imports: [GridComponent, DropdownSearchComponent, GenericFilterBarComponent],
  template: `<gd-generic-filter-bar
      [filters]="filters()"
      [currentSelectedFilters]="currentSelectedFilters()"
      (selectedFilters)="onSelectedFilters($event)" />
    <gd-grid [items]="widgets()" />`,
  styles: ``,
})
export class GraviteeDashboardComponent {
  dashboardService = inject(GraviteeDashboardService);
  baseURL = input.required<string>();
  widgets = model.required<Widget[]>();
  filters = input.required<Filter[]>();

  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);

  currentSelectedFilters = toSignal(
    this.activatedRoute.queryParams.pipe(
      map(params => {
        const selectedFilters: SelectedFilter[] = [];

        for (const key in params) {
          if (params.hasOwnProperty(key)) {
            const paramValue: string = params[key];
            const values: string[] = paramValue.split(',');

            values.forEach(value => {
              selectedFilters.push({
                parentKey: key,
                value: value.trim(),
              });
            });
          }
        }
        return selectedFilters;
      }),
    ),
    { initialValue: [] as SelectedFilter[] },
  );

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
  onSelectedFilters($event: SelectedFilter[]) {
    console.log('onSelectedFilters', $event);
    // Create query params for the router from the list
    // For each selected filter, group by parentKey and create an array of values
    const groupedFilters = new Map<string, string[]>();
    const queryParams: Record<string, string | string[] | null> = {};
    $event.forEach(filter => {
      const key = filter.parentKey;
      const value = filter.value;

      if (groupedFilters.has(key)) {
        groupedFilters.get(key)!.push(value);
      } else {
        groupedFilters.set(key, [value]);
      }
    });

    groupedFilters.forEach((values, key) => {
      queryParams[key] = values.join(',');
    });

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams,
    });
  }
}
