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

import {
  AnalyticsRequestStats,
  ApiAnalyticsRequestStatsComponent,
} from '../components/api-analytics-requests-stats/api-analytics-request-stats.component';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { onlyApiV4Filter } from '../../../../../util/apiFilter.operator';
import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
import { ApiAnalyticsResponseStatusOvertimeComponent } from '../components/api-analytics-response-status-overtime/api-analytics-response-status-overtime.component';
import { ApiAnalyticsResponseTimeOverTimeComponent } from '../components/api-analytics-response-time-over-time/api-analytics-response-time-over-time.component';
import { AnalyticsCountResponse } from '../../../../../entities/management-api-v2/analytics/analyticsCount';
import { AnalyticsStatsResponse } from '../../../../../entities/management-api-v2/analytics/analyticsStats';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;
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
    ApiAnalyticsResponseStatusOvertimeComponent,
    ApiAnalyticsResponseTimeOverTimeComponent,
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private readonly apiId = this.activatedRoute.snapshot.params.apiId;

  private getCount$ = this.apiAnalyticsV2Service
    .getAnalytics<AnalyticsCountResponse>(this.apiId, { type: 'COUNT' })
    .pipe(
      map((r) => ({ count: r.count, isLoading: false as const })),
      startWith({ count: undefined as number | undefined, isLoading: true as const }),
      catchError(() => of({ count: undefined, isLoading: false })),
    );

  private getGatewayStats$ = this.apiAnalyticsV2Service
    .getAnalytics<AnalyticsStatsResponse>(this.apiId, { type: 'STATS', field: 'gateway-response-time-ms' })
    .pipe(
      map((r) => ({ avg: r.avg, isLoading: false as const })),
      startWith({ avg: undefined as number | undefined, isLoading: true as const }),
      catchError(() => of({ avg: undefined, isLoading: false })),
    );

  private getEndpointStats$ = this.apiAnalyticsV2Service
    .getAnalytics<AnalyticsStatsResponse>(this.apiId, { type: 'STATS', field: 'endpoint-response-time-ms' })
    .pipe(
      map((r) => ({ avg: r.avg, isLoading: false as const })),
      startWith({ avg: undefined as number | undefined, isLoading: true as const }),
      catchError(() => of({ avg: undefined, isLoading: false })),
    );

  private getContentLengthStats$ = this.apiAnalyticsV2Service
    .getAnalytics<AnalyticsStatsResponse>(this.apiId, { type: 'STATS', field: 'request-content-length' })
    .pipe(
      map((r) => ({ avg: r.avg, isLoading: false as const })),
      startWith({ avg: undefined as number | undefined, isLoading: true as const }),
      catchError(() => of({ avg: undefined, isLoading: false })),
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
    this.getCount$,
    this.getGatewayStats$,
    this.getEndpointStats$,
    this.getContentLengthStats$,
  ]).pipe(
    map(([count, gateway, endpoint, contentLength]) => ({
      requestStats: [
        { label: 'Total Requests', value: count.count, isLoading: count.isLoading },
        { label: 'Avg Gateway Response Time', unitLabel: 'ms', value: gateway.avg, isLoading: gateway.isLoading },
        { label: 'Avg Upstream Response Time', unitLabel: 'ms', value: endpoint.avg, isLoading: endpoint.isLoading },
        { label: 'Avg Content Length', unitLabel: 'B', value: contentLength.avg, isLoading: contentLength.isLoading },
      ],
    })),
  );

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}
}
