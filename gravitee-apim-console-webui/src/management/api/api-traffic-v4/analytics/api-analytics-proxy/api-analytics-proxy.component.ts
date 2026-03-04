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
import { catchError, delay, distinctUntilChanged, map, startWith } from 'rxjs/operators';
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

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;
  error?: boolean;
  hasNoData?: boolean;
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
  private readonly apiId = this.activatedRoute.snapshot.params.apiId;

  private v4AnalyticsData$: Observable<Omit<ApiAnalyticsVM, 'isLoading' | 'isAnalyticsEnabled'>> = this.apiAnalyticsV2Service
    .timeRangeFilter()
    .pipe(
      distinctUntilChanged((a, b) => a?.from === b?.from && a?.to === b?.to),
      switchMap((timeRange) => {
        if (!timeRange?.from || !timeRange?.to) {
          return of({
            requestStats: [
              { label: 'Total Requests', value: undefined, isLoading: false },
              { label: 'Avg Gateway Response Time', unitLabel: 'ms', value: undefined, isLoading: false },
              { label: 'Avg Upstream Response Time', unitLabel: 'ms', value: undefined, isLoading: false },
              { label: 'Avg Content Length', unitLabel: 'bytes', value: undefined, isLoading: false },
            ],
            responseStatusRanges: { isLoading: false, data: [] },
            error: false,
          });
        }
        const { from, to } = timeRange;
        return forkJoin({
          count: this.apiAnalyticsV2Service
            .getV4Analytics(this.apiId, { type: 'COUNT', from, to })
            .pipe(catchError(() => of(null))),
          statsGateway: this.apiAnalyticsV2Service
            .getV4Analytics(this.apiId, { type: 'STATS', from, to, field: 'gateway-response-time-ms' })
            .pipe(catchError(() => of(null))),
          statsEndpoint: this.apiAnalyticsV2Service
            .getV4Analytics(this.apiId, { type: 'STATS', from, to, field: 'endpoint-response-time-ms' })
            .pipe(catchError(() => of(null))),
          statsContentLength: this.apiAnalyticsV2Service
            .getV4Analytics(this.apiId, { type: 'STATS', from, to, field: 'request-content-length' })
            .pipe(catchError(() => of(null))),
          groupByStatus: this.apiAnalyticsV2Service
            .getV4Analytics(this.apiId, { type: 'GROUP_BY', from, to, field: 'status', size: 10 })
            .pipe(catchError(() => of(null))),
        }).pipe(
          map((res) => {
            const count = res.count && res.count.type === 'COUNT' ? res.count.count : 0;
            const statsGateway = res.statsGateway && res.statsGateway.type === 'STATS' ? res.statsGateway : null;
            const statsEndpoint = res.statsEndpoint && res.statsEndpoint.type === 'STATS' ? res.statsEndpoint : null;
            const statsContentLength = res.statsContentLength && res.statsContentLength.type === 'STATS' ? res.statsContentLength : null;
            const groupByStatus = res.groupByStatus && res.groupByStatus.type === 'GROUP_BY' ? res.groupByStatus : null;
            const hasError =
              res.count === null ||
              res.statsGateway === null ||
              res.statsEndpoint === null ||
              res.statsContentLength === null ||
              res.groupByStatus === null;

            const requestStats: AnalyticsRequestStats = [
              { label: 'Total Requests', value: count, isLoading: false },
              {
                label: 'Avg Gateway Response Time',
                unitLabel: 'ms',
                value: statsGateway ? Math.round(statsGateway.avg * 100) / 100 : undefined,
                isLoading: false,
              },
              {
                label: 'Avg Upstream Response Time',
                unitLabel: 'ms',
                value: statsEndpoint ? Math.round(statsEndpoint.avg * 100) / 100 : undefined,
                isLoading: false,
              },
              {
                label: 'Avg Content Length',
                unitLabel: 'bytes',
                value: statsContentLength ? Math.round(statsContentLength.avg) : undefined,
                isLoading: false,
              },
            ];

            const responseStatusRanges: ApiAnalyticsResponseStatusRanges = {
              isLoading: false,
              data: groupByStatus
                ? Object.entries(groupByStatus.values).map(([label, value]) => ({ label, value: toNumber(value) }))
                : [],
            };

            const hasNoData = !hasError && count === 0;
            return { requestStats, responseStatusRanges, error: hasError, hasNoData };
          }),
          delay(0), // Defer to next tick to avoid ExpressionChangedAfterItHasBeenCheckedError when forkJoin completes
        );
      }),
    );

  public apiAnalyticsVM$: Observable<ApiAnalyticsVM> = combineLatest([
    this.apiService.getLastApiFetch(this.apiId).pipe(onlyApiV4Filter()),
    this.apiAnalyticsV2Service.timeRangeFilter(),
  ]).pipe(
    map(([api, timeRange]) => ({ api, timeRange })),
    switchMap(({ api, timeRange }) => {
      if (!api?.analytics?.enabled) {
        return of({ isAnalyticsEnabled: false });
      }
      // No time range yet (filters bar not mounted or not set): show loader so filters bar is visible and can set range
      if (!timeRange?.from || !timeRange?.to) {
        return of({ isAnalyticsEnabled: true, isLoading: true });
      }
      return this.v4AnalyticsData$.pipe(
        map((data) => ({ isAnalyticsEnabled: true, ...data })),
        startWith({ isAnalyticsEnabled: true, isLoading: true }),
      );
    }),
    map((vm) => ({ ...vm, isLoading: (vm as ApiAnalyticsVM).isLoading ?? false })),
    startWith({ isLoading: true }),
  );

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}
}
