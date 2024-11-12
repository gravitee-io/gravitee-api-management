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
import { Component, DestroyRef, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs/operators';

import { GioChartLineModule } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import { GioChartLineData, GioChartLineOptions } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-analytics-response-status-overtime',
  standalone: true,
  imports: [MatCardModule, GioChartLineModule, GioLoaderModule],
  templateUrl: './api-analytics-response-status-overtime.component.html',
  styleUrl: './api-analytics-response-status-overtime.component.scss',
})
export class ApiAnalyticsResponseStatusOvertimeComponent implements OnInit {
  public chartInput: GioChartLineData[];
  public isLoading = true;
  public chartOptions: GioChartLineOptions;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.apiAnalyticsV2Service
      .timeRangeFilter()
      .pipe(
        switchMap(() => {
          this.isLoading = true;
          return this.apiAnalyticsV2Service.getResponseStatusOvertime(this.activatedRoute.snapshot.params.apiId);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.isLoading = false;
          this.chartInput = Object.entries(res.data).map(([key, value]) => ({
            name: key,
            values: value,
          }));
          this.chartOptions = {
            pointStart: res.timeRange?.from,
            pointInterval: res.timeRange?.interval,
          };
        },
        error: ({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
        },
      });
  }
}
