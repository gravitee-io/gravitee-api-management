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
import { AnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { ApiAnalyticsResponseStatusOvertimeComponent } from '../components/api-analytics-response-status-overtime/api-analytics-response-status-overtime.component';
import { ApiAnalyticsResponseTimeOverTimeComponent } from '../components/api-analytics-response-time-over-time/api-analytics-response-time-over-time.component';
import { AnalyticsCount } from '../../../../../entities/management-api-v2/analytics/analyticsCount';
import { AnalyticsStats } from '../../../../../entities/management-api-v2/analytics/analyticsStats';
import { AnalyticsGroupBy } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy';
import { GioChartPieInput } from '../../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartPieModule } from '../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;
  statusPieChart?: { isLoading: boolean; data?: GioChartPieInput[] };
};

@Component({
  selector: 'api-analytics-proxy',
  imports: [
    CommonModule,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    GioChartPieModule,
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
  private getCount$: Observable<Partial<AnalyticsCount> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getCount(this.activatedRoute.snapshot.params.apiId)
    .pipe(
      map((countResult) => ({ isLoading: false, ...countResult })),
      startWith({ isLoading: true }),
    );

  private getGatewayResponseTimeStats$: Observable<Partial<AnalyticsStats> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getStats(this.activatedRoute.snapshot.params.apiId, 'gateway-response-time-ms')
    .pipe(
      map((statsResult) => ({ isLoading: false, ...statsResult })),
      startWith({ isLoading: true }),
    );

  private getUpstreamResponseTimeStats$: Observable<Partial<AnalyticsStats> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getStats(this.activatedRoute.snapshot.params.apiId, 'endpoint-response-time-ms')
    .pipe(
      map((statsResult) => ({ isLoading: false, ...statsResult })),
      startWith({ isLoading: true }),
    );

  private getContentLengthStats$: Observable<Partial<AnalyticsStats> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getStats(this.activatedRoute.snapshot.params.apiId, 'request-content-length')
    .pipe(
      map((statsResult) => ({ isLoading: false, ...statsResult })),
      startWith({ isLoading: true }),
    );

  private getResponseStatusRanges$: Observable<Partial<AnalyticsResponseStatusRanges> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getResponseStatusRanges(this.activatedRoute.snapshot.params.apiId)
    .pipe(
      map((responseStatusRanges) => ({ isLoading: false, ...responseStatusRanges })),
      startWith({ isLoading: true }),
    );

  private getStatusGroupBy$: Observable<Partial<AnalyticsGroupBy> & { isLoading: boolean }> = this.apiAnalyticsV2Service
    .getGroupBy(this.activatedRoute.snapshot.params.apiId, 'status')
    .pipe(
      map((groupByResult) => ({ isLoading: false, ...groupByResult })),
      startWith({ isLoading: true }),
    );

  public apiAnalyticsVM$: Observable<ApiAnalyticsVM> = combineLatest([
    this.apiService.getLastApiFetch(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV4Filter()),
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

  private analyticsData$: Observable<Omit<ApiAnalyticsVM, 'isLoading' | 'isAnalyticsEnabled'>> = combineLatest([
    this.getCount$.pipe(catchError(() => of({ isLoading: false, count: undefined }))),
    this.getGatewayResponseTimeStats$.pipe(catchError(() => of({ isLoading: false, avg: undefined }))),
    this.getUpstreamResponseTimeStats$.pipe(catchError(() => of({ isLoading: false, avg: undefined }))),
    this.getContentLengthStats$.pipe(catchError(() => of({ isLoading: false, avg: undefined }))),
    this.getResponseStatusRanges$.pipe(catchError(() => of({ isLoading: false, ranges: undefined }))),
    this.getStatusGroupBy$.pipe(catchError(() => of({ isLoading: false, values: undefined }))),
  ]).pipe(
    map(([countResult, gatewayStats, upstreamStats, contentLengthStats, responseStatuesRanges, statusGroupBy]) => ({
      requestStats: [
        {
          label: 'Total Requests',
          value: countResult.count,
          isLoading: countResult.isLoading,
        },
        {
          label: 'Avg Gateway Response Time',
          unitLabel: 'ms',
          value: gatewayStats.avg,
          isLoading: gatewayStats.isLoading,
        },
        {
          label: 'Avg Upstream Response Time',
          unitLabel: 'ms',
          value: upstreamStats.avg,
          isLoading: upstreamStats.isLoading,
        },
        {
          label: 'Avg Content Length',
          unitLabel: 'B',
          value: contentLengthStats.avg,
          isLoading: contentLengthStats.isLoading,
        },
      ],
      responseStatusRanges: {
        isLoading: responseStatuesRanges.isLoading,
        data: Object.entries(responseStatuesRanges.ranges ?? {}).map(([label, value]) => ({ label, value: toNumber(value) })),
      },
      statusPieChart: {
        isLoading: statusGroupBy.isLoading,
        data: statusGroupBy.values
          ? Object.entries(statusGroupBy.values)
              .filter(([, value]) => (value as number) > 0)
              .map(([label, value]) => ({
                label: getStatusLabel(label),
                value: value as number,
                color: getStatusColor(label),
              }))
          : undefined,
      },
    })),
  );

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}
}

const getStatusColor = (statusCode: string): string => {
  if (statusCode.startsWith('2')) {
    return '#30ab61';
  } else if (statusCode.startsWith('3')) {
    return '#365bd3';
  } else if (statusCode.startsWith('4')) {
    return '#ff9f40';
  } else if (statusCode.startsWith('5')) {
    return '#cf3942';
  } else {
    return '#bbb';
  }
};

const getStatusLabel = (statusCode: string): string => {
  return statusCode;
};
