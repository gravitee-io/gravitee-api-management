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
import { combineLatest, Observable, of, switchMap } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { map, startWith } from 'rxjs/operators';
import { CommonModule } from '@angular/common';

import {
  AnalyticsRequestStats,
  ApiAnalyticsRequestStatsComponent,
} from './components/api-analytics-requests-stats/api-analytics-request-stats.component';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { onlyApiV4Filter } from '../../../../util/apiFilter.operator';
import { AnalyticsRequestsCount } from '../../../../entities/management-api-v2/analytics/analyticsRequestsCount';
import { ApiAnalyticsV2Service } from '../../../../services-ngx/api-analytics-v2.service';
import { AnalyticsAverageConnectionDuration } from '../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;
};

@Component({
  selector: 'api-analytics',
  standalone: true,
  imports: [CommonModule, MatButton, MatCardModule, GioLoaderModule, GioCardEmptyStateModule, ApiAnalyticsRequestStatsComponent],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private readonly apiService = inject(ApiV2Service);
  private readonly apiAnalyticsService = inject(ApiAnalyticsV2Service);
  private readonly activatedRoute = inject(ActivatedRoute);

  private isAnalyticsEnabled$ = this.apiService.getLastApiFetch(this.activatedRoute.snapshot.params.apiId).pipe(
    onlyApiV4Filter(),
    map((api) => api.analytics.enabled),
  );

  private getRequestsCount$: Observable<Partial<AnalyticsRequestsCount> & { isLoading: boolean }> = this.apiAnalyticsService
    .getRequestsCount(this.activatedRoute.snapshot.params.apiId)
    .pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getAverageConnectionDuration$: Observable<Partial<AnalyticsAverageConnectionDuration> & { isLoading: boolean }> =
    this.apiAnalyticsService.getAverageConnectionDuration(this.activatedRoute.snapshot.params.apiId).pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private analyticsData$: Observable<{ requestStats: AnalyticsRequestStats }> = combineLatest([
    this.getRequestsCount$,
    this.getAverageConnectionDuration$,
  ]).pipe(
    map(([requestsCount, averageConnectionDuration]) => ({
      requestStats: [
        {
          label: 'Total requests',
          value: requestsCount.total,
          isLoading: requestsCount.isLoading,
        },
        {
          label: 'Average Connection Duration',
          value: averageConnectionDuration.average,
          isLoading: averageConnectionDuration.isLoading,
        },
      ],
    })),
  );

  apiAnalyticsVM$: Observable<ApiAnalyticsVM> = this.isAnalyticsEnabled$.pipe(
    switchMap((isAnalyticsEnabled) => {
      if (isAnalyticsEnabled) {
        return this.analyticsData$.pipe(map((analyticsData) => ({ isAnalyticsEnabled: true, ...analyticsData })));
      }
      return of({ isAnalyticsEnabled: false });
    }),
    map((analyticsData) => ({ isLoading: false, ...analyticsData })),
    startWith({ isLoading: true }),
  );
}
