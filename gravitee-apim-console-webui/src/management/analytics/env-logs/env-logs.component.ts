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
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { forkJoin, from, Observable, of } from 'rxjs';
import { catchError, map, mergeMap, reduce, switchMap, tap } from 'rxjs/operators';

import type { EnvLog } from './models/env-log.model';

import { EnvLogsFilterBarComponent } from './components/env-logs-filter-bar/env-logs-filter-bar.component';
import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';

import { EnvironmentLogsService, EnvironmentApiLog } from '../../../services-ngx/environment-logs.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApplicationService } from '../../../services-ngx/application.service';
import { ApiPlanV2Service } from '../../../services-ngx/api-plan-v2.service';
import { InstanceService } from '../../../services-ngx/instance.service';
import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';

/** Gravitee's built-in application ID for unauthenticated / Keyless traffic. */
const UNKNOWN_APPLICATION_ID = '1';

/** Max concurrent HTTP requests for name resolution to avoid browser connection saturation. */
const RESOLVE_CONCURRENCY = 6;

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
  imports: [EnvLogsTableComponent, EnvLogsFilterBarComponent, MatCardModule, GioBannerModule],
  standalone: true,
})
export class EnvLogsComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly environmentLogsService = inject(EnvironmentLogsService);
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly applicationService = inject(ApplicationService);
  private readonly planService = inject(ApiPlanV2Service);
  private readonly instanceService = inject(InstanceService);
  private readonly datePipe = new DatePipe('en-US');

  logs = signal<EnvLog[]>([]);
  isLoading = signal(true);
  pagination = signal<Pagination>({ page: 1, perPage: 10, totalCount: 0 });

  ngOnInit() {
    this.refresh();
  }

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
    this.refresh();
  }

  refresh() {
    this.isLoading.set(true);
    const currentPagination = this.pagination();

    this.environmentLogsService
      .searchLogs({ page: currentPagination.page, perPage: currentPagination.perPage })
      .pipe(
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
        tap(({ response, resolved }) => {
          this.logs.set(response.data.map(log => this.mapToEnvLog(log, resolved)));
          this.pagination.update(prev => ({ ...prev, totalCount: response.pagination.totalCount }));
          this.isLoading.set(false);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private resolveNames(ids: string[], fetcher: (id: string) => Observable<string>): Observable<Record<string, string>> {
    if (ids.length === 0) return of({});

    return from(ids).pipe(
      mergeMap(
        id =>
          fetcher(id).pipe(
            catchError(() => of(id)),
            map(name => [id, name] as const),
          ),
        RESOLVE_CONCURRENCY,
      ),
      reduce((acc, [id, name]) => ({ ...acc, [id]: name }), {} as Record<string, string>),
    );
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
