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

import { Component, DestroyRef, input, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs/operators';

import { ApiAnalyticsV2Service } from '../../../../../../../../services-ngx/api-analytics-v2.service';
import { SnackBarService } from '../../../../../../../../services-ngx/snack-bar.service';
import { GioChartPieInput } from '../../../../../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartPieModule } from '../../../../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { WidgetConfig } from '../../../../../../../../entities/management-api-v2/analytics/analytics';

export const colors = ['#2B72FB', '#64BDC6', '#EECA34', '#FA4B42', '#FE6A35'];
export const labels = ['100-199', '200-299', '300-399', '400-499', '500-599'];

@Component({
  selector: 'pie-chart-widget',
  standalone: true,
  imports: [MatCardModule, GioLoaderModule, GioChartPieModule],
  templateUrl: './pie-chart-widget.component.html',
  styleUrl: './pie-chart-widget.component.scss',
})
export class PieChartWidgetComponent implements OnInit {
  public config = input<WidgetConfig>();
  public isLoading = true;

  public chartInput: GioChartPieInput[];
  public labelFormatter = function () {
    const value = this.point.y;
    return `<div style="text-align: center;">
            <div style="font-size: 14px; color: #666; font-weight: 700;">${value}</div>
          </div>`;
  };

  constructor(
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.apiAnalyticsV2Service
      .timeRangeFilter()
      .pipe(
        switchMap((timeRangeParams) => {
          this.isLoading = true;
          const ranges = '100:199;200:299;300:399;400:499;500:599';
          return this.apiAnalyticsV2Service.getGroupBy(this.config().apiId, timeRangeParams, { ranges, field: this.config().groupByField });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.chartInput = Object.entries(res.values)
            .filter((data) => data[1] > 0)
            .map(([label, value]) => {
              return {
                label: labels[+label.charAt(0) - 1],
                value: value,
                color: colors[+label.charAt(0) - 1],
              };
            })
            .sort((a, b) => a.label.localeCompare(b.label));
          this.isLoading = false;
        },
        error: ({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
        },
      });
  }
}
