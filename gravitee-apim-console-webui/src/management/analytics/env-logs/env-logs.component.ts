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
import { forkJoin, Observable, of, EMPTY } from 'rxjs';
import { catchError, debounceTime, map, switchMap, tap } from 'rxjs/operators';

import type { EnvLog } from './models/env-log.model';

import { EnvLogsFilterBarComponent, EnvLogsFilterValues } from './components/env-logs-filter-bar/env-logs-filter-bar.component';
import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';

import { EnvironmentLogsService, EnvironmentApiLog, SearchLogsParam } from '../../../services-ngx/environment-logs.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApplicationService } from '../../../services-ngx/application.service';
import { ApiPlanV2Service } from '../../../services-ngx/api-plan-v2.service';
import { InstanceService } from '../../../services-ngx/instance.service';
import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';

/** Gravitee's built-in application ID for unauthenticated / Keyless traffic. */
const UNKNOWN_APPLICATION_ID = '1';

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
  private readonly datePipe = inject(DatePipe);

  /** Cache of resolved entity names to avoid redundant HTTP calls across page loads. */
  private readonly nameCache = new Map<string, string>();

  pagination = signal<Pagination>({ page: 1, perPage: 10, totalCount: 0 });
  filters = signal<EnvLogsFilterValues | null>(null);
  error = signal<string | null>(null);

  /** Combined search params that drive the reactive pipeline. */
  private searchParams = computed(() => ({
    pagination: this.pagination(),
    filters: this.filters(),
  }));

  private readonly logsResult$ = toObservable(this.searchParams).pipe(
    debounceTime(0),
    switchMap(({ pagination: { page, perPage }, filters }) => {
      const searchParam: SearchLogsParam = {
        page,
        perPage,
        period: filters?.period,
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
      };

      // If more-filters has explicit from/to, use them as timeRange (overrides period)
      if (filters?.more?.from && filters?.more?.to) {
        searchParam.timeRange = {
          from: filters.more.from.toISOString(),
          to: filters.more.to.toISOString(),
        };
      } else if (filters?.more?.from) {
        searchParam.timeRange = {
          from: filters.more.from.toISOString(),
          to: new Date().toISOString(),
        };
      }

      this.error.set(null);

      return this.environmentLogsService.searchLogs(searchParam).pipe(
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

          const planIdToApiId = new Map(planEntries.map(e => [e.planId, e.apiId]));

          return forkJoin({
            api: this.resolveNames(apiIds, id => this.apiV2Service.get(id).pipe(map(a => a.name))),
            app: this.resolveNames(appIds, id => this.applicationService.getById(id).pipe(map(a => a.name))),
            plan: this.resolveNames([...planIdToApiId.keys()], planId =>
              this.planService.get(planIdToApiId.get(planId), planId).pipe(map(p => p.name)),
            ),
            gateway: this.resolveNames(gatewayIds, id => this.instanceService.getByGatewayId(id).pipe(map(i => i.hostname ?? id))),
          }).pipe(map((resolved: ResolvedNames) => ({ response, resolved })));
        }),
        catchError(err => {
          const message =
            err instanceof HttpErrorResponse
              ? `Request failed: ${err.status} ${err.statusText}`
              : 'An unexpected error occurred while loading logs.';
          this.error.set(message);
          return EMPTY;
        }),
      );
    }),
  );

  private readonly logsResult = toSignal(this.logsResult$);

  logs = computed(() => this.logsResult()?.response.data.map(log => this.mapToEnvLog(log, this.logsResult()!.resolved)) ?? []);
  isLoading = computed(() => this.logsResult() === undefined);
  /** Merges backend totalCount into the pagination signal for the table wrapper. */
  paginationWithTotal = computed(() => ({
    ...this.pagination(),
    totalCount: this.logsResult()?.response.pagination.totalCount ?? 0,
  }));

  onRefresh() {
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  onFiltersChanged(filters: EnvLogsFilterValues) {
    this.filters.set(filters);
    // Reset to page 1 when filters change
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
  }

  private resolveNames(ids: string[], fetcher: (id: string) => Observable<string>): Observable<Record<string, string>> {
    if (ids.length === 0) return of({});

    // Return cached names immediately, only fetch uncached ones
    const cached: Record<string, string> = {};
    const uncachedIds: string[] = [];
    for (const id of ids) {
      const name = this.nameCache.get(id);
      if (name) {
        cached[id] = name;
      } else {
        uncachedIds.push(id);
      }
    }

    if (uncachedIds.length === 0) return of(cached);

    return forkJoin(
      Object.fromEntries(
        uncachedIds.map(id => [
          id,
          fetcher(id).pipe(
            tap(name => this.nameCache.set(id, name)),
            catchError(err => {
              // Re-throw auth errors so the user sees permission issues
              if (err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403)) {
                throw err;
              }
              return of(id);
            }),
          ),
        ]),
      ),
    ).pipe(map(fetched => ({ ...cached, ...fetched })));
  }

  private mapToEnvLog(log: EnvironmentApiLog, names: ResolvedNames): EnvLog {
    const planName = log.plan?.id ? names.plan[log.plan.id] : undefined;
    const appName = log.application?.id ? names.app[log.application.id] : undefined;

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
