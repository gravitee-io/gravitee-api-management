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
import { combineLatest, EMPTY, map, of, startWith, Subject, switchMap, take } from 'rxjs';
import { catchError, debounceTime, tap } from 'rxjs/operators';

import {
  DashboardMetadataDialogComponent,
  DashboardMetadataDialogData,
  DashboardMetadataDialogResult,
} from './components/dashboard-metadata-dialog/dashboard-metadata-dialog.component';
import { GridComponent } from './components/grid/grid.component';
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
  imports: [GridComponent, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  templateUrl: './gravitee-dashboard.component.html',
  styleUrl: './gravitee-dashboard.component.scss',
})
export class GraviteeDashboardComponent {
  private readonly dashboardService = inject(GraviteeDashboardService);
  private readonly matDialog = inject(MatDialog);
  private readonly persistence = inject(DASHBOARD_PERSISTENCE, { optional: true });

  dashboard = input.required<Dashboard>();
  baseURL = input.required<string>();
  capabilities = input<DashboardCapabilities>(DEFAULT_CAPABILITIES);
  showTitle = input<boolean>(true);
  requestFilters = input<RequestFilter[]>([]);
  timeRange = input.required<TimeRange>();
  interval = input<number | undefined>(undefined);
  refreshToken = input<number>(0);

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

  readonly widgetsWithFilters = computed(() => {
    this.refreshToken();
    const widgets = this.localWidgets();
    const externalFilters = this.requestFilters();
    const tr = this.timeRange();
    const intv = this.interval();

    return widgets.map(widget => {
      if (!widget.request) return widget;

      const finalFilters = [...(widget.request.filters ?? []), ...externalFilters];

      if (isTimeSeriesWidget(widget) && intv !== undefined) {
        return {
          ...widget,
          request: { ...widget.request, timeRange: tr, interval: intv, filters: finalFilters } as TimeSeriesRequest,
          response: undefined,
        };
      }

      return {
        ...widget,
        request: { ...widget.request, timeRange: tr, filters: finalFilters },
        response: undefined,
      };
    });
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
    effect(() => {
      const d = this.dashboard();
      this.localWidgets.set(d.widgets);
      this.localLayout.set(new Map());
      this.localRemovedIds.set(new Set());
      this.localName.set(d.name);
      this.localLabels.set(d.labels ?? {});
    });

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

  private loadWidgetData(widget: Widget) {
    if (!widget.request) return of(widget);

    return this.dashboardService
      .getMetrics(this.baseURL(), widget.request.type, widget.request)
      .pipe(map(response => ({ ...widget, response }) satisfies Widget));
  }
}
