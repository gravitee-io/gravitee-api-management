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

import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { EMPTY } from 'rxjs';
import { catchError, debounceTime, switchMap, tap } from 'rxjs/operators';

import type { EnvLog } from './models/env-log.model';

import { EnvLogsFilterBarComponent, EnvLogsFilterValues } from './components/env-logs-filter-bar/env-logs-filter-bar.component';
import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';

import { EnvironmentLogsService, EnvironmentApiLog, SearchLogsParam } from '../../../services-ngx/environment-logs.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';
import { GioHeaderComponent } from '../../../shared/components/gio-header/gio-header.component';

const EMPTY_FIELD = '—';

@Component({
  selector: 'env-logs',
  templateUrl: './env-logs.component.html',
  styleUrl: './env-logs.component.scss',
  imports: [EnvLogsTableComponent, EnvLogsFilterBarComponent, MatCardModule, GioBannerModule, DatePipe, GioHeaderComponent],
  providers: [DatePipe],
  standalone: true,
})
export class EnvLogsComponent {
  private readonly environmentLogsService = inject(EnvironmentLogsService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly datePipe = inject(DatePipe);

  pagination = signal<Pagination>({ page: 1, perPage: 10, totalCount: 0 });
  filters = signal<EnvLogsFilterValues | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  private readonly searchParams = computed(() => ({
    pagination: this.pagination(),
    filters: this.filters(),
  }));

  private readonly logsResult$ = toObservable(this.searchParams).pipe(
    debounceTime(0),
    switchMap(params => this.fetchLogs(params)),
  );

  private readonly logsResult = toSignal(this.logsResult$);

  logs = computed(() => {
    const result = this.logsResult();
    return result?.data.map(log => this.mapToEnvLog(log)) ?? [];
  });

  paginationWithTotal = computed(() => ({
    ...this.pagination(),
    totalCount: this.logsResult()?.pagination.totalCount ?? 0,
  }));

  onRefresh() {
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  onFiltersChanged(filters: EnvLogsFilterValues) {
    this.filters.set(filters);
    this.pagination.update(prev => (prev.page === 1 ? prev : { ...prev, page: 1 }));
  }

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
  }

  private fetchLogs(params: { pagination: Pagination; filters: EnvLogsFilterValues | null }) {
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

  private buildSearchParam(params: { pagination: Pagination; filters: EnvLogsFilterValues | null }): SearchLogsParam {
    const { page, perPage } = params.pagination;
    const filters = params.filters;
    return {
      page,
      perPage,
      period: filters?.period,
      from: filters?.more?.from?.toISOString(),
      to: filters?.more?.to?.toISOString(),
      apiIds: filters?.apiIds?.length ? filters.apiIds : undefined,
      applicationIds: filters?.applicationIds?.length ? filters.applicationIds : undefined,
      planIds: filters?.more?.plans?.length ? filters.more.plans : undefined,
      methods: filters?.more?.methods?.length ? filters.more.methods : undefined,
      statuses: filters?.more?.statuses?.size ? [...filters.more.statuses] : undefined,
      entrypoints: filters?.more?.entrypoints?.length ? filters.more.entrypoints : undefined,
      transactionId: filters?.more?.transactionId ?? undefined,
      requestId: filters?.more?.requestId ?? undefined,
      uri: filters?.more?.uri ?? undefined,
      responseTime: filters?.more?.responseTime ?? undefined,
      errorKeys: filters?.more?.errorKeys?.length ? filters.more.errorKeys : undefined,
    };
  }

  private mapToEnvLog(log: EnvironmentApiLog): EnvLog {
    return {
      id: log.id,
      timestamp: this.datePipe.transform(log.timestamp, 'medium') ?? log.timestamp,
      api: log.apiName ?? log.apiId,
      apiId: log.apiId,
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
