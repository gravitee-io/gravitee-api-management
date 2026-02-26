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

import { EnvLogsFilterBarComponent } from './components/env-logs-filter-bar/env-logs-filter-bar.component';
import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';

import { EnvironmentLogsService, EnvironmentApiLog } from '../../../services-ngx/environment-logs.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';

/** Gravitee's built-in sentinel ID for unauthenticated / Keyless traffic. */
const UNKNOWN_APPLICATION_ID = '1';
const UNKNOWN_APPLICATION_LABEL = 'Default application';

@Component({
  selector: 'env-logs',
  templateUrl: './env-logs.component.html',
  styleUrl: './env-logs.component.scss',
  imports: [EnvLogsTableComponent, EnvLogsFilterBarComponent, MatCardModule, GioBannerModule, DatePipe],
  providers: [DatePipe],
  standalone: true,
})
export class EnvLogsComponent {
  private readonly environmentLogsService = inject(EnvironmentLogsService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly datePipe = inject(DatePipe);

  pagination = signal<Pagination>({ page: 1, perPage: 10, totalCount: 0 });
  loading = signal(true);
  error = signal<string | null>(null);

  private readonly logsResult$ = toObservable(this.pagination).pipe(
    debounceTime(0),
    tap(() => {
      this.loading.set(true);
      this.error.set(null);
    }),
    switchMap(({ page, perPage }) =>
      this.environmentLogsService.searchLogs({ page, perPage }).pipe(
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
      ),
    ),
  );

  private readonly logsResult = toSignal(this.logsResult$);

  logs = computed(() => {
    const result = this.logsResult();
    return result?.data.map(log => this.mapToEnvLog(log)) ?? [];
  });
  /** Merges backend totalCount into the pagination signal for the table wrapper. */
  paginationWithTotal = computed(() => ({
    ...this.pagination(),
    totalCount: this.logsResult()?.pagination.totalCount ?? 0,
  }));

  onRefresh() {
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
  }

  private mapToEnvLog(log: EnvironmentApiLog): EnvLog {
    const appName = log.application?.id
      ? log.application.id === UNKNOWN_APPLICATION_ID
        ? UNKNOWN_APPLICATION_LABEL
        : (log.application.name ?? log.application.id)
      : undefined;

    return {
      id: log.id,
      timestamp: this.datePipe.transform(log.timestamp, 'medium') ?? log.timestamp,
      api: log.apiName ?? log.apiId,
      apiId: log.apiId,
      application: appName ?? '—',
      method: log.method ?? '—',
      path: log.uri ?? '—',
      status: log.status,
      responseTime: log.gatewayResponseTime != null ? `${log.gatewayResponseTime} ms` : '—',
      gateway: log.gateway ?? undefined,
      plan: log.plan?.name ? { name: log.plan.name } : undefined,
      requestEnded: log.requestEnded,
      errorKey: log.errorKey,
      warnings: log.warnings?.map(w => ({ key: w.key ?? '' })),
    };
  }
}
