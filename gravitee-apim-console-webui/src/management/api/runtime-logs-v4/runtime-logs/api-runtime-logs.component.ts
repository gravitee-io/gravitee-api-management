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
import { map, shareReplay, takeUntil, tap } from 'rxjs/operators';
import { StateParams } from '@uirouter/core';
import { StateService } from '@uirouter/angular';
import * as moment from 'moment';
import { ReplaySubject, Subject } from 'rxjs';

import DurationConstructor = moment.unitOfTime.DurationConstructor;

import { LogFilters, LogFiltersInitialValues, SimpleFilter } from './components';

import { ApiLogsV2Service } from '../../../../services-ngx/api-logs-v2.service';
import { ApiLogsResponse, ApiV4 } from '../../../../entities/management-api-v2';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApplicationService } from '../../../../services-ngx/application.service';

@Component({
  selector: 'api-runtime-logs',
  template: require('./api-runtime-logs.component.html'),
  styles: [require('./api-runtime-logs.component.scss')],
})
export class ApiRuntimeLogsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private api$ = this.apiService.get(this.ajsStateParams.apiId).pipe(shareReplay(1));
  private currentFilters: Record<string, number | string>;
  apiLogsSubject$ = new ReplaySubject<ApiLogsResponse>(1);
  isMessageApi$ = this.api$.pipe(map((api: ApiV4) => api?.type === 'MESSAGE'));
  apiLogsEnabled$ = this.api$.pipe(map(ApiRuntimeLogsComponent.isLogEnabled));
  initialValues: LogFiltersInitialValues;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiLogsService: ApiLogsV2Service,
    private readonly apiService: ApiV2Service,
    private readonly applicationService: ApplicationService,
  ) {}

  ngOnInit(): void {
    this.currentFilters = {
      page: +this.ajsStateParams.page,
      perPage: +this.ajsStateParams.perPage,
      from: +this.ajsStateParams.from,
      to: +this.ajsStateParams.to,
      applicationIds: this.ajsStateParams.applicationIds,
    };
    this.apiLogsService
      .searchConnectionLogs(this.ajsStateParams.apiId, { ...this.currentFilters })
      .pipe(
        tap((apiLogsResponse) => {
          this.apiLogsSubject$.next(apiLogsResponse);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.initFilters();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  paginationUpdated(event: PageEvent) {
    this.currentFilters = {
      page: event.pageIndex + 1,
      perPage: event.pageSize,
      ...(!!this.currentFilters.from && { from: +this.currentFilters.from }),
      ...(!!this.currentFilters.to && { to: +this.currentFilters.to }),
      ...(!!this.currentFilters.applicationIds && { applicationIds: this.currentFilters.applicationIds }),
    };

    this.apiLogsService
      .searchConnectionLogs(this.ajsStateParams.apiId, { ...this.currentFilters })
      .pipe(
        tap((apiLogsResponse) => {
          this.apiLogsSubject$.next(apiLogsResponse);
          this.ajsState.go('.', { ...this.currentFilters }, { notify: false });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  openLogsSettings() {
    return this.ajsState.go('management.apis.runtimeLogs-settings');
  }

  private static isLogEnabled = (api: ApiV4) => {
    return api.analytics.enabled && (api.analytics.logging?.mode?.endpoint === true || api.analytics.logging?.mode?.entrypoint === true);
  };

  applyFilter(logFilter: LogFilters) {
    const periodFilter = this.preparePeriodFilter(logFilter.period);
    this.currentFilters = {
      page: 1,
      perPage: +this.ajsStateParams.perPage,
      from: periodFilter?.from ? periodFilter.from : null,
      to: periodFilter?.to ? periodFilter.to : null,
      applicationIds: logFilter.applications?.length > 0 ? logFilter.applications?.map((app) => app.value).join(',') : null,
    };

    this.apiLogsService
      .searchConnectionLogs(this.ajsStateParams.apiId, this.currentFilters)
      .pipe(
        tap((apiLogsResponse) => {
          this.apiLogsSubject$.next(apiLogsResponse);
          this.ajsState.go('.', { ...this.currentFilters }, { notify: false });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private preparePeriodFilter(period: SimpleFilter): { from: number; to: number } {
    if (period.value === '0') {
      return null;
    }
    const now = moment();
    const operation = period.value.charAt(0);
    const timeUnit: DurationConstructor = period.value.charAt(period.value.length - 1) as DurationConstructor;
    const duration = Number(period.value.substring(1, period.value.length - 1));
    let from;
    if (operation === '-') {
      from = now.clone().subtract(duration, timeUnit);
    } else {
      from = now.clone().add(duration, timeUnit);
    }
    return { from: from.valueOf(), to: now.valueOf() };
  }

  private initFilters() {
    const applicationIds: string[] = this.ajsStateParams.applicationIds ? this.ajsStateParams.applicationIds.split(',') : null;

    this.applicationService
      .findByIds(applicationIds, 1, applicationIds?.length ?? 10)
      .pipe(
        map((applications) => ({
          applications:
            applicationIds?.map((id) => {
              const application = applications.data.find((app) => app.id === id);
              return {
                value: id,
                label: `${application.name} ( ${application.owner?.displayName} )`,
              };
            }) ?? undefined,
        })),
      )
      .subscribe((data) => {
        this.initialValues = data;
      });
  }
}
