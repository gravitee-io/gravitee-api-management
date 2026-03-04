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
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';

import { GioChartPieModule } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { BehaviorSubject, of } from 'rxjs';
import { catchError, filter, map, startWith, switchMap } from 'rxjs/operators';

import { GioChartPieInput } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { AnalyticsGroupByResponse } from '../../../../../../entities/management-api-v2/analytics/analyticsGroupBy';

function getColor(status: string): string {
  if (status.startsWith('2')) return '#30ab61';
  if (status.startsWith('3')) return '#365bd3';
  if (status.startsWith('4')) return '#ff9f40';
  if (status.startsWith('5')) return '#cf3942';
  return '#bbb';
}

@Component({
  selector: 'api-analytics-http-status-pie-chart',
  standalone: true,
  imports: [CommonModule, MatCardModule, GioChartPieModule, GioLoaderModule, GioCardEmptyStateModule],
  templateUrl: './api-analytics-http-status-pie-chart.component.html',
  styleUrl: './api-analytics-http-status-pie-chart.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiAnalyticsHttpStatusPieChartComponent {
  private apiId$ = new BehaviorSubject<string | null>(null);

  @Input() set apiId(value: string) {
    this.apiId$.next(value);
  }

  chartInput$ = this.apiId$.pipe(
    filter((id): id is string => !!id),
    switchMap((apiId) =>
      this.apiAnalyticsV2Service.getAnalytics<AnalyticsGroupByResponse>(apiId, { type: 'GROUP_BY', field: 'status', size: 20 }).pipe(
        map((r) => ({ isLoading: false, input: this.mapToChartInput(r) })),
        startWith({ isLoading: true, input: [] as GioChartPieInput[] }),
        catchError(() => of({ isLoading: false, input: [] as GioChartPieInput[] })),
      ),
    ),
  );

  constructor(private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service) {}

  private mapToChartInput(response: AnalyticsGroupByResponse): GioChartPieInput[] {
    return Object.entries(response.values)
      .filter(([, value]) => (value as number) > 0)
      .map(([status, value]) => ({
        label: status,
        value,
        color: getColor(status),
      }));
  }
}
