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
import { Observable, of } from 'rxjs';
import { catchError, filter, map, startWith, switchMap } from 'rxjs/operators';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';

import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { GroupByResponse } from '../../../../../../entities/management-api-v2/analytics/analyticsResponse';
import { GioChartPieInput } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartPieModule } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';

type StatusPieVM = { status: 'loading' } | { status: 'loaded'; chartData: GioChartPieInput[] } | { status: 'empty' } | { status: 'error' };

const STATUS_COLORS: Record<string, string> = {
  '1': '#bbb',
  '2': '#30ab61',
  '3': '#365bd3',
  '4': '#ff9f40',
  '5': '#cf3942',
};

function getStatusColor(code: string): string {
  return STATUS_COLORS[code.charAt(0)] ?? '#bbb';
}

function mapToVm(response: GroupByResponse): StatusPieVM {
  const entries = Object.entries(response.values);
  if (entries.length === 0) {
    return { status: 'empty' };
  }
  const chartData: GioChartPieInput[] = entries.map(([code, count]) => ({
    label: code,
    value: count,
    color: getStatusColor(code),
  }));
  return { status: 'loaded', chartData };
}

@Component({
  selector: 'api-analytics-status-pie',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCardModule, GioLoaderModule, GioCardEmptyStateModule, GioChartPieModule, MatIcon],
  templateUrl: './api-analytics-status-pie.component.html',
  styleUrl: './api-analytics-status-pie.component.scss',
})
export class ApiAnalyticsStatusPieComponent {
  private readonly analyticsService = inject(ApiAnalyticsV2Service);
  private readonly route = inject(ActivatedRoute);
  private readonly apiId: string = this.route.snapshot.params.apiId;

  vm$: Observable<StatusPieVM> = this.analyticsService.timeRangeFilter().pipe(
    filter(Boolean),
    switchMap(({ from, to }) =>
      this.analyticsService.getAnalytics(this.apiId, { type: 'GROUP_BY', field: 'status', size: 10, from, to }).pipe(
        map((r) => mapToVm(r as GroupByResponse)),
        catchError(() => of({ status: 'error' as const })),
        startWith({ status: 'loading' as const }),
      ),
    ),
    startWith({ status: 'loading' as const }),
  );
}
