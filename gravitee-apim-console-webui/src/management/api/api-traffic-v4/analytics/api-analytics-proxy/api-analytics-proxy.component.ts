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
import { ChangeDetectionStrategy, Component } from '@angular/core';
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
import { AnalyticsCount, AnalyticsStats } from '../../../../../entities/management-api-v2/analytics/analyticsUnified';
import { ApiAnalyticsStatusPieComponent } from '../components/api-analytics-status-pie/api-analytics-status-pie.component';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  apiId?: string;
  requestStats?: AnalyticsRequestStats;
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;
};

@Component({
  selector: 'api-analytics-proxy',
  changeDetection: ChangeDetectionStrategy.OnPush,
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
    ApiAnalyticsStatusPieComponent,
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private readonly apiId = this.activatedRoute.snapshot.params.apiId;

  public apiAnalyticsVM$: Observable<ApiAnalyticsVM> = combineLatest([
    this.apiService.getLastApiFetch(this.apiId).pipe(onlyApiV4Filter()),
    this.apiAnalyticsV2Service.timeRangeFilter(),
  ]).pipe(
    map(([api]) => ({ isAnalyticsEnabled: api.analytics.enabled })),
    switchMap(({ isAnalyticsEnabled }) => {
      if (!isAnalyticsEnabled) {
        return of({ isAnalyticsEnabled: false });
      }
      // All HTTP calls share this switchMap so that a new time-range emission cancels
      // any in-flight requests from the previous range before issuing fresh ones.
      return combineLatest([
        this.apiAnalyticsV2Service.getAnalytics<AnalyticsCount>(this.apiId, { type: 'COUNT' }).pipe(startWith(null), catchError(() => of(null))),
        this.apiAnalyticsV2Service
          .getAnalytics<AnalyticsStats>(this.apiId, { type: 'STATS', field: 'gateway-response-time-ms' })
          .pipe(startWith(null), catchError(() => of(null))),
        this.apiAnalyticsV2Service
          .getAnalytics<AnalyticsStats>(this.apiId, { type: 'STATS', field: 'endpoint-response-time-ms' })
          .pipe(startWith(null), catchError(() => of(null))),
        this.apiAnalyticsV2Service
          .getAnalytics<AnalyticsStats>(this.apiId, { type: 'STATS', field: 'request-content-length' })
          .pipe(startWith(null), catchError(() => of(null))),
        this.apiAnalyticsV2Service
          .getResponseStatusRanges(this.apiId)
          .pipe(
            map((responseStatusRanges) => ({ isLoading: false, ...responseStatusRanges })),
            startWith({ isLoading: true }),
            catchError(() => of({ isLoading: false, ranges: undefined })),
          ),
      ]).pipe(
        map(([count, gatewayRt, upstreamRt, contentLength, responseStatusRanges]) => ({
          isAnalyticsEnabled: true,
          apiId: this.apiId,
          requestStats: [
            { label: 'Total Requests', value: count?.count, isLoading: count === null },
            { label: 'Avg Gateway Response Time', unitLabel: 'ms', value: gatewayRt?.avg, isLoading: gatewayRt === null },
            { label: 'Avg Upstream Response Time', unitLabel: 'ms', value: upstreamRt?.avg, isLoading: upstreamRt === null },
            { label: 'Avg Content Length', unitLabel: 'bytes', value: contentLength?.avg, isLoading: contentLength === null },
          ],
          responseStatusRanges: {
            isLoading: (responseStatusRanges as { isLoading: boolean }).isLoading,
            data: Object.entries((responseStatusRanges as Partial<AnalyticsResponseStatusRanges>).ranges ?? {}).map(([label, value]) => ({
              label,
              value: toNumber(value),
            })),
          },
        })),
      );
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
