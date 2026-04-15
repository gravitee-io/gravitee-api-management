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
import { combineLatest, forkJoin, Observable, of, switchMap } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { catchError, filter, map, startWith } from 'rxjs/operators';
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
import { ApiAnalyticsResponseStatusOvertimeComponent } from '../components/api-analytics-response-status-overtime/api-analytics-response-status-overtime.component';
import { ApiAnalyticsResponseTimeOverTimeComponent } from '../components/api-analytics-response-time-over-time/api-analytics-response-time-over-time.component';
import { ApiAnalyticsStatsCardsComponent } from '../components/api-analytics-stats-cards/api-analytics-stats-cards.component';
import { ApiAnalyticsStatusPieComponent } from '../components/api-analytics-status-pie/api-analytics-status-pie.component';
import { CountResponse, GroupByResponse, StatsResponse } from '../../../../../entities/management-api-v2/analytics/analyticsResponse';
import { TimeRangeParams } from '../../../../../shared/utils/timeFrameRanges';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  isAllNewWidgetsEmpty?: boolean;
  requestStats?: AnalyticsRequestStats;
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;
};

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
    ApiAnalyticsStatsCardsComponent,
    ApiAnalyticsStatusPieComponent,
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private readonly apiId = this.activatedRoute.snapshot.params.apiId;

  private getRequestsCount$: Observable<Partial<AnalyticsRequestsCount> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getRequestsCount(this.apiId)
    .pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getAverageConnectionDuration$: Observable<Partial<AnalyticsAverageConnectionDuration> & { isLoading: boolean }> =
    this.apiAnalyticsV2Service.getAverageConnectionDuration(this.apiId).pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getResponseStatusRanges$: Observable<Partial<AnalyticsResponseStatusRanges> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getResponseStatusRanges(this.apiId)
    .pipe(
      map((responseStatusRanges) => ({ isLoading: false, ...responseStatusRanges })),
      startWith({ isLoading: true }),
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
        return combineLatest([this.analyticsData$, this.newWidgetsEmptyState$]).pipe(
          map(([analyticsData, newWidgetsEmptyState]) => ({
            isAnalyticsEnabled: true,
            isAllNewWidgetsEmpty: newWidgetsEmptyState.isAllEmpty,
            ...analyticsData,
          })),
        );
      }
      return of({ isAnalyticsEnabled: false });
    }),
    map((analyticsData) => ({ isLoading: false, ...analyticsData })),
    startWith({ isLoading: true }),
  );

  private analyticsData$: Observable<Omit<ApiAnalyticsVM, 'isLoading' | 'isAnalyticsEnabled'>> = combineLatest([
    this.getRequestsCount$.pipe(catchError(() => of({ isLoading: false, total: undefined }))),
    this.getAverageConnectionDuration$.pipe(catchError(() => of({ isLoading: false, average: undefined }))),
    this.getResponseStatusRanges$.pipe(catchError(() => of({ isLoading: false, ranges: undefined }))),
  ]).pipe(
    map(([requestsCount, averageConnectionDuration, responseStatuesRanges]) => ({
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
    })),
  );

  private newWidgetsEmptyState$: Observable<{ isLoading: boolean; isAllEmpty: boolean }> = this.apiAnalyticsV2Service
    .timeRangeFilter()
    .pipe(
      filter((timeRange): timeRange is TimeRangeParams => !!timeRange),
      switchMap(({ from, to }) =>
        forkJoin({
          count: this.apiAnalyticsV2Service
            .getAnalytics(this.apiId, { type: 'COUNT', from, to })
            .pipe(catchError(() => of<CountResponse | null>(null))),
          gatewayResponseTime: this.apiAnalyticsV2Service
            .getAnalytics(this.apiId, { type: 'STATS', from, to, field: 'gateway-response-time-ms' })
            .pipe(catchError(() => of<StatsResponse | null>(null))),
          upstreamResponseTime: this.apiAnalyticsV2Service
            .getAnalytics(this.apiId, { type: 'STATS', from, to, field: 'endpoint-response-time-ms' })
            .pipe(catchError(() => of<StatsResponse | null>(null))),
          contentLength: this.apiAnalyticsV2Service
            .getAnalytics(this.apiId, { type: 'STATS', from, to, field: 'request-content-length' })
            .pipe(catchError(() => of<StatsResponse | null>(null))),
          statusPie: this.apiAnalyticsV2Service
            .getAnalytics(this.apiId, { type: 'GROUP_BY', field: 'status', size: 10, from, to })
            .pipe(catchError(() => of<GroupByResponse | null>(null))),
        }).pipe(
          map((responses) => {
            const hasError = Object.values(responses).some((response) => response === null);
            if (hasError) {
              return { isLoading: false, isAllEmpty: false };
            }

            const countResponse = responses.count as CountResponse;
            const gatewayResponseTime = responses.gatewayResponseTime as StatsResponse;
            const upstreamResponseTime = responses.upstreamResponseTime as StatsResponse;
            const contentLength = responses.contentLength as StatsResponse;
            const statusPie = responses.statusPie as GroupByResponse;

            const isAllEmpty =
              countResponse.count === 0 &&
              gatewayResponseTime.count === 0 &&
              upstreamResponseTime.count === 0 &&
              contentLength.count === 0 &&
              Object.keys(statusPie.values ?? {}).length === 0;

            return { isLoading: false, isAllEmpty };
          }),
          startWith({ isLoading: true, isAllEmpty: false }),
        ),
      ),
      startWith({ isLoading: true, isAllEmpty: false }),
    );

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}
}
