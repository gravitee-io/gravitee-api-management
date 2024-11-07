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

import { GioChartLineModule } from '../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioChartLineData, GioChartLineOptions } from '../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { ApiHealthV2Service } from '../../../../../services-ngx/api-health-v2.service';
import { TimeRangeParams } from '../../../../../shared/utils/timeFrameRanges';

@Component({
  selector: 'app-global-response-time-trend',
  standalone: true,
  imports: [MatCardModule, GioLoaderModule, GioChartLineModule],
  templateUrl: './global-response-time-trend.component.html',
  styleUrl: './global-response-time-trend.component.scss',
})
export class GlobalResponseTimeTrendComponent implements OnInit {
  public isLoading = true;
  public filters: TimeRangeParams;
  private apiId = this.activatedRoute.snapshot.params.apiId;

  public input: GioChartLineData[];
  public options: GioChartLineOptions;

  constructor(
    private readonly destroyRef: DestroyRef,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
    public readonly apiHealthV2Service: ApiHealthV2Service,
  ) {}

  ngOnInit() {
    this.apiHealthV2Service
      .activeFilter()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.getData(data.from, data.to);
        },
      });
  }

  getData(from: number, to: number) {
    this.isLoading = true;
    this.apiHealthV2Service
      .getApiHealthResponseTimeOvertime(this.apiId, from, to)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.input = [
            {
              name: 'Response time (ms)',
              values: res.data,
            },
          ];
          this.options = {
            pointStart: res.timeRange.from,
            pointInterval: res.timeRange.interval,
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
