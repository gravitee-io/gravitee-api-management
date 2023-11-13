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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { map, shareReplay, skip, switchMap, takeUntil, tap } from 'rxjs/operators';
import { StateParams } from '@uirouter/core';
import { StateService } from '@uirouter/angular';
import { forkJoin, of, ReplaySubject, Subject } from 'rxjs';
import * as moment from 'moment';

import { QuickFiltersStoreService } from './services';
import { LogFiltersInitialValues } from './models';

import { ApiLogsV2Service } from '../../../../services-ngx/api-logs-v2.service';
import { ApiLogsParam, ApiLogsResponse, ApiV4 } from '../../../../entities/management-api-v2';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApplicationService } from '../../../../services-ngx/application.service';
import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';

@Component({
  selector: 'api-runtime-logs',
  template: require('./api-runtime-logs.component.html'),
  styles: [require('./api-runtime-logs.component.scss')],
})
export class ApiRuntimeLogsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private api$ = this.apiService.get(this.ajsStateParams.apiId).pipe(shareReplay(1));
  apiLogsSubject$ = new ReplaySubject<ApiLogsResponse>(1);
  isMessageApi$ = this.api$.pipe(map((api: ApiV4) => api?.type === 'MESSAGE'));
  apiLogsEnabled$ = this.api$.pipe(map(ApiRuntimeLogsComponent.isLogEnabled));
  apiPlans$ = this.planService.list(this.ajsStateParams.apiId, undefined, ['PUBLISHED', 'DEPRECATED', 'CLOSED'], undefined, 1, 9999).pipe(
    map((plans) => plans.data),
    shareReplay(1),
  );
  initialValues: LogFiltersInitialValues;
  loading = true;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiLogsService: ApiLogsV2Service,
    private readonly apiService: ApiV2Service,
    private readonly applicationService: ApplicationService,
    private readonly planService: ApiPlanV2Service,
    private readonly quickFilterStore: QuickFiltersStoreService,
  ) {}

  ngOnInit(): void {
    this.initData();
    this.quickFilterStore
      .filters$()
      .pipe(
        skip(1), // skip first value from the store as the filters will be initialized and update it
        switchMap((values, index) => {
          // for the first trigger we keep the page in the URL to be sure that the user can load the data in a specific page
          const page = index === 0 ? +this.ajsStateParams.page : 1;
          return of({ values, page });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(({ values, page }) => {
        const params = this.quickFilterStore.toLogFilterQueryParam(values, page, +this.ajsStateParams.perPage);
        this.searchConnectionLogs(params);
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  paginationUpdated(event: PageEvent) {
    const logFilters = this.quickFilterStore.getFilters();
    const params = this.quickFilterStore.toLogFilterQueryParam(logFilters, event.pageIndex + 1, event.pageSize);
    this.searchConnectionLogs(params);
  }

  openLogsSettings() {
    return this.ajsState.go('management.apis.runtimeLogs-settings');
  }

  refresh() {
    this.quickFilterStore.next(this.quickFilterStore.getFilters());
  }

  searchConnectionLogs(queryParam?: ApiLogsParam) {
    this.loading = true;
    this.apiLogsService
      .searchConnectionLogs(this.ajsStateParams.apiId, queryParam)
      .pipe(
        tap((apiLogsResponse) => {
          this.apiLogsSubject$.next(apiLogsResponse);
          this.loading = false;
          this.ajsState.go('.', queryParam, { notify: false });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private static isLogEnabled = (api: ApiV4) => {
    return api.analytics.enabled && (api.analytics.logging?.mode?.endpoint === true || api.analytics.logging?.mode?.entrypoint === true);
  };

  private initData() {
    const applicationIds: string[] = this.ajsStateParams.applicationIds ? this.ajsStateParams.applicationIds.split(',') : null;
    const planIds: string[] = this.ajsStateParams.planIds ? this.ajsStateParams.planIds.split(',') : null;

    forkJoin([
      applicationIds?.length > 0 ? this.applicationService.findByIds(applicationIds, 1, applicationIds?.length ?? 10) : of(null),
      this.apiPlans$,
    ])
      .pipe(
        map(([applications, plans]) => {
          return {
            plans:
              planIds?.map((id) => {
                const plan = plans.find((p) => p.id === id);
                return { value: id, label: plan.name };
              }) ?? undefined,
            applications:
              applicationIds?.map((id) => {
                const application = applications.data.find((app) => app.id === id);
                return { value: id, label: `${application.name} ( ${application.owner?.displayName} )` };
              }) ?? undefined,
            from: this.ajsStateParams.from ? moment(this.ajsStateParams.from) : undefined,
            to: this.ajsStateParams.to ? moment(this.ajsStateParams.to) : undefined,
            methods: this.ajsStateParams.methods?.split(',') ?? undefined,
          };
        }),
      )
      .subscribe((data) => {
        this.initialValues = data;
      });
  }
}
