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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
  private apiId = this.activatedRoute.snapshot.params.apiId;
  private destroyRef = inject(DestroyRef);

  // toDo:  default we take one day, remove hardcoded values when time filters is developed, by
  private MS_IN_DAY = 24 * 3600 * 1000;
  private timeRange = {
    to: new Date().getTime(),
    from: new Date().getTime() - this.MS_IN_DAY,
  };

  public input: GioChartLineData[];
  public isLoading = true;
  public options: GioChartLineOptions;

  constructor(
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.getData();
  }

  getData() {
    this.apiAnalyticsV2Service
      .getResponseStatusOvertime(this.apiId, this.timeRange.from, this.timeRange.to)
      .pipe(
        tap(() => (this.isLoading = true)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.isLoading = false;
          this.input = Object.entries(res.data).map(([key, value]) => ({
            name: key,
            values: value,
          }));
          this.options = {
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
