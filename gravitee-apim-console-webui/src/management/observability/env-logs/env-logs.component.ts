/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import {
  AddFilterDialogComponent,
  AddFilterDialogData,
  customTimeFrames,
  DynamicFilterBarComponent,
  FilterCondition,
  provideFilterDefinitions,
  provideFilterValues,
  TimeframeSelectorComponent,
  timeFrames,
} from '@gravitee/gravitee-dashboard';

import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, Injector, signal } from '@angular/core';
import { toObservable, toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY } from 'rxjs';
import { catchError, debounceTime, switchMap, tap } from 'rxjs/operators';

import type { EnvLog } from './models/env-log.model';

import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';

import { DashboardFiltersStore } from '../dashboards/ui/dashboard-viewer/dashboard-filters.store';
import { FilterLabelResolver } from '../dashboards/ui/dashboard-viewer/filter-label.resolver';
import { ObservabilityFiltersApiService } from '../data-access/observability-filters-api.service';
import { EnvironmentLogsService, EnvironmentApiLog, SearchLogsParam, LogApiType } from '../../../services-ngx/environment-logs.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';
import { GioHeaderComponent } from '../../../shared/components/gio-header/gio-header.component';

const EMPTY_FIELD = '—';
const DEFAULT_PER_PAGE = 10;

const API_TYPE_LABELS: Record<LogApiType, string> = {
  HTTP_PROXY: 'HTTP',
  LLM_PROXY: 'LLM',
  MCP_PROXY: 'MCP',
};

@Component({
  selector: 'env-logs',
  templateUrl: './env-logs.component.html',
  styleUrl: './env-logs.component.scss',
  imports: [
    EnvLogsTableComponent,
    DynamicFilterBarComponent,
    TimeframeSelectorComponent,
    ReactiveFormsModule,
    MatCardModule,
    GioBannerModule,
    GioHeaderComponent,
  ],
  providers: [
    DatePipe,
    DashboardFiltersStore,
    ObservabilityFiltersApiService,
    FilterLabelResolver,
    provideFilterDefinitions(ObservabilityFiltersApiService),
    provideFilterValues(ObservabilityFiltersApiService),
  ],
})
export class EnvLogsComponent {
  private readonly environmentLogsService = inject(EnvironmentLogsService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly datePipe = inject(DatePipe);
  private readonly dialog = inject(MatDialog);
  // Required so dialogs can resolve DashboardFiltersStore from this component's injector scope
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);
  /** Prevents opening a second filter dialog while one is already active. */
  private dialogOpen = false;

