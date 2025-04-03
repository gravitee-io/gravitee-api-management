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
  selector: 'api-analytics-response-time-over-time',
  imports: [MatCardModule, GioChartLineModule, GioLoaderModule],
  templateUrl: './api-analytics-response-time-over-time.component.html',
  styleUrl: './api-analytics-response-time-over-time.component.scss',
})
export class ApiAnalyticsResponseTimeOverTimeComponent implements OnInit {
  private apiId = this.activatedRoute.snapshot.params.apiId;
  public isLoading = true;
  public input: GioChartLineData[];
  public options: GioChartLineOptions;

  constructor(
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
    private readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit() {
    this.apiAnalyticsV2Service
      .timeRangeFilter()
      .pipe(
        switchMap(() => {
          this.isLoading = true;
          return this.apiAnalyticsV2Service.getResponseTimeOverTime(this.apiId);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.input = [
            {
              name: 'Response time (ms)',
              values: res.data,
            },
          ];
          this.options = {
            pointStart: res.timeRange?.from,
            pointInterval: res.timeRange?.interval,
          };
          this.isLoading = false;
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
          this.isLoading = false;
        },
      });
  }
}
