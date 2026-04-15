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
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, filter, map, startWith, switchMap } from 'rxjs/operators';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { CountResponse, StatsResponse } from '../../../../../../entities/management-api-v2/analytics/analyticsResponse';

type CardStatus = 'loading' | 'loaded' | 'empty' | 'error';

export type StatCardDisplay = {
  label: string;
  status: CardStatus;
  displayValue?: string;
};

const LOADING_CARDS: StatCardDisplay[] = [
  { label: 'Total Requests', status: 'loading' },
  { label: 'Avg Gateway Response Time', status: 'loading' },
  { label: 'Avg Upstream Response Time', status: 'loading' },
  { label: 'Avg Content Length', status: 'loading' },
];

function humanizeBytes(bytes: number, precision = 1): string {
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  if (bytes === 0) {
    return '0 B';
  }
  const i = Math.min(Math.floor(Math.log(Math.abs(bytes)) / Math.log(1024)), units.length - 1);
  const value = bytes / Math.pow(1024, i);
  return `${value.toFixed(precision)} ${units[i]}`;
}

@Component({
  selector: 'api-analytics-stats-cards',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, GioLoaderModule, MatCardModule, MatIcon, MatTooltip],
  templateUrl: './api-analytics-stats-cards.component.html',
  styleUrl: './api-analytics-stats-cards.component.scss',
})
export class ApiAnalyticsStatsCardsComponent {
  private readonly analyticsService = inject(ApiAnalyticsV2Service);
  private readonly route = inject(ActivatedRoute);
  private readonly apiId: string = this.route.snapshot.params.apiId;

  cards$: Observable<StatCardDisplay[]> = this.analyticsService.timeRangeFilter().pipe(
    filter(Boolean),
    switchMap(({ from, to }) =>
      forkJoin([
        this.fetchCount(from, to),
        this.fetchGatewayResponseTime(from, to),
        this.fetchUpstreamResponseTime(from, to),
        this.fetchContentLength(from, to),
      ]).pipe(startWith(LOADING_CARDS)),
    ),
    startWith(LOADING_CARDS),
  );

  private fetchCount(from: number, to: number): Observable<StatCardDisplay> {
    return this.analyticsService.getAnalytics(this.apiId, { type: 'COUNT', from, to }).pipe(
      map((r) => {
        const count = (r as CountResponse).count;
        return count === 0
          ? { label: 'Total Requests', status: 'empty' as const }
          : { label: 'Total Requests', status: 'loaded' as const, displayValue: count.toLocaleString() };
      }),
      catchError(() => of({ label: 'Total Requests', status: 'error' as const })),
    );
  }

  private fetchGatewayResponseTime(from: number, to: number): Observable<StatCardDisplay> {
    return this.analyticsService.getAnalytics(this.apiId, { type: 'STATS', from, to, field: 'gateway-response-time-ms' }).pipe(
      map((r) => {
        const stats = r as StatsResponse;
        return stats.count === 0
          ? { label: 'Avg Gateway Response Time', status: 'empty' as const }
          : { label: 'Avg Gateway Response Time', status: 'loaded' as const, displayValue: `${Math.round(stats.avg)} ms` };
      }),
      catchError(() => of({ label: 'Avg Gateway Response Time', status: 'error' as const })),
    );
  }

  private fetchUpstreamResponseTime(from: number, to: number): Observable<StatCardDisplay> {
    return this.analyticsService.getAnalytics(this.apiId, { type: 'STATS', from, to, field: 'endpoint-response-time-ms' }).pipe(
      map((r) => {
        const stats = r as StatsResponse;
        return stats.count === 0
          ? { label: 'Avg Upstream Response Time', status: 'empty' as const }
          : { label: 'Avg Upstream Response Time', status: 'loaded' as const, displayValue: `${Math.round(stats.avg)} ms` };
      }),
      catchError(() => of({ label: 'Avg Upstream Response Time', status: 'error' as const })),
    );
  }

  private fetchContentLength(from: number, to: number): Observable<StatCardDisplay> {
    return this.analyticsService.getAnalytics(this.apiId, { type: 'STATS', from, to, field: 'request-content-length' }).pipe(
      map((r) => {
        const stats = r as StatsResponse;
        return stats.count === 0
          ? { label: 'Avg Content Length', status: 'empty' as const }
          : { label: 'Avg Content Length', status: 'loaded' as const, displayValue: humanizeBytes(stats.avg) };
      }),
      catchError(() => of({ label: 'Avg Content Length', status: 'error' as const })),
    );
  }
}
