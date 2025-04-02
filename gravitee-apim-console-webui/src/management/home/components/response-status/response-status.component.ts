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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCard, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { switchMap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

import { GioChartLineData, GioChartLineOptions } from '../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { AnalyticsService } from '../../../../services-ngx/analytics.service';
import { HomeService } from '../../../../services-ngx/home.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioChartLineModule } from '../../../../shared/components/gio-chart-line/gio-chart-line.module';
import { TimeRangeParams } from '../../../../shared/utils/timeFrameRanges';

@Component({
  selector: 'response-status',
  imports: [GioChartLineModule, GioLoaderModule, MatCard, MatCardHeader, MatCardSubtitle, MatCardTitle],
  templateUrl: './response-status.component.html',
  styleUrl: './response-status.component.scss',
})
export class ResponseStatusComponent implements OnInit {
  public isLoading = true;
  public chartInput: GioChartLineData[];
  public chartOptions: GioChartLineOptions;

  constructor(
    private readonly analyticsService: AnalyticsService,
    private readonly homeService: HomeService,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.homeService
      .timeRangeParams()
      .pipe(
        switchMap(({ from, to }: TimeRangeParams) => {
          this.isLoading = true;
          return this.analyticsService.getV4ResponseStatus({
            from,
            to,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res): void => {
          this.chartInput = Object.entries(res.data).map(([key, value]) => ({
            name: key,
            values: value,
          }));
          this.chartOptions = {
            pointStart: res.timeRange?.from,
            pointInterval: res.timeRange?.interval,
          };
          this.isLoading = false;
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        },
      });
  }
}
