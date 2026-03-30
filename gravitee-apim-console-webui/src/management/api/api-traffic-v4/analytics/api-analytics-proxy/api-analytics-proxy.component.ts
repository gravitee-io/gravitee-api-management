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
import { MatCardModule } from '@angular/material/card';
import { combineLatest, forkJoin, Observable, of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { catchError, filter, map, startWith, switchMap } from 'rxjs/operators';
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
import { AnalyticsUnifiedQueryType } from '../../../../../entities/management-api-v2/analytics/analyticsUnifiedQuery';
import { AnalyticsUnifiedResponse } from '../../../../../entities/management-api-v2/analytics/analyticsUnifiedResponse';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { TimeRangeParams } from '../../../../../shared/utils/timeFrameRanges';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  /** True when COUNT is 0: show dashboard empty state and hide widgets. */
  noAnalyticsData?: boolean;
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
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private readonly apiService = inject(ApiV2Service);
  private readonly apiAnalyticsV2Service = inject(ApiAnalyticsV2Service);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly snackBarService = inject(SnackBarService);

  private readonly apiId = this.activatedRoute.snapshot.params.apiId;

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

  private analyticsData$: Observable<Omit<ApiAnalyticsVM, 'isLoading' | 'isAnalyticsEnabled'>> = this.apiAnalyticsV2Service
    .timeRangeFilter()
    .pipe(
      filter((t): t is TimeRangeParams => !!t),
      switchMap(() =>
        forkJoin({
          count: this.safeUnified({ type: AnalyticsUnifiedQueryType.COUNT }),
          gatewayMs: this.safeUnified({
            type: AnalyticsUnifiedQueryType.STATS,
            field: 'gateway-response-time-ms',
          }),
          endpointMs: this.safeUnified({
            type: AnalyticsUnifiedQueryType.STATS,
            field: 'endpoint-response-time-ms',
          }),
          contentLen: this.safeUnified({
            type: AnalyticsUnifiedQueryType.STATS,
            field: 'request-content-length',
          }),
        }).pipe(
          switchMap((unified) => {
            if (this.isUnifiedFailed(unified)) {
              this.snackBarService.error('Failed to load analytics');
              return of({
                noAnalyticsData: false,
                requestStats: this.buildFailedStatsRow(),
                responseStatusRanges: { isLoading: false, data: [] },
              });
            }

            const total = unified.count.total ?? 0;
            if (total === 0) {
              return of({
                noAnalyticsData: true,
                requestStats: undefined,
                responseStatusRanges: undefined,
              });
            }

            return this.apiAnalyticsV2Service.getResponseStatusRanges(this.apiId).pipe(
              map((ranges: AnalyticsResponseStatusRanges) => ({
                noAnalyticsData: false,
                requestStats: this.buildRequestStatsFromUnified(unified),
                responseStatusRanges: {
                  isLoading: false,
                  data: Object.entries(ranges.ranges ?? {}).map(([label, value]) => ({ label, value: toNumber(value) })),
                },
              })),
              startWith({
                noAnalyticsData: false,
                requestStats: this.buildRequestStatsFromUnified(unified),
                responseStatusRanges: { isLoading: true, data: [] },
              }),
              catchError(() =>
                of({
                  noAnalyticsData: false,
                  requestStats: this.buildRequestStatsFromUnified(unified),
                  responseStatusRanges: { isLoading: false, data: [] },
                }),
              ),
            );
          }),
          startWith({
            noAnalyticsData: false,
            requestStats: this.buildLoadingStats(),
            responseStatusRanges: { isLoading: true, data: [] },
          }),
        ),
      ),
    );

  private safeUnified(params: Parameters<ApiAnalyticsV2Service['getUnifiedAnalytics']>[1]): Observable<AnalyticsUnifiedResponse | null> {
    return this.apiAnalyticsV2Service.getUnifiedAnalytics(this.apiId, params).pipe(catchError(() => of(null)));
  }

  private isUnifiedFailed(unified: {
    count: AnalyticsUnifiedResponse | null;
    gatewayMs: AnalyticsUnifiedResponse | null;
    endpointMs: AnalyticsUnifiedResponse | null;
    contentLen: AnalyticsUnifiedResponse | null;
  }): boolean {
    return unified.count === null || unified.gatewayMs === null || unified.endpointMs === null || unified.contentLen === null;
  }

  private buildLoadingStats(): AnalyticsRequestStats {
    return [
      { label: 'Total Requests', isLoading: true },
      { label: 'Avg Gateway Response Time', unitLabel: ' ms', isLoading: true },
      { label: 'Avg Upstream Response Time', unitLabel: ' ms', isLoading: true },
      { label: 'Avg Request Content Length', unitLabel: ' B', isLoading: true },
    ];
  }

  private buildFailedStatsRow(): AnalyticsRequestStats {
    return [
      { label: 'Total Requests', isLoading: false },
      { label: 'Avg Gateway Response Time', unitLabel: ' ms', isLoading: false },
      { label: 'Avg Upstream Response Time', unitLabel: ' ms', isLoading: false },
      { label: 'Avg Request Content Length', unitLabel: ' B', isLoading: false },
    ];
  }

  private buildRequestStatsFromUnified(unified: {
    count: AnalyticsUnifiedResponse;
    gatewayMs: AnalyticsUnifiedResponse;
    endpointMs: AnalyticsUnifiedResponse;
    contentLen: AnalyticsUnifiedResponse;
  }): AnalyticsRequestStats {
    const total = unified.count.total ?? 0;
    return [
      { label: 'Total Requests', value: total, isLoading: false },
      {
        label: 'Avg Gateway Response Time',
        unitLabel: ' ms',
        value: unified.gatewayMs.stats?.avg,
        isLoading: false,
      },
      {
        label: 'Avg Upstream Response Time',
        unitLabel: ' ms',
        value: unified.endpointMs.stats?.avg,
        isLoading: false,
      },
      {
        label: 'Avg Request Content Length',
        unitLabel: ' B',
        value: unified.contentLen.stats?.avg,
        isLoading: false,
      },
    ];
  }
}
