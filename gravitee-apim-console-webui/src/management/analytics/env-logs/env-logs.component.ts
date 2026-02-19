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
import { EMPTY, forkJoin, Observable, of } from 'rxjs';
import { catchError, debounceTime, map, switchMap, tap } from 'rxjs/operators';

import type { EnvLog } from './models/env-log.model';

import { EnvLogsFilterBarComponent } from './components/env-logs-filter-bar/env-logs-filter-bar.component';
import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';

import { EnvironmentLogsService, EnvironmentApiLog } from '../../../services-ngx/environment-logs.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApplicationService } from '../../../services-ngx/application.service';
import { ApiPlanV2Service } from '../../../services-ngx/api-plan-v2.service';
import { InstanceService } from '../../../services-ngx/instance.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';

/** Gravitee's built-in sentinel ID for unauthenticated / Keyless traffic. */
const UNKNOWN_APPLICATION_ID = '1';
const UNKNOWN_APPLICATION_LABEL = 'Unknown application (keyless)';

type ResolvedNames = {
  api: Record<string, string>;
  app: Record<string, string>;
  plan: Record<string, string>;
  gateway: Record<string, string>;
};

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
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly applicationService = inject(ApplicationService);
  private readonly planService = inject(ApiPlanV2Service);
  private readonly instanceService = inject(InstanceService);
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
        switchMap(response => {
          const apiIds = [...new Set(response.data.map(log => log.apiId).filter(Boolean))];
          const appIds = [
            ...new Set(response.data.map(log => log.application?.id).filter(id => Boolean(id) && id !== UNKNOWN_APPLICATION_ID)),
          ];
          const gatewayIds = [...new Set(response.data.map(log => log.gateway).filter(Boolean))];
          const planEntries = [
            ...new Map(
              response.data
                .filter(log => log.plan?.id && log.apiId)
                .map(log => [`${log.apiId}|${log.plan.id}`, { apiId: log.apiId, planId: log.plan.id }]),
            ).values(),
          ];

          const planCompositeKeys = planEntries.map(e => `${e.apiId}|${e.planId}`);

          // TODO: Move name resolution to the backend (SearchEnvironmentLogsUseCase) to match proxy logs pattern.
          // Currently resolving names via N+1 frontend calls; the backend should return enriched objects
          // with names (like ConnectionLog does for API runtime logs). See ApplicationMetadataProvider for reference.
          return forkJoin({
            api: this.resolveNames(apiIds, id => this.apiV2Service.get(id).pipe(map(a => a.name))),
            app: this.resolveNames(appIds, id => this.applicationService.getById(id).pipe(map(a => a.name))),
            plan: this.resolveNames(planCompositeKeys, compositeKey => {
              const [apiId, planId] = compositeKey.split('|');
              return this.planService.get(apiId, planId).pipe(map(p => p.name));
            }),
            gateway: this.resolveNames(gatewayIds, id => this.instanceService.getByGatewayId(id).pipe(map(i => i.hostname ?? id))),
          }).pipe(map((resolved: ResolvedNames) => ({ response, resolved })));
        }),
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
    return result?.response.data.map(log => this.mapToEnvLog(log, result.resolved)) ?? [];
  });
  /** Merges backend totalCount into the pagination signal for the table wrapper. */
  paginationWithTotal = computed(() => ({
    ...this.pagination(),
    totalCount: this.logsResult()?.response.pagination.totalCount ?? 0,
  }));

  onRefresh() {
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
  }

  private resolveNames(ids: string[], fetcher: (id: string) => Observable<string>): Observable<Record<string, string>> {
    if (ids.length === 0) return of({});

    return forkJoin(
      Object.fromEntries(
        ids.map(id => [
          id,
          fetcher(id).pipe(
            catchError(err => {
              if (err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403)) {
                throw err;
              }
              return of(id);
            }),
          ),
        ]),
      ),
    );
  }

  private mapToEnvLog(log: EnvironmentApiLog, names: ResolvedNames): EnvLog {
    const planKey = log.plan?.id && log.apiId ? `${log.apiId}|${log.plan.id}` : undefined;
    const planName = planKey ? names.plan[planKey] : undefined;
    const appName = log.application?.id
      ? log.application.id === UNKNOWN_APPLICATION_ID
        ? UNKNOWN_APPLICATION_LABEL
        : names.app[log.application.id]
      : undefined;

    return {
      id: log.id,
      timestamp: this.datePipe.transform(log.timestamp, 'medium') ?? log.timestamp,
      api: names.api[log.apiId] ?? log.apiId,
      apiId: log.apiId,
      application: appName ?? '—',
      method: log.method ?? '—',
      path: log.uri ?? '—',
      status: log.status,
      responseTime: log.gatewayResponseTime != null ? `${log.gatewayResponseTime} ms` : '—',
      gateway: log.gateway ? (names.gateway[log.gateway] ?? log.gateway) : undefined,
      plan: planName ? { name: planName } : undefined,
      requestEnded: log.requestEnded,
      errorKey: log.errorKey,
      warnings: log.warnings?.map(w => ({ key: w.key ?? '' })),
    };
  }
}
