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
import { Component, inject } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { BehaviorSubject, combineLatest, Observable, of, switchMap } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { catchError, map, startWith } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { toNumber } from 'lodash';

import {
  AnalyticsRequestStats,
  ApiAnalyticsRequestStatsComponent,
} from '../components/api-analytics-requests-stats/api-analytics-request-stats.component';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { onlyApiV4Filter } from '../../../../../util/apiFilter.operator';
import { AnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount';
import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
import { AnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
import {
  ApiAnalyticsResponseStatusRanges,
  ApiAnalyticsResponseStatusRangesComponent,
} from '../../../../../shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component';
import { AnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import {
  ApiAnalyticsResponseStatusOvertime,
  ApiAnalyticsResponseStatusOvertimeComponent,
} from '../components/api-analytics-response-status-overtime/api-analytics-response-status-overtime.component';
import {
  ApiAnalyticsResponseTimeOverTimeComponent,
  ApiAnalyticsResponseTimeOverTimeComponentInput, ResponseTimeIsLoading
} from '../components/api-analytics-response-time-over-time/api-analytics-response-time-over-time.component';
import { TimeRangeParams } from '../../../../home/components/gio-quick-time-range/gio-quick-time-range.component';
import { GioQuickTimeRangeModule } from "../../../../home/components/gio-quick-time-range/gio-quick-time-range.module";

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;
  responseStatusOvertime?: ApiAnalyticsResponseStatusOvertime;
  responseTimeOverTime?: ApiAnalyticsResponseTimeOverTimeComponentInput;
};

type LoadingData = [
  Partial<AnalyticsRequestsCount> & { isLoading: boolean },
  Partial<AnalyticsAverageConnectionDuration> & { isLoading: boolean },
  Partial<AnalyticsResponseStatusRanges> & { isLoading: boolean },
  Partial<ApiAnalyticsResponseStatusOvertime>,
  ApiAnalyticsResponseTimeOverTimeComponentInput
];

@Component({
  selector: 'api-analytics-proxy',
  standalone: true,
  imports: [
    CommonModule,
    MatButton,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsRequestStatsComponent,
    ApiAnalyticsFiltersBarComponent,
    ApiAnalyticsResponseStatusRangesComponent,
    ApiAnalyticsResponseStatusOvertimeComponent,
    ApiAnalyticsResponseTimeOverTimeComponent,
    GioQuickTimeRangeModule
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private static readonly MS_IN_DAY = 24 * 3600 * 1000;
  private readonly apiService = inject(ApiV2Service);
  private readonly apiAnalyticsService = inject(ApiAnalyticsV2Service);
  private readonly activatedRoute = inject(ActivatedRoute);
  timeRangeParams: TimeRangeParams = {
    id: '1d',
    from: new Date().getTime() - ApiAnalyticsProxyComponent.MS_IN_DAY,
    to: new Date().getTime(),
    interval: 0,
  };

  private getRequestsCount: (string, TimeRangeParams) => Observable<Partial<AnalyticsRequestsCount> & { isLoading: boolean }> = (apiId, _timeRangeParams) => this.apiAnalyticsService
    .getRequestsCount(apiId)
    .pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getAverageConnectionDuration: (string, TimeRangeParams) => Observable<Partial<AnalyticsAverageConnectionDuration> & { isLoading: boolean }> = (apiId, _timeRangeParams) =>
    this.apiAnalyticsService.getAverageConnectionDuration(apiId).pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getResponseStatusRanges: (string, TimeRangeParams) => Observable<Partial<AnalyticsResponseStatusRanges> & { isLoading: boolean }> = (apiId, _timeRangeParams) => this.apiAnalyticsService
    .getResponseStatusRanges(apiId)
    .pipe(
      map((responseStatusRanges) => ({ isLoading: false, ...responseStatusRanges })),
      startWith({ isLoading: true }),
    );

  private getResponseStatusOvertime: (string, TimeRangeParams) => Observable<Partial<ApiAnalyticsResponseStatusOvertime> & { isLoading: boolean }> = (apiId, timeRangeParams) =>
    this.apiAnalyticsService.getResponseStatusOvertime(apiId, new Date(timeRangeParams.from), new Date(timeRangeParams.to)).pipe(
      map((responseStatusOvertime) => ({ isLoading: false, ...responseStatusOvertime })),
      startWith({ isLoading: true }),
    );

  private getResponseTimeOverTime: (string, TimeRangeParams) => Observable<ApiAnalyticsResponseTimeOverTimeComponentInput> = (apiId, timeRangeParams) =>
    this.apiAnalyticsService.getResponseTimeOverTime(apiId, new Date(timeRangeParams.from), new Date(timeRangeParams.to)).pipe(
      map((responseTimeOverTime) => ({ isLoading: false, ...responseTimeOverTime })),
      startWith(ResponseTimeIsLoading),
    );

  private analyticsData: (string, TimeRangeParams) => Observable<Omit<ApiAnalyticsVM, 'isLoading' | 'isAnalyticsEnabled'>> = (apiId, timeRangeParams) => combineLatest([
    this.getRequestsCount(apiId, timeRangeParams).pipe(catchError(() => of({ isLoading: false, total: undefined }))),
    this.getAverageConnectionDuration(apiId, timeRangeParams).pipe(catchError(() => of({ isLoading: false, average: undefined }))),
    this.getResponseStatusRanges(apiId, timeRangeParams).pipe(catchError(() => of({ isLoading: false, ranges: undefined }))),
    this.getResponseStatusOvertime(apiId, timeRangeParams).pipe(catchError(() => of({ isLoading: false, timeRange: undefined, data: undefined }))),
    this.getResponseTimeOverTime(apiId, timeRangeParams).pipe(catchError(() => of(ResponseTimeIsLoading))),
  ]).pipe(
    map(([requestsCount, averageConnectionDuration, responseStatuesRanges, responseStatusOvertime, responseTimeOverTime]: LoadingData) => ({
      requestStats: [
        {
          label: 'Total Requests',
          value: requestsCount.total,
          isLoading: requestsCount.isLoading,
        },
        {
          label: 'Average Connection Duration',
          unitLabel: 'ms',
          value: averageConnectionDuration.average,
          isLoading: averageConnectionDuration.isLoading,
        },
      ],
      responseStatusRanges: {
        isLoading: responseStatuesRanges.isLoading,
        data: Object.entries(responseStatuesRanges.ranges ?? {}).map(([label, value]) => ({ label, value: toNumber(value) })),
      },
      responseStatusOvertime: {
        isLoading: responseStatusOvertime.isLoading,
        timeRange: responseStatusOvertime.timeRange,
        data: responseStatusOvertime.data,
      },
      responseTimeOverTime
    })),
  );

  filters$ = new BehaviorSubject<TimeRangeParams>(this.timeRangeParams);

  apiAnalyticsVM$: Observable<ApiAnalyticsVM> = combineLatest([
    this.apiService.getLastApiFetch(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV4Filter()),
    this.filters$,
  ]).pipe(
    map(([api, timeRange]) => [api.analytics.enabled, timeRange]),
    switchMap(([isAnalyticsEnabled, range]) => {
      if (isAnalyticsEnabled) {
        return this.analyticsData(this.activatedRoute.snapshot.params.apiId, range).pipe(map((analyticsData) => ({ isAnalyticsEnabled: true, ...analyticsData })));
      }
      return of({ isAnalyticsEnabled: false });
    }),
    map((analyticsData) => ({ isLoading: false, ...analyticsData })),
    startWith({ isLoading: true }),
  );
}
