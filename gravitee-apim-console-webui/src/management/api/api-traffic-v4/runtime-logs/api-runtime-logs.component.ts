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

import { Component, DestroyRef, inject, OnInit, Signal } from '@angular/core';
import { distinctUntilChanged, map, shareReplay, skip, switchMap, tap } from 'rxjs/operators';
import { forkJoin, of, ReplaySubject, Subject } from 'rxjs';
import moment from 'moment';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { QuickFiltersStoreService } from './services';
import { LogFiltersInitialValues, PERIODS } from './models';

import { ApiLogsV2Service } from '../../../../services-ngx/api-logs-v2.service';
import { ApiLogsParam, ApiLogsResponse, ApiType, ApiV4 } from '../../../../entities/management-api-v2';
import { ApplicationService } from '../../../../services-ngx/application.service';
import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { GioTableWrapperPagination } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

@Component({
  selector: 'api-runtime-logs',
  templateUrl: './api-runtime-logs.component.html',
  styleUrls: ['./api-runtime-logs.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private apiLogsService = inject(ApiLogsV2Service);
  private applicationService = inject(ApplicationService);
  private planService = inject(ApiPlanV2Service);
  private quickFilterStore = inject(QuickFiltersStoreService);
  private connectorPluginsService = inject(ConnectorPluginsV2Service);
  private apiService = inject(ApiV2Service);
  private api$ = this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(shareReplay(1));

  isReportingDisabled$ = this.api$.pipe(
    map((api: ApiV4) => !api.analytics.enabled || (!api.analytics.logging?.mode?.endpoint && !api.analytics.logging?.mode?.entrypoint)),
  );
  apiLogsSubject$ = new ReplaySubject<ApiLogsResponse>(1);
  apiPlans$ = this.planService
    .list(this.activatedRoute.snapshot.params.apiId, undefined, ['PUBLISHED', 'DEPRECATED', 'CLOSED'], undefined, undefined, 1, 9999)
    .pipe(
      map(plans => plans.data),
      shareReplay(1),
    );
  entrypoints$ = this.connectorPluginsService.listEntrypointPlugins().pipe(
    map(plugins => {
      return plugins.map(plugin => {
        return { id: plugin.id, name: plugin.name };
      });
    }),
  );
  private readonly errorKeysSubject$ = new ReplaySubject<string[]>(1);
  errorKeys$ = this.errorKeysSubject$.asObservable();
  private readonly refetchErrorKeys$ = new Subject<{ from?: number; to?: number }>();
  apiType: Signal<ApiType> = toSignal(this.api$.pipe(map((api: ApiV4) => api.type)));
  initialValues: LogFiltersInitialValues;
  loading = true;

  ngOnInit(): void {
    this.initData();
    this.refetchErrorKeys$
      .pipe(
        switchMap(({ from, to }) =>
          this.apiLogsService.searchErrorKeys(this.activatedRoute.snapshot.params.apiId, from, to).pipe(map(keys => keys.filter(Boolean))),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(keys => this.errorKeysSubject$.next(keys));

    const { from: initialFrom, to: initialTo } = this.quickFilterStore.toLogFilterQueryParam(this.quickFilterStore.getFilters(), 1, 10);
    this.refetchErrorKeys$.next({ from: initialFrom ?? undefined, to: initialTo ?? undefined });

    this.quickFilterStore
      .filters$()
      .pipe(
        skip(1),
        distinctUntilChanged((a, b) => a.from === b.from && a.to === b.to && a.period?.value === b.period?.value),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(filters => {
        const { from, to } = this.quickFilterStore.toLogFilterQueryParam(filters, 1, 10);
        this.refetchErrorKeys$.next({ from: from ?? undefined, to: to ?? undefined });
      });

    this.quickFilterStore
      .filters$()
      .pipe(
        skip(1), // skip first value from the store as the filters will be initialized and update it
        switchMap((values, index) => {
          // for the first trigger we keep the page in the URL to be sure that the user can load the data in a specific page
          const page = index === 0 ? +(this.activatedRoute.snapshot.queryParams.page ?? 1) : 1;
          return of({ values, page });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ values, page }) => {
        const params = this.quickFilterStore.toLogFilterQueryParam(values, page, +(this.activatedRoute.snapshot.queryParams.perPage ?? 10));
        this.searchConnectionLogs(params);
      });
  }

  paginationUpdated(event: GioTableWrapperPagination) {
    const logFilters = this.quickFilterStore.getFilters();
    const params = this.quickFilterStore.toLogFilterQueryParam(logFilters, event.index, event.size);
    this.searchConnectionLogs(params);
  }

  refresh() {
    this.quickFilterStore.next(this.quickFilterStore.getFilters());
  }

  searchConnectionLogs(queryParam?: ApiLogsParam) {
    this.loading = true;
    this.apiLogsService
      .searchConnectionLogs(this.activatedRoute.snapshot.params.apiId, queryParam)
      .pipe(
        tap(apiLogsResponse => {
          this.apiLogsSubject$.next(apiLogsResponse);
          this.loading = false;
          this.router.navigate(['.'], {
            relativeTo: this.activatedRoute,
            queryParams: queryParam,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private initData() {
    const qp = this.activatedRoute.snapshot.queryParams;
    const applicationIds: string[] = qp?.applicationIds ? qp.applicationIds.split(',') : null;
    const planIds: string[] = qp?.planIds ? qp.planIds.split(',') : null;
    const statuses: Set<number> = qp?.statuses ? new Set(qp.statuses.split(',').map(Number)) : null;
    const period = PERIODS.find(p => p.value === qp?.period) ?? undefined;
    const hasRelativePeriod = period && period.value !== '0';

    forkJoin([
      applicationIds?.length > 0 ? this.applicationService.findByIds(applicationIds, 1, applicationIds?.length ?? 10) : of(null),
      this.apiPlans$,
    ])
      .pipe(
        map(([applications, plans]) => {
          return {
            period,
            plans:
              planIds?.map(id => {
                const plan = plans.find(p => p.id === id);
                return { value: id, label: plan.name };
              }) ?? undefined,
            applications:
              applicationIds?.map(id => {
                const application = applications.data.find(app => app.id === id);
                return { value: id, label: `${application.name} ( ${application.owner?.displayName} )` };
              }) ?? undefined,
            from: !hasRelativePeriod && qp?.from ? moment(Number(qp.from)) : undefined,
            to: !hasRelativePeriod && qp?.to ? moment(Number(qp.to)) : undefined,
            methods: qp?.methods?.split(',') ?? undefined,
            mcpMethods: qp?.mcpMethods?.split(',') ?? undefined,
            statuses: statuses?.size > 0 ? statuses : undefined,
            entrypoints: qp?.entrypointIds?.split(',') ?? undefined,
            errorKeys: qp?.errorKeys?.split(',') ?? undefined,
          };
        }),
      )
      .subscribe(data => {
        this.initialValues = data;
      });
  }
}
