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

import { Component, computed, effect, OnDestroy, OnInit, Signal } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';

import { timeFrames } from '../../../../../shared/utils/timeFrameRanges';
import {
  ApiAnalyticsProxyFilterBarComponent,
  ApiAnalyticsProxyFilters,
} from '../components/api-analytics-proxy-filter-bar/api-analytics-proxy-filter-bar.component';
import {
  AggregationFields,
  AggregationTypes,
  AnalyticsHistogramAggregation,
} from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { GroupByField } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy';
import {
  ApiAnalyticsWidgetComponent,
  ApiAnalyticsWidgetConfig,
  ApiAnalyticsWidgetType,
} from '../components/api-analytics-widget/api-analytics-widget.component';
import { ApiAnalyticsWidgetService, ApiAnalyticsWidgetUrlParamsData } from '../api-analytics-widget.service';
import { GioChartPieModule } from '../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { Stats, StatsField } from '../../../../../entities/management-api-v2/analytics/analyticsStats';

type WidgetDisplayConfig = {
  title: string;
  statsKey?: Stats;
  statsUnit?: string;
  tooltip: string;
  shouldSortBuckets?: boolean;
  type: ApiAnalyticsWidgetType;
};

interface Range {
  label: string;
  value: string;
  color?: string;
}

export interface WidgetDataConfigColumn {
  label: string;
  dataType: 'string' | 'number';
}

type WidgetDataConfig = {
  apiId: string;
  analyticsType: 'STATS' | 'GROUP_BY' | 'HISTOGRAM';
  aggregations?: AnalyticsHistogramAggregation[];
  groupByField?: GroupByField;
  statsField?: StatsField;
  ranges?: Range[];
  orderBy?: string;
  tableData?: {
    columns: WidgetDataConfigColumn[];
  };
};

export type ApiAnalyticsDashboardWidgetConfig = WidgetDisplayConfig & WidgetDataConfig;

