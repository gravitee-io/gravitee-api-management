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
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { forkJoin, map, of, switchMap } from 'rxjs';

import { Filter, GenericFilterBarComponent, SelectedFilter } from './components/filter/generic-filter-bar/generic-filter-bar.component';
import { GridComponent } from './components/grid/grid.component';
import { FilterName } from './components/widget/model/request/enum/filter-name';
import { RequestFilter } from './components/widget/model/request/request';
import { Widget } from './components/widget/model/widget/widget';
import { GraviteeDashboardService } from './gravitee-dashboard.service';

@Component({
  selector: 'gd-dashboard',
  imports: [GridComponent, GenericFilterBarComponent],
  template: `<div class="container">
    <gd-generic-filter-bar
      [filters]="filters()"
      [currentSelectedFilters]="currentSelectedFilters()"
      (selectedFilters)="onSelectedFilters($event)" />

    <gd-grid [items]="dashboardWidgets()" />
  </div>`,
  styles: `
    .container {
      margin: 18px;
    }
  `,
})
export class GraviteeDashboardComponent {
  baseURL = input.required<string>();
  filters = input.required<Filter[]>();
  widgetConfigs = input.required<Widget[]>();

  currentSelectedFilters = toSignal(this.activatedRoute.queryParams.pipe(map(params => this.getSelectedFiltersFromQueryParams(params))), {
    initialValue: [] as SelectedFilter[],
  });

  readonly widgetsWithFilters = computed(() => {
    return this.getUpdatedWidgetsWithFilters(this.widgetConfigs(), this.currentSelectedFilters());
  });

  readonly dashboardWidgets = toSignal(
    toObservable(this.widgetsWithFilters).pipe(
      switchMap(widgets => {
        const loadObservables = widgets.map(w => this.loadWidgetData(w));
        return forkJoin(loadObservables);
      }),
    ),
    { initialValue: [] as Widget[] },
  );

  private readonly router = inject(Router);
  private readonly dashboardService = inject(GraviteeDashboardService);

  constructor(private readonly activatedRoute: ActivatedRoute) {}

  onSelectedFilters($event: SelectedFilter[]) {
    const queryParams: Record<string, string> = {};
    const groupedFilters = this.groupFilters($event);

    groupedFilters.forEach((values, key) => {
      queryParams[key] = values.join(',');
    });

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams,
    });
  }

  private loadWidgetData(widget: Widget) {
    if (!widget.request) return of(widget);

    return this.dashboardService
      .getMetrics(this.baseURL(), widget.request.type, widget.request)
      .pipe(map(response => ({ ...widget, response }) satisfies Widget));
  }

  private getUpdatedWidgetsWithFilters(widgets: Widget[], selectedFilters: SelectedFilter[]): Widget[] {
    const groupedFilters = this.groupFilters(selectedFilters);
    const newFilters: RequestFilter[] = [];

    groupedFilters.forEach((values, key) => {
      newFilters.push({ name: key, operator: 'IN', value: values });
    });

    return widgets.map(widget => {
      if (!widget.request) return widget;

      return {
        ...widget,
        request: { ...widget.request, filters: newFilters },
        response: undefined,
      };
    });
  }

  private groupFilters(selectedFilters: SelectedFilter[]) {
    const groupedFilters = new Map<FilterName, string[]>();
    selectedFilters.forEach(selectedFilter => {
      const key = selectedFilter.parentKey as FilterName;
      const value = selectedFilter.value;
      groupedFilters.has(key) ? groupedFilters.get(key)!.push(value) : groupedFilters.set(key, [value]);
    });
    return groupedFilters;
  }

  private getSelectedFiltersFromQueryParams(params: Params) {
    const selectedFilters: SelectedFilter[] = [];
    Object.keys(params).forEach(key => {
      const values: string[] = params[key].split(',');
      values.forEach(value => selectedFilters.push({ parentKey: key, value: value.trim() }));
    });
    return selectedFilters;
  }
}
