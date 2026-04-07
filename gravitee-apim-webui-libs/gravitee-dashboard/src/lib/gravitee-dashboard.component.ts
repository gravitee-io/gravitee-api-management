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
import { Component, computed, effect, inject, input, output, signal, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Params, Router } from '@angular/router';
import moment from 'moment';
import { combineLatest, EMPTY, map, of, startWith, Subject, switchMap, take } from 'rxjs';
import { catchError, debounceTime, tap } from 'rxjs/operators';

import {
  DashboardMetadataDialogComponent,
  DashboardMetadataDialogData,
  DashboardMetadataDialogResult,
} from './components/dashboard-metadata-dialog/dashboard-metadata-dialog.component';
import { Filter, GenericFilterBarComponent, SelectedFilter } from './components/filter/generic-filter-bar/generic-filter-bar.component';
import { timeFrames, timeFrameRangesParams, calculateCustomInterval } from './components/filter/timeframe-selector/utils/timeframe-ranges';
import { GridComponent } from './components/grid/grid.component';
import { FilterName } from './components/widget/model/request/enum/filter-name';
import { RequestFilter, TimeRange } from './components/widget/model/request/request';
import { TimeSeriesRequest } from './components/widget/model/request/time-series-request';
import { Widget, isTimeSeriesWidget } from './components/widget/model/widget/widget.model';
import { GraviteeDashboardService } from './gravitee-dashboard.service';
import { DashboardCapabilities, DEFAULT_CAPABILITIES } from './models/dashboard-capabilities.model';
import { DASHBOARD_PERSISTENCE } from './models/dashboard-persistence.model';
import { Dashboard } from './models/dashboard.model';

export type SaveState = 'saving' | 'saved' | 'error';

const METADATA_DIALOG_WIDTH = '500px';

@Component({
  selector: 'gd-dashboard',
  encapsulation: ViewEncapsulation.None,
  imports: [GridComponent, GenericFilterBarComponent, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  templateUrl: './gravitee-dashboard.component.html',
  styleUrl: './gravitee-dashboard.component.scss',
})
export class GraviteeDashboardComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dashboardService = inject(GraviteeDashboardService);
  private readonly matDialog = inject(MatDialog);
  private readonly persistence = inject(DASHBOARD_PERSISTENCE, { optional: true });

  dashboard = input.required<Dashboard>();
  filters = input.required<Filter[]>();
  baseURL = input.required<string>();
  capabilities = input<DashboardCapabilities>(DEFAULT_CAPABILITIES);
  defaultPeriod = input<string>('5m');
  showTitle = input<boolean>(true);

  readonly deleteRequested = output<void>();
  readonly nameChanged = output<string>();
  readonly saveStateChange = output<SaveState>();

  private readonly localWidgets = signal<Widget[]>([]);
  private readonly localLayout = signal<Map<string, Widget['layout']>>(new Map());
  private readonly localRemovedIds = signal<Set<string>>(new Set());
  protected readonly localName = signal('');
  private readonly localLabels = signal<Record<string, string>>({});

  private readonly saveSubject = new Subject<void>();

  readonly hasMenu = computed(() => {
    const caps = this.capabilities();
    return caps.canEditMetadata || caps.canDeleteDashboard;
  });

  currentSelectedFilters = toSignal(
    this.activatedRoute.queryParams.pipe(
      map(params => {
        const filters = this.getSelectedFiltersFromQueryParams(params);
        if (!filters.some(f => f.parentKey === 'period')) {
          return [{ parentKey: 'period', value: this.defaultPeriod() }, ...filters];
        }
        return filters;
      }),
    ),
    { initialValue: [{ parentKey: 'period', value: '5m' }] as SelectedFilter[] },
  );

  private readonly refreshTrigger = signal(0);

  readonly widgetsWithFilters = computed(() => {
    this.refreshTrigger();
    return this.getUpdatedWidgetsWithFilters(this.localWidgets(), this.currentSelectedFilters());
  });

  private readonly loadedWidgets = toSignal(
    toObservable(this.widgetsWithFilters).pipe(
      switchMap(widgets => {
        const widgetObservables = widgets.map(widget => this.loadWidgetData(widget).pipe(startWith(widget), take(2)));
        return combineLatest(widgetObservables);
      }),
    ),
    { initialValue: [] as Widget[] },
  );

  readonly dashboardWidgets = computed(() => {
    const overrides = this.localLayout();
    const removed = this.localRemovedIds();
    return this.loadedWidgets()
      .filter(w => !removed.has(w.id))
      .map(w => {
        const layout = overrides.get(w.id);
        return layout ? { ...w, layout } : w;
      });
  });

  constructor() {
    // Initialise local state whenever a new dashboard is passed in
    effect(() => {
      const d = this.dashboard();
      this.localWidgets.set(d.widgets);
      this.localLayout.set(new Map());
      this.localRemovedIds.set(new Set());
      this.localName.set(d.name);
      this.localLabels.set(d.labels ?? {});
    });

    // Auto-save pipeline
    this.saveSubject
      .pipe(
        debounceTime(700),
        tap(() => this.saveStateChange.emit('saving')),
        switchMap(() => {
          if (!this.persistence) {
            return EMPTY;
          }
          const layoutMap = this.localLayout();
          const toSave: Dashboard = {
            ...this.dashboard(),
            name: this.localName(),
            labels: this.localLabels(),
            widgets: this.localWidgets()
              .filter(w => !this.localRemovedIds().has(w.id))
              .map(({ response: _, ...w }) => {
                const layout = layoutMap.get(w.id);
                return layout ? { ...w, layout } : w;
              }),
          };
          return this.persistence.update(toSave).pipe(
            tap(() => this.saveStateChange.emit('saved')),
            catchError(() => {
              this.saveStateChange.emit('error');
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe();
  }

  onWidgetsChange(widgets: Widget[]): void {
    const survivingIds = new Set(widgets.map(w => w.id));
    const newRemovedIds = new Set([
      ...this.localRemovedIds(),
      ...this.localWidgets()
        .map(w => w.id)
        .filter(id => !survivingIds.has(id)),
    ]);
    const prunedLayout = new Map([...this.localLayout().entries()].filter(([id]) => survivingIds.has(id)));
    this.localRemovedIds.set(newRemovedIds);
    this.localLayout.set(prunedLayout);
    this.saveSubject.next();
  }

  onLayoutChange(widgets: Widget[]): void {
    this.localLayout.set(new Map(widgets.map(w => [w.id, w.layout])));
    this.saveSubject.next();
  }

  openMetadataDialog(): void {
    const dialogRef = this.matDialog.open<DashboardMetadataDialogComponent, DashboardMetadataDialogData, DashboardMetadataDialogResult>(
      DashboardMetadataDialogComponent,
      {
        data: { name: this.localName(), labels: this.localLabels() },
        width: METADATA_DIALOG_WIDTH,
      },
    );

    dialogRef.afterClosed().subscribe(result => {
      if (!result) return;
      this.localName.set(result.name);
      this.localLabels.set(result.labels);
      this.nameChanged.emit(result.name);
      this.saveSubject.next();
    });
  }

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

      const finalFilters = [...(widget.request.filters ?? []), ...newFilters];

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
