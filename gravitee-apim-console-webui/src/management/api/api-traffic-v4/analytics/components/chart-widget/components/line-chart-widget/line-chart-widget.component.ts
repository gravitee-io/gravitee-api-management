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

import { GioChartLineModule } from '../../../../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import { GioChartLineData, GioChartLineOptions } from '../../../../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { ApiAnalyticsV2Service } from '../../../../../../../../services-ngx/api-analytics-v2.service';
import { SnackBarService } from '../../../../../../../../services-ngx/snack-bar.service';
import {
  AnalyticsHistogramAggregation,
  Bucket,
  HistogramAnalyticsResponse,
} from '../../../../../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { ChartWidgetConfig } from '../../../../../../../../entities/management-api-v2/analytics/analytics';

const namesFormatted = {
  'avg_gateway-response-time-ms': 'Gateway Response Time',
  'avg_endpoint-response-time-ms': 'Endpoint Response Time',
};

@Component({
  selector: 'line-chart-widget',
  imports: [MatCardModule, GioChartLineModule, GioLoaderModule],
  templateUrl: './line-chart-widget.component.html',
  styleUrl: './line-chart-widget.component.scss',
})
export class LineChartWidgetComponent implements OnInit {
  public chartInput: GioChartLineData[];
  public isLoading = true;
  public chartOptions: GioChartLineOptions;
  public config = input<ChartWidgetConfig>();

  constructor(
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
  ) {}

  private buildAggregationsParams(aggregations: AnalyticsHistogramAggregation[]): string {
    return aggregations.reduce((acc, aggregation, index) => {
      return acc + `${aggregation.type}:${aggregation.field}${index !== aggregations.length - 1 ? ',' : ''}`;
    }, '');
  }

  private mapResponseToChartData(res: HistogramAnalyticsResponse): GioChartLineData[] {
    return res.values
      .reduce((acc: Bucket[], value): Bucket[] => {
        return [...acc, ...value.buckets];
      }, [])
      .map(({ name, data }) => ({ name: namesFormatted[name] || name, values: data }));
  }

  ngOnInit() {
    this.apiAnalyticsV2Service
      .timeRangeFilter()
      .pipe(
        switchMap((timeRangeParams) => {
          this.isLoading = true;
          const aggregationsParams = this.buildAggregationsParams(this.config().aggregations);
          return this.apiAnalyticsV2Service.getHistogramAnalytics(this.config().apiId, aggregationsParams, timeRangeParams);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.isLoading = false;
          this.chartInput = this.mapResponseToChartData(res);

          if (this.config().shouldSortBuckets) {
            this.chartInput = this.chartInput.sort((a, b) => +a.name - +b.name);
          }

          this.chartOptions = {
            pointStart: res.timestamp.from,
            pointInterval: res.timestamp.interval,
          };
        },
        error: ({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
        },
      });
  }
}
