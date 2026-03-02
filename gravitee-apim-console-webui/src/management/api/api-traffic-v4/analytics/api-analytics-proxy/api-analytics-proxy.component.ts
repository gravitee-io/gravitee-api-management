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
import { Component } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { combineLatest, Observable, of, switchMap } from 'rxjs';
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
import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
import {
  ApiAnalyticsResponseStatusRanges,
  ApiAnalyticsResponseStatusRangesComponent,
} from '../../../../../shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component';
import { ApiAnalyticsResponseStatusOvertimeComponent } from '../components/api-analytics-response-status-overtime/api-analytics-response-status-overtime.component';
import { ApiAnalyticsResponseTimeOverTimeComponent } from '../components/api-analytics-response-time-over-time/api-analytics-response-time-over-time.component';
import { AnalyticsCount, AnalyticsGroupBy, AnalyticsStats } from '../../../../../entities/management-api-v2/analytics/analyticsUnified';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;
};

function bucketStatusCodes(values: Record<string, number>): Record<string, number> {
  const ranges: Record<string, number> = {};
  for (const [code, count] of Object.entries(values)) {
    const n = Number(code);
    const key =
      n < 200 ? '100.0-200.0' : n < 300 ? '200.0-300.0' : n < 400 ? '300.0-400.0' : n < 500 ? '400.0-500.0' : '500.0-600.0';
    ranges[key] = (ranges[key] ?? 0) + count;
  }
  return ranges;
}

@Component({
  selector: 'api-analytics-proxy',
  imports: [
    CommonModule,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsRequestStatsComponent,
    ApiAnalyticsFiltersBarComponent,
    ApiAnalyticsResponseStatusRangesComponent,
    ApiAnalyticsResponseStatusOvertimeComponent,
    ApiAnalyticsResponseTimeOverTimeComponent,
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private readonly apiId = this.activatedRoute.snapshot.params.apiId;

  private count$: Observable<Partial<AnalyticsCount> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getAnalyticsCount(this.apiId)
    .pipe(map((data) => ({ isLoading: false, ...data })), startWith({ isLoading: true }));

  private gwStats$: Observable<Partial<AnalyticsStats> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getAnalyticsStats(this.apiId, 'gateway-response-time-ms')
    .pipe(map((data) => ({ isLoading: false, ...data })), startWith({ isLoading: true }));

  private upStats$: Observable<Partial<AnalyticsStats> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getAnalyticsStats(this.apiId, 'endpoint-response-time-ms')
    .pipe(map((data) => ({ isLoading: false, ...data })), startWith({ isLoading: true }));

  private clStats$: Observable<Partial<AnalyticsStats> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getAnalyticsStats(this.apiId, 'request-content-length')
    .pipe(map((data) => ({ isLoading: false, ...data })), startWith({ isLoading: true }));

  private groupByStatus$: Observable<Partial<AnalyticsGroupBy> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getAnalyticsGroupBy(this.apiId, 'status')
    .pipe(map((data) => ({ isLoading: false, ...data })), startWith({ isLoading: true }));

  private analyticsData$: Observable<Omit<ApiAnalyticsVM, 'isLoading' | 'isAnalyticsEnabled'>> = combineLatest([
    this.count$.pipe(catchError(() => of({ isLoading: false, count: undefined }))),
    this.gwStats$.pipe(catchError(() => of({ isLoading: false, avg: undefined }))),
    this.upStats$.pipe(catchError(() => of({ isLoading: false, avg: undefined }))),
    this.clStats$.pipe(catchError(() => of({ isLoading: false, avg: undefined }))),
    this.groupByStatus$.pipe(catchError(() => of({ isLoading: false, values: undefined }))),
  ]).pipe(
    map(([count, gwStats, upStats, clStats, groupByStatus]) => ({
      requestStats: [
        { label: 'Total Requests', value: count.count, isLoading: count.isLoading },
        { label: 'Avg GW Response Time', unitLabel: 'ms', value: gwStats.avg, isLoading: gwStats.isLoading },
        { label: 'Avg Upstream Response Time', unitLabel: 'ms', value: upStats.avg, isLoading: upStats.isLoading },
        { label: 'Avg Content Length', unitLabel: 'B', value: clStats.avg, isLoading: clStats.isLoading },
      ],
      responseStatusRanges: {
        isLoading: groupByStatus.isLoading,
        data: Object.entries(bucketStatusCodes(groupByStatus.values ?? {})).map(([label, value]) => ({ label, value: toNumber(value) })),
      },
    })),
  );

  public apiAnalyticsVM$: Observable<ApiAnalyticsVM> = combineLatest([
    this.apiService.getLastApiFetch(this.apiId).pipe(onlyApiV4Filter()),
    this.apiAnalyticsV2Service.timeRangeFilter(),
  ]).pipe(
    map(([api]) => {
      return { isAnalyticsEnabled: api.analytics.enabled };
    }),
    switchMap(({ isAnalyticsEnabled }) => {
      if (isAnalyticsEnabled) {
        return this.analyticsData$.pipe(map((analyticsData) => ({ isAnalyticsEnabled: true, ...analyticsData })));
      }
      return of({ isAnalyticsEnabled: false });
    }),
    map((analyticsData) => ({ isLoading: false, ...analyticsData })),
    startWith({ isLoading: true }),
  );

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}
}
