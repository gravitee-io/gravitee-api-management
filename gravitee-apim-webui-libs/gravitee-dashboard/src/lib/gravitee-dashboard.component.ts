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
import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router } from '@angular/router';
import moment from 'moment';
import { combineLatest, map, of, startWith, switchMap, take } from 'rxjs';

import { Filter, GenericFilterBarComponent, SelectedFilter } from './components/filter/generic-filter-bar/generic-filter-bar.component';
import { timeFrames, timeFrameRangesParams, calculateCustomInterval } from './components/filter/timeframe-selector/utils/timeframe-ranges';
import { GridComponent } from './components/grid/grid.component';
import { FilterName } from './components/widget/model/request/enum/filter-name';
import { RequestFilter, TimeRange } from './components/widget/model/request/request';
import { TimeSeriesRequest } from './components/widget/model/request/time-series-request';
import { Widget, isTimeSeriesWidget } from './components/widget/model/widget/widget.model';
import { GraviteeDashboardService } from './gravitee-dashboard.service';

@Component({
  selector: 'gd-dashboard',
  imports: [GridComponent, GenericFilterBarComponent],
  template: `<div class="container">
    <gd-generic-filter-bar
      [filters]="filters()"
      [currentSelectedFilters]="currentSelectedFilters()"
      [defaultPeriod]="'5m'"
      (selectedFilters)="onSelectedFilters($event)"
      (refresh)="onRefresh()"
      class="filterBar"
    />

    <gd-grid [items]="dashboardWidgets()" />
  </div>`,
  styles: `
    .filterBar {
      margin-left: 32px;
      margin-right: 32px;
    }
  `,
})
export class GraviteeDashboardComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dashboardService = inject(GraviteeDashboardService);
  baseURL = input.required<string>();
  filters = input.required<Filter[]>();
  widgetConfigs = input.required<Widget[]>();

  currentSelectedFilters = toSignal(this.activatedRoute.queryParams.pipe(map(params => this.getSelectedFiltersFromQueryParams(params))), {
    initialValue: [] as SelectedFilter[],
  });

  readonly widgetsWithFilters = computed(() => {
    this.refreshTrigger();
    return this.getUpdatedWidgetsWithFilters(this.widgetConfigs(), this.currentSelectedFilters());
  });

  readonly dashboardWidgets = toSignal(
    toObservable(this.widgetsWithFilters).pipe(
      switchMap(widgets => {
        const widgetObservables = widgets.map(widget => this.loadWidgetData(widget).pipe(startWith(widget), take(2)));
        return combineLatest(widgetObservables);
      }),
    ),
    { initialValue: [] as Widget[] },
  );

  private readonly refreshTrigger = signal(0);

  onSelectedFilters($event: SelectedFilter[]) {
    const queryParams: Record<string, string> = {};

    const periodFilter = $event.find(f => f.parentKey === 'period');
    const fromFilter = $event.find(f => f.parentKey === 'from');
    const toFilter = $event.find(f => f.parentKey === 'to');

    if (periodFilter) {
      queryParams['period'] = periodFilter.value;

      if (periodFilter.value === 'custom' && fromFilter && toFilter) {
        queryParams['from'] = fromFilter.value;
        queryParams['to'] = toFilter.value;
      }
    }

    const otherFilters = $event.filter(f => !['period', 'from', 'to'].includes(f.parentKey));
    const groupedFilters = this.groupFilters(otherFilters);

    groupedFilters.forEach((values, key) => {
      queryParams[key] = values.join(',');
    });

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams,
    });
  }

  onRefresh(): void {
    this.refreshTrigger.update(value => value + 1);
  }

  private loadWidgetData(widget: Widget) {
    if (!widget.request) return of(widget);

    return this.dashboardService
      .getMetrics(this.baseURL(), widget.request.type, widget.request)
      .pipe(map(response => ({ ...widget, response }) satisfies Widget));
  }

  private getUpdatedWidgetsWithFilters(widgets: Widget[], selectedFilters: SelectedFilter[]): Widget[] {
    const periodFilter = selectedFilters.find(f => f.parentKey === 'period');
    const fromFilter = selectedFilters.find(f => f.parentKey === 'from');
    const toFilter = selectedFilters.find(f => f.parentKey === 'to');

    const { timeRange, interval } = this.getTimeRangeAndInterval(periodFilter?.value, fromFilter?.value, toFilter?.value);

    const otherFilters = selectedFilters.filter(f => !['period', 'from', 'to'].includes(f.parentKey));
    const groupedFilters = this.groupFilters(otherFilters);
    const newFilters: RequestFilter[] = [];

    groupedFilters.forEach((values, key) => {
      if (values.length > 0) {
        newFilters.push({ name: key, operator: 'IN', value: values });
      }
    });

    return widgets.map(widget => {
      if (!widget.request) return widget;

      const finalFilters = newFilters;

      if (isTimeSeriesWidget(widget) && interval !== undefined) {
        return {
          ...widget,
          request: { ...widget.request, timeRange, interval, filters: finalFilters } as TimeSeriesRequest,
          response: undefined,
        };
      }

      return {
        ...widget,
        request: { ...widget.request, timeRange, filters: finalFilters },
        response: undefined,
      };
    });
  }

  private getTimeRangeAndInterval(period?: string, from?: string, to?: string): { timeRange: TimeRange; interval?: number } {
    const normalizedPeriod = period || '5m';

    if (normalizedPeriod === 'custom' && from && to) {
      const fromTimestamp = Number.parseInt(from, 10);
      const toTimestamp = Number.parseInt(to, 10);
      const interval = calculateCustomInterval(fromTimestamp, toTimestamp);

      return {
        timeRange: {
          from: moment(fromTimestamp).toISOString(),
          to: moment(toTimestamp).toISOString(),
        },
        interval,
      };
    }

    const timeFrame = timeFrames.find(tf => tf.id === normalizedPeriod) || timeFrames.find(tf => tf.id === '5m');
    const timeRangeParams = timeFrameRangesParams(timeFrame!.id);

    return {
      timeRange: {
        from: moment(timeRangeParams.from).toISOString(),
        to: moment(timeRangeParams.to).toISOString(),
      },
      interval: timeRangeParams.interval,
    };
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
      const paramValue = params[key];

      if (paramValue == null) return;

      if (key === 'period') {
        selectedFilters.push({ parentKey: 'period', value: String(paramValue) });
      } else if (key === 'from') {
        selectedFilters.push({ parentKey: 'from', value: String(paramValue) });
      } else if (key === 'to') {
        selectedFilters.push({ parentKey: 'to', value: String(paramValue) });
      } else {
        const paramString = Array.isArray(paramValue) ? paramValue.join(',') : String(paramValue);
        const values: string[] = paramString.split(',');
        values.forEach(value => {
          const trimmedValue = value.trim();
          if (trimmedValue) {
            selectedFilters.push({ parentKey: key, value: trimmedValue });
          }
        });
      }
    });

    return selectedFilters;
  }
}
