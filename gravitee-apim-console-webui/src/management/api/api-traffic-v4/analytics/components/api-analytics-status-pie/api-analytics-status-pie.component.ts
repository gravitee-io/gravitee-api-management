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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, Input, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { AnalyticsGroupBy } from '../../../../../../entities/management-api-v2/analytics/analyticsUnified';
import { GioChartPieInput } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartPieModule } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';

const STATUS_COLORS: Record<string, string> = {
  '1': '#d3d5dc',
  '2': '#02c37f',
  '3': '#6978ff',
  '4': '#bf3f0e',
  '5': '#cb0366',
};

function statusColor(code: string): string {
  return STATUS_COLORS[code.charAt(0)] ?? '#d3d5dc';
}

@Component({
  selector: 'api-analytics-status-pie',
  imports: [MatCardModule, GioLoaderModule, GioCardEmptyStateModule, GioChartPieModule],
  templateUrl: './api-analytics-status-pie.component.html',
  styleUrl: './api-analytics-status-pie.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiAnalyticsStatusPieComponent implements OnInit {
  @Input({ required: true }) apiId: string;

  public isLoading = true;
  public isEmpty = false;
  public hasError = false;
  public pieData: GioChartPieInput[] = [];

  constructor(
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.apiAnalyticsV2Service
      .timeRangeFilter()
      .pipe(
        switchMap(() => {
          this.isLoading = true;
          this.hasError = false;
          this.cdr.markForCheck();
          return this.apiAnalyticsV2Service.getAnalytics<AnalyticsGroupBy>(this.apiId, { type: 'GROUP_BY', field: 'status' });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.pieData = Object.entries(res.values).map(([code, value]) => ({
            label: code,
            value,
            color: statusColor(code),
          }));
          this.isEmpty = isEmpty(this.pieData) || !this.pieData.some((d) => d.value > 0);
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.hasError = true;
          this.isLoading = false;
          this.cdr.markForCheck();
        },
      });
  }
}