  protected readonly filtersStore = inject(DashboardFiltersStore);
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];

  protected pagination = signal<Pagination>({ page: 1, perPage: DEFAULT_PER_PAGE, totalCount: 0 });
  protected loading = signal(true);
  protected error = signal<string | null>(null);

  private readonly searchParams = computed(() => ({
    pagination: this.pagination(),
    requestFilters: this.filtersStore.requestFilters(),
    timeRange: this.filtersStore.timeRange(),
  }));

  private readonly logsResult$ = toObservable(this.searchParams).pipe(
    debounceTime(0), // batch synchronous signal updates into a single emission
    switchMap(params => this.fetchLogs(params)),
  );

  private readonly logsResult = toSignal(this.logsResult$);

  protected logs = computed(() => {
    const result = this.logsResult();
    return result?.data.map(log => this.mapToEnvLog(log)) ?? [];
  });

  protected paginationWithTotal = computed(() => ({
    ...this.pagination(),
    totalCount: this.logsResult()?.pagination.totalCount ?? 0,
  }));

  protected onRefresh() {
    // Spreading into a new object always produces a new signal reference, which marks
    // searchParams as dirty and triggers a re-fetch even when the page is already 1.
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  protected onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
  }

  protected openAddFilter(): void {
    this.openFilterDialog(null);
  }

  protected openEditFilter(index: number, condition: FilterCondition): void {
    this.openFilterDialog({ index, condition });
  }

  /**
   * Wrapper called from the template for single-chip removal.
   * Resets to page 1 synchronously before the store mutation so that
   * debounceTime(0) in logsResult$ collapses both signal writes into one fetch.
   */
  protected onFilterRemoved(index: number): void {
    this.resetToFirstPage();
    this.filtersStore.remove(index);
  }

  /**
   * Wrapper called from the template for "clear all" action.
   * Same page-reset contract as onFilterRemoved.
   */
  protected onFilterCleared(): void {
    this.resetToFirstPage();
    this.filtersStore.clear();
  }

  private openFilterDialog(edit: { index: number; condition: FilterCondition } | null): void {
    if (this.dialogOpen) return;
    this.dialogOpen = true;

    const { from, to } = this.filtersStore.timeRangeEpoch();
    const data: AddFilterDialogData = edit
      ? { existingCondition: edit.condition, timeFrom: from, timeTo: to }
      : { timeFrom: from, timeTo: to };

    this.dialog
      .open<AddFilterDialogComponent, AddFilterDialogData, FilterCondition>(AddFilterDialogComponent, {
        data,
        injector: this.injector,
        autoFocus: 'dialog',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        this.dialogOpen = false;
        if (!result) return;
        // Reset synchronously before the store mutation so debounceTime(0)
        // collapses both signal writes into a single fetch on page 1.
        this.resetToFirstPage();
        if (edit) {
          this.filtersStore.edit(edit.index, result);
        } else {
          this.filtersStore.add(result);
        }
      });
  }

  /** Resets pagination to page 1 without changing perPage. */
  private resetToFirstPage(): void {
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  private fetchLogs(params: {
    pagination: Pagination;
    requestFilters: ReturnType<DashboardFiltersStore['requestFilters']>;
    timeRange: { from: string; to: string };
  }) {
    this.loading.set(true);
    this.error.set(null);

    return this.environmentLogsService.searchLogs(this.buildSearchParam(params)).pipe(
      tap(() => this.loading.set(false)),
      catchError(err => {
        this.loading.set(false);
        const message =
          err instanceof HttpErrorResponse
            ? `Request failed: ${err.status} ${err.statusText}`
            : 'An unexpected error occurred while loading logs.';
        this.error.set(message);
        this.snackBarService.error(message);
        return EMPTY;
      }),
    );
  }

  private buildSearchParam(params: {
    pagination: Pagination;
    requestFilters: ReturnType<DashboardFiltersStore['requestFilters']>;
    timeRange: { from: string; to: string };
  }): SearchLogsParam {
    const { page, perPage } = params.pagination;
    // RequestFilter.value is always string[] for IN filters returned by the store;
    // scalar (EQ) filters also arrive as string[] with a single element.
    const filterMap = new Map<string, string[]>(
      params.requestFilters.map(f => [f.name as string, Array.isArray(f.value) ? f.value : [f.value]]),
    );

    return {
      page,
      perPage,
      timeRange: params.timeRange,
      apiIds: filterMap.get('API'),
      applicationIds: filterMap.get('APPLICATION'),
      planIds: filterMap.get('PLAN'),
      methods: filterMap.get('HTTP_METHOD'),
      statuses: filterMap.get('HTTP_STATUS')?.map(Number),
      entrypoints: filterMap.get('ENTRYPOINT'),
      errorKeys: filterMap.get('ERROR_KEY'),
      apiProductIds: filterMap.get('API_PRODUCT'),
      bodyText: filterMap.get('PAYLOAD')?.[0],
    };
  }

  private mapToEnvLog(log: EnvironmentApiLog): EnvLog {
    return {
      id: log.id,
      timestamp: this.datePipe.transform(log.timestamp, 'medium') ?? log.timestamp,
      api: log.apiName ?? log.apiId,
      apiId: log.apiId,
      apiType: log.apiType ? (API_TYPE_LABELS[log.apiType] ?? log.apiType) : undefined,
      apiProductName: log.apiProductName ?? undefined,
      application: log.application?.name ?? log.application?.id ?? EMPTY_FIELD,
      method: log.method ?? EMPTY_FIELD,
      path: log.uri ?? EMPTY_FIELD,
      status: log.status,
      responseTime: log.gatewayResponseTime == null ? EMPTY_FIELD : `${log.gatewayResponseTime} ms`,
      gateway: log.gateway ?? undefined,
      plan: log.plan?.name ? { name: log.plan.name } : undefined,
      requestEnded: log.requestEnded,
      errorKey: log.errorKey,
      warnings: log.warnings?.map(w => ({ key: w.key ?? '' })),
    };
  }
}