@Component({
  selector: 'api-analytics-proxy',
  imports: [
    CommonModule,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsProxyFilterBarComponent,
    ApiAnalyticsWidgetComponent,
    GioChartPieModule,
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent implements OnInit, OnDestroy {
  private readonly apiId: string = this.activatedRoute.snapshot.params.apiId;
  private activatedRouteQueryParams = toSignal(this.activatedRoute.queryParams);

  public topRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public leftColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public rightColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];

  public activeFilters: Signal<ApiAnalyticsProxyFilters> = computed(() => this.mapQueryParamsToFilters(this.activatedRouteQueryParams()));

  private topRowWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'stats',
      apiId: this.apiId,
      title: 'Total Requests',
      statsKey: 'count',
      statsUnit: '',
      tooltip: '',
      shouldSortBuckets: false,
      statsField: 'gateway-response-time-ms',
      analyticsType: 'STATS',
    },
    {
      type: 'stats',
      apiId: this.apiId,
      title: 'Min Response Time',
      statsKey: 'min',
      statsUnit: 'ms',
      tooltip: '',
      shouldSortBuckets: false,
      statsField: 'gateway-response-time-ms',
      analyticsType: 'STATS',
    },
    {
      type: 'stats',
      apiId: this.apiId,
      title: 'Max Response Time',
      statsKey: 'max',
      statsUnit: 'ms',
      tooltip: '',
      shouldSortBuckets: false,
      statsField: 'gateway-response-time-ms',
      analyticsType: 'STATS',
    },
    {
      type: 'stats',
      apiId: this.apiId,
      title: 'Avg Response Time',
      statsKey: 'avg',
      statsUnit: 'ms',
      tooltip: '',
      shouldSortBuckets: false,
      statsField: 'gateway-response-time-ms',
      analyticsType: 'STATS',
    },
    {
      type: 'stats',
      apiId: this.apiId,
      title: 'Requests Per Second',
      statsKey: 'rps',
      statsUnit: '',
      tooltip: '',
      shouldSortBuckets: false,
      statsField: 'gateway-response-time-ms',
      analyticsType: 'STATS',
    },
  ];

  private leftColumnWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'pie',
      apiId: this.apiId,
      title: 'HTTP Status Repartition',
      tooltip: 'Displays the distribution of HTTP status codes returned by the API',
      groupByField: 'status',
      analyticsType: 'GROUP_BY',
      ranges: [
        { label: '100-199', value: '100:199', color: '#2B72FB' },
        { label: '200-299', value: '200:299', color: '#64BDC6' },
        { label: '300-399', value: '300:399', color: '#EECA34' },
        { label: '400-499', value: '400:499', color: '#FA4B42' },
        { label: '500-599', value: '500:599', color: '#FE6A35' },
      ],
    },
    {
      type: 'line',
      apiId: this.apiId,
      aggregations: [
        {
          type: AggregationTypes.FIELD,
          field: AggregationFields.STATUS,
        },
      ],
      title: 'Response Status Over Time',
      tooltip: 'Visualizes the breakdown of HTTP status codes (2xx, 4xx, 5xx) across time',
      shouldSortBuckets: true,
      analyticsType: 'HISTOGRAM',
    },
    {
      type: 'line',
      apiId: this.apiId,
      aggregations: [
        {
          type: AggregationTypes.AVG,
          field: AggregationFields.GATEWAY_RESPONSE_TIME_MS,
          label: 'Gateway Response Time',
        },
        {
          type: AggregationTypes.AVG,
          field: AggregationFields.ENDPOINT_RESPONSE_TIME_MS,
          label: 'Endpoint Response Time',
        },
      ],
      title: 'Response Time Over Time',
      tooltip: 'Measures response time for gateway and endpoint',
      shouldSortBuckets: false,
      analyticsType: 'HISTOGRAM',
    },
    {
      type: 'line',
      apiId: this.apiId,
      title: 'Hits By Application',
      tooltip: 'Hits repartition by application',
      shouldSortBuckets: false,
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.FIELD,
          field: AggregationFields.APPLICATION_ID,
        },
      ],
    },
  ];

  private rightColumnWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'table',
      apiId: this.apiId,
      title: 'Top Applications',
      tooltip: 'Applications ranked by total API calls over time',
      shouldSortBuckets: false,
      groupByField: 'application-id',
      analyticsType: 'GROUP_BY',
      orderBy: '-count:_count',
      tableData: {
        columns: [
          { label: 'App', dataType: 'string' },
          { label: 'Requests Count', dataType: 'number' },
        ],
      },
    },
    {
      type: 'table',
      apiId: this.apiId,
      title: 'Top Api Plans',
      tooltip: 'Distribution of hits across API plans',
      shouldSortBuckets: false,
      groupByField: 'plan-id',
      analyticsType: 'GROUP_BY',
      orderBy: '-count:_count',
      tableData: {
        columns: [
          { label: 'Plan', dataType: 'string' },
          { label: 'Count', dataType: 'number' },
        ],
      },
    },
    {
      type: 'table',
      apiId: this.apiId,
      title: 'Top Paths',
      tooltip: 'Most frequently hit API paths',
      shouldSortBuckets: false,
      groupByField: 'path-info.keyword',
      analyticsType: 'GROUP_BY',
      orderBy: '-count:_count',
    },
    {
      type: 'table',
      apiId: this.apiId,
      title: 'Top Slow Applications',
      tooltip: 'Apps ranked by average response time',
      shouldSortBuckets: false,
      groupByField: 'application-id',
      analyticsType: 'GROUP_BY',
      orderBy: '-avg:gateway-response-time-ms',
      tableData: {
        columns: [
          { label: 'App', dataType: 'string' },
          { label: 'Avg. Response Time (ms)', dataType: 'number' },
        ],
      },
    },
    {
      type: 'table',
      apiId: this.apiId,
      title: 'Hits by Host (HTTP Header)',
      tooltip: 'Distribution of calls by host header (useful if you run APIs on subdomains or multi-tenant hosts)',
      shouldSortBuckets: false,
      groupByField: 'host',
      analyticsType: 'GROUP_BY',
      orderBy: '-count:_count',
      tableData: {
        columns: [
          { label: 'Host', dataType: 'string' },
          { label: 'Hits', dataType: 'number' },
        ],
      },
    },
  ];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiAnalyticsWidgetService: ApiAnalyticsWidgetService,
  ) {
    effect(() => {
      this.apiAnalyticsWidgetService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(this.activatedRouteQueryParams()));
    });
  }

  ngOnInit(): void {
    // Initialize widgets
    this.topRowTransformed$ = this.topRowWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    this.leftColumnTransformed$ = this.leftColumnWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    this.rightColumnTransformed$ = this.rightColumnWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });
  }

  onFiltersChange(filters: ApiAnalyticsProxyFilters): void {
    this.updateQueryParamsFromFilters(filters);
  }

  onRefreshFilters(): void {
    this.apiAnalyticsWidgetService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(this.activeFilters()));
  }

  ngOnDestroy(): void {
    this.apiAnalyticsWidgetService.clearStatsCache();
  }

  private updateQueryParamsFromFilters(filters: ApiAnalyticsProxyFilters): void {
    const queryParams = this.createQueryParamsFromFilters(filters);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'replace',
    });
  }

  private createQueryParamsFromFilters(filters: ApiAnalyticsProxyFilters): Record<string, any> {
    if (filters.period === 'custom' && filters.from && filters.to) {
      return {
        from: filters.from,
        to: filters.to,
        period: 'custom',
      };
    } else {
      return {
        period: filters.period,
      };
    }
  }

  private mapQueryParamsToUrlParamsData(queryParams: unknown): ApiAnalyticsWidgetUrlParamsData {
    const { from, to, period } = queryParams as { from?: string; to?: string; period?: string };
    const normalizedPeriod = period || '1d';

    if (normalizedPeriod === 'custom' && from && to) {
      return {
        timeRangeParams: {
          from: +from,
          to: +to,
          interval: this.calculateCustomInterval(+from, +to),
        },
      };
    } else {
      const timeFrame = timeFrames.find((tf) => tf.id === normalizedPeriod) || timeFrames.find((tf) => tf.id === '1d');
      return {
        timeRangeParams: timeFrame.timeFrameRangesParams(),
      };
    }
  }

  private mapQueryParamsToFilters(queryParams: unknown): ApiAnalyticsProxyFilters {
    const { from, to, period } = queryParams as { from?: string; to?: string; period?: string };
    const normalizedPeriod = period || '1d';

    if (normalizedPeriod === 'custom' && from && to) {
      return {
        period: 'custom',
        from: +from,
        to: +to,
      };
    } else {
      return {
        period: normalizedPeriod,
        from: null,
        to: null,
      };
    }
  }

  private calculateCustomInterval(from: number, to: number, nbValuesByBucket = 30): number {
    const range: number = to - from;
    return Math.floor(range / nbValuesByBucket);
  }
}
