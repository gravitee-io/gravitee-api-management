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

import { Component, DestroyRef } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { of, switchMap } from 'rxjs';
import { catchError, filter, map, startWith } from 'rxjs/operators';

import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
import {
  AggregationFields,
  AggregationTypes,
  AnalyticsHistogramAggregation,
  Bucket,
  HistogramAnalyticsResponse,
} from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { WidgetConfig } from '../../../../../entities/management-api-v2/analytics/analytics';
import { WidgetComponent } from '../../../../../shared/components/widget/widget.component';
import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { TableWidgetDataItem } from '../../../../../shared/components/widget/components/table-widget/table-widget.component';
import { colors, labels } from '../../../../../shared/components/widget/components/pie-chart-widget/pie-chart-widget.component';
import { GioChartLineData } from '../../../../../shared/components/gio-chart-line/gio-chart-line.component';

export const namesFormatted = {
  'avg_gateway-response-time-ms': 'Gateway Response Time',
  'avg_endpoint-response-time-ms': 'Endpoint Response Time',
};

@Component({
  selector: 'api-analytics-proxy',
  imports: [CommonModule, MatCardModule, GioLoaderModule, GioCardEmptyStateModule, ApiAnalyticsFiltersBarComponent, WidgetComponent],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private apiId = this.activatedRoute.snapshot.params.apiId;

  private topApplicationsConfig: WidgetConfig = {
    type: 'table',
    title: 'Top Applications',
    tooltip: 'Applications ranked by total API calls over time',
    groupByField: 'application-id',
    data: [],
    isLoading: true,
  };

  public topApplicationsConfig$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
    filter((val) => !!val),
    switchMap((timeRangeParams) => {
      return this.apiAnalyticsV2Service.getGroupBy(this.apiId, timeRangeParams, { field: this.topApplicationsConfig.groupByField }).pipe(
        map((res) => {
          const data: TableWidgetDataItem[] = Object.entries(res.metadata).reduce((acc, [id, metadataRecord]) => {
            const dataItem: TableWidgetDataItem = {
              id,
              name: metadataRecord.name,
              count: res.values[id],
              isUnknown: !!metadataRecord.unknown,
            };
            return [...acc, dataItem];
          }, []);

          return { ...this.topApplicationsConfig, data, isLoading: false };
        }),
        startWith({ ...this.topApplicationsConfig, isLoading: true }),
        catchError((error) => {
          this.snackBarService.error(error.error.message);
          return of({ ...this.topApplicationsConfig, isLoading: false, data: [] }); // toDo: handle error state more explicitly when design ready
        }),
      );
    }),
    takeUntilDestroyed(this.destroyRef),
  );

  private httpStatusRepartitionConfig: WidgetConfig = {
    type: 'pie',
    title: 'HTTP Status Repartition',
    tooltip: 'Displays the distribution of HTTP status codes returned by the API',
    groupByField: 'status',
    data: [],
    isLoading: true,
  };

  public httpStatusRepartitionConfig$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
    filter((val) => !!val),
    switchMap((timeRangeParams) => {
      const ranges = '100:199;200:299;300:399;400:499;500:599';
      return this.apiAnalyticsV2Service
        .getGroupBy(this.apiId, timeRangeParams, {
          ranges,
          field: this.httpStatusRepartitionConfig.groupByField,
        })
        .pipe(
          map((res) => {
            const data = Object.entries(res.values)
              .filter(([, value]) => value > 0)
              .map(([label, value]) => {
                const index = +label.charAt(0) - 1;
                return {
                  label: labels[index],
                  value: value as number,
                  color: colors[index],
                };
              })
              .sort((a, b) => a.label.localeCompare(b.label));

            return { ...this.httpStatusRepartitionConfig, data, isLoading: false };
          }),
          startWith({ ...this.httpStatusRepartitionConfig, isLoading: true }),
          catchError((error) => {
            this.snackBarService.error(error.error.message);
            return of({ ...this.httpStatusRepartitionConfig, isLoading: false, data: [] });
          }),
        );
    }),
    takeUntilDestroyed(this.destroyRef),
  );

  private responseStatusOverTimeConfig: WidgetConfig = {
    type: 'line',
    aggregations: [
      {
        type: AggregationTypes.FIELD,
        field: AggregationFields.STATUS,
      },
    ],
    title: 'Response Status Over Time',
    tooltip: 'Visualizes the breakdown of HTTP status codes (2xx, 4xx, 5xx) across time',
    data: [],
    isLoading: true,
    chartOptions: null,
  };

  public responseStatusOverTimeConfig$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
    filter((val) => !!val),
    switchMap((timeRangeParams) => {
      return this.apiAnalyticsV2Service
        .getHistogramAnalytics(this.apiId, this.buildAggregationsParams(this.responseStatusOverTimeConfig.aggregations), timeRangeParams)
        .pipe(
          map((res) => {
            const data: GioChartLineData[] = this.mapResponseToChartLineData(res).sort((a, b) => +a.name - +b.name);
            const chartOptions = {
              pointStart: res.timestamp.from,
              pointInterval: res.timestamp.interval,
            };
            return { ...this.responseStatusOverTimeConfig, data, isLoading: false, chartOptions };
          }),
          startWith({ ...this.responseStatusOverTimeConfig, data: [], isLoading: true, chartOptions: null }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return of({ ...this.responseStatusOverTimeConfig, data: [], isLoading: false, chartOptions: null });
          }),
        );
    }),
    takeUntilDestroyed(this.destroyRef),
  );

  private responseTimeOverTimeConfig: WidgetConfig = {
    type: 'line',
    apiId: this.activatedRoute.snapshot.params.apiId,
    aggregations: [
      {
        type: AggregationTypes.AVG,
        field: AggregationFields.GATEWAY_RESPONSE_TIME_MS,
      },
      {
        type: AggregationTypes.AVG,
        field: AggregationFields.ENDPOINT_RESPONSE_TIME_MS,
      },
    ],
    title: 'Response Time Over Time',
    tooltip: 'Measures response time for gateway and endpoint',
    data: [],
    isLoading: true,
    chartOptions: null,
  };

  public responseTimeOverTimeConfig$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
    filter((val) => !!val),
    switchMap((timeRangeParams) => {
      return this.apiAnalyticsV2Service
        .getHistogramAnalytics(this.apiId, this.buildAggregationsParams(this.responseTimeOverTimeConfig.aggregations), timeRangeParams)
        .pipe(
          map((res) => {
            const data = this.mapResponseToChartLineData(res);
            const chartOptions = {
              pointStart: res.timestamp.from,
              pointInterval: res.timestamp.interval,
            };
            return { ...this.responseTimeOverTimeConfig, data, isLoading: false, chartOptions };
          }),
          startWith({ ...this.responseTimeOverTimeConfig, data: [], isLoading: true, chartOptions: null }),
          catchError(({ error }) => {
            this.snackBarService.error(error.message);
            return of({ ...this.responseTimeOverTimeConfig, data: [], isLoading: false, chartOptions: null });
          }),
        );
    }),
    takeUntilDestroyed(this.destroyRef),
  );

  constructor(
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  private mapResponseToChartLineData(res: HistogramAnalyticsResponse): GioChartLineData[] {
    return res.values
      .reduce((acc: Bucket[], value): Bucket[] => {
        return [...acc, ...value.buckets];
      }, [])
      .map(({ name, data }) => ({ name: namesFormatted[name] || name, values: data }));
  }

  private buildAggregationsParams(aggregations: AnalyticsHistogramAggregation[]): string {
    return aggregations.reduce((acc, aggregation, index) => {
      return acc + `${aggregation.type}:${aggregation.field}${index !== aggregations.length - 1 ? ',' : ''}`;
    }, '');
  }
}
