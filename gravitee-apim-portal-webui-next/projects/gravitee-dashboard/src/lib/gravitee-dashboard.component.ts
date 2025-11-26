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
import {forkJoin, map, of, switchMap} from 'rxjs';

import { DropdownSearchComponent } from './components/filter/dropdown-search/dropdown-search.component';
import { GridComponent } from './components/grid/grid.component';
import { Widget } from './components/widget/model/widget/widget';
import { GraviteeDashboardService } from './gravitee-dashboard.service';
import {
  Filter,
  GenericFilterBarComponent,
  SelectedFilter
} from "./components/filter/generic-filter-bar/generic-filter-bar.component";
import {ActivatedRoute, Router} from "@angular/router";
import {toSignal} from "@angular/core/rxjs-interop";

@Component({
  selector: 'gd-dashboard',
  imports: [GridComponent, DropdownSearchComponent, GenericFilterBarComponent],
  template: ` <!--    <gd-filter-bar-->
    <!--      [activeFilters]="activeFilters()"-->
    <!--      [plans]="apiPlans$ | async"-->
    <!--      (filtersChange)="onFiltersChange($event)"-->
    <!--      (refresh)="onRefreshFilters()" />-->
    <gd-generic-filter-bar [filters]="filters()" [currentSelectedFilters]="currentSelectedFilters()" (selectedFilters)="onSelectedFilters($event)" />
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

  currentSelectedFilters = toSignal(this.activatedRoute.queryParams.pipe(
    map(params => {
      const selectedFilters: SelectedFilter[] = [];
      for (const key in params) {
        const values = Array.isArray(params[key]) ? params[key] : [params[key]];
        values.forEach(value => {
          selectedFilters.push({ parentKey: key, value: value });
        });
      }
      return selectedFilters;
    }
  )), { initialValue: [] as SelectedFilter[] });


  // public activeFilters: Signal<FilterBarComponent> = computed(() => this.mapQueryParamsToFilters(this.activatedRouteQueryParams()));
  // private activatedRouteQueryParams = toSignal(this.activatedRoute.queryParams);

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
    // effect(() => {
    //   this.dashboardService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(this.activatedRouteQueryParams()));
    // });
  }
  //
  // onFiltersChange(filters: Filters): void {
  //   this.updateQueryParamsFromFilters(filters);
  // }
  // onRefreshFilters(): void {
  //   this.dashboardService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(this.activeFilters()));
  // }

  private loadWidgetData(widget: Widget) {
    if (!widget.request) return of(widget);

    const metrics$ = this.dashboardService.getMetrics(this.baseURL(), widget.request.type, widget.request);
    return metrics$.pipe(switchMap(response => of({ ...widget, response: response } satisfies Widget)));
  }
  // private mapQueryParamsToFilters(queryParams: unknown): Filters {
  //   const params = queryParams as QueryParamsBase;
  //   const normalizedPeriod = params.period || '1d';
  //   const filters = this.getFilterFields(params);
  //
  //   if (normalizedPeriod === 'custom' && params.from && params.to) {
  //     return <Filters>{
  //       period: normalizedPeriod,
  //       from: +params.from,
  //       to: +params.to,
  //       ...filters,
  //     };
  //   }
  // }
  //
  // private getFilterFields(queryParams: QueryParamsBase) {
  //   return {
  //     httpStatuses: this.processFilter(queryParams.httpStatuses),
  //     plans: this.processFilter(queryParams.plans),
  //     applications: this.processFilter(queryParams.applications),
  //   };
  // }
  // private processFilter(value: string | string[] | undefined): string[] | undefined {
  //   if (value === undefined) {
  //     return undefined;
  //   }
  //   return Array.isArray(value) ? value : value.split(',');
  // }
  //
  // private updateQueryParamsFromFilters(filters: Filters): void {
  //   const queryParams = this.createQueryParamsFromFilters(filters);
  //   this.router.navigate([], {
  //     queryParams,
  //     queryParamsHandling: 'replace',
  //   });
  // }
  // private createQueryParamsFromFilters(filters: Filters): Record<string, any> {
  //   const params: Record<string, any> = {};
  //
  //   if (filters.period === 'custom' && filters.from && filters.to) {
  //     params['from'] = filters.from;
  //     params['to'] = filters.to;
  //     params['period'] = 'custom';
  //   } else {
  //     params['period'] = filters.period;
  //   }
  //
  //   if (filters.httpStatuses?.length) {
  //     params['httpStatuses'] = filters.httpStatuses.join(',');
  //   }
  //
  //   if (filters.plans?.length) {
  //     params['plans'] = filters.plans.join(',');
  //   }
  //
  //   if (filters.applications?.length) {
  //     params['applications'] = filters.applications.join(',');
  //   }
  //
  //   return params;
  // }
  onSelectedFilters($event: SelectedFilter[]) {
    // Create query params for the router from the list
      // For each selected filter, group by parentKey and create an array of values
    const queryParams: Record<string, any> = {};
    $event.forEach(filter => {
      if (!queryParams[filter.parentKey]) {
        queryParams[filter.parentKey] = [];
      }
      queryParams[filter.parentKey].push(filter.value);
    });
    console.log(queryParams)

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams,
    })
  }
}
//
// interface QueryParamsBase {
//   from?: string;
//   to?: string;
//   period?: string;
//   httpStatuses?: string;
//   plans?: string;
//   applications?: string[];
// }
