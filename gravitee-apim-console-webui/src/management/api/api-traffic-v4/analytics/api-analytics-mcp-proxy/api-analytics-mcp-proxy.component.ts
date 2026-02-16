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

import { Component, computed, effect, inject, OnDestroy, OnInit, Signal } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, shareReplay } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';

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
import { ApiPlanV2Service } from '../../../../../services-ngx/api-plan-v2.service';
import { StatsUnitType } from '../../../../../shared/components/analytics-stats/analytics-stats.component';
import { ApiNavigationModule } from '../../../api-navigation/api-navigation.module';
import { MenuItemHeader } from '../../../api-navigation/MenuGroupItem';
import { UrlQueryParamsData } from '../../../../../services-ngx/api-analytics-v2.service';

type WidgetDisplayConfig = {
  title: string;
  statsKey?: Stats;
  statsUnit?: StatsUnitType;
  tooltip: string;
  shouldSortBuckets?: boolean;
  type: ApiAnalyticsWidgetType;
  isClickable?: boolean;
  relativePath?: string;
  minHeight?: 'small' | 'medium' | 'large';
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
  filterQueryParams?: (queryParams: ApiAnalyticsWidgetUrlParamsData) => ApiAnalyticsWidgetUrlParamsData;
  mapQueryParams?: (queryParams: ApiAnalyticsWidgetUrlParamsData) => UrlQueryParamsData;
  tableData?: {
    columns: WidgetDataConfigColumn[];
  };
};

interface QueryParamsBase {
  from?: string;
  to?: string;
  period?: string;
  httpStatuses?: string;
  plans?: string;
  applications?: string[];
}

export type ApiAnalyticsDashboardWidgetConfig = WidgetDisplayConfig & WidgetDataConfig;

@Component({
  selector: 'api-analytics-mcp-proxy',
  imports: [
    CommonModule,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsProxyFilterBarComponent,
    ApiAnalyticsWidgetComponent,
    GioChartPieModule,
    ApiNavigationModule,
    MatButtonModule,
  ],
  templateUrl: './api-analytics-mcp-proxy.component.html',
  styleUrl: './api-analytics-mcp-proxy.component.scss',
})
export class ApiAnalyticsMcpProxyComponent implements OnInit, OnDestroy {
  private readonly apiId: string = this.activatedRoute.snapshot.params.apiId;
  private activatedRouteQueryParams = toSignal(this.activatedRoute.queryParams);
  private planService = inject(ApiPlanV2Service);

  public topRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public leftColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public rightColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];

  public activeFilters: Signal<ApiAnalyticsProxyFilters> = computed(() => this.mapQueryParamsToFilters(this.activatedRouteQueryParams()));

  public menuItemHeader: MenuItemHeader = {
    title: 'API Traffic',
  };

  private topRowWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'stats',
      apiId: this.apiId,
      title: 'Total Requests',
      statsKey: 'count',
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
      title: 'Requests per second',
      statsKey: 'rps',
      tooltip: '',
      shouldSortBuckets: false,
      statsField: 'gateway-response-time-ms',
      analyticsType: 'STATS',
    },
  ];

  private leftColumnWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'bar',
      apiId: this.apiId,
      title: 'MCP Methods Usage',
      tooltip: 'Displays the distribution of HTTP status codes returned by the API',
      aggregations: [
        {
          type: AggregationTypes.FIELD,
          field: AggregationFields.MCP_METHOD,
        },
      ],
      shouldSortBuckets: true,
      analyticsType: 'HISTOGRAM',
      minHeight: 'medium',
    },
    {
      type: 'bar',
      apiId: this.apiId,
      title: 'MCP Tools Usage',
      tooltip: 'Visualizes the breakdown of used MCP tools across time',
      aggregations: [
        {
          type: AggregationTypes.FIELD,
          field: AggregationFields.MCP_TOOLS_CALL,
        },
      ],
      shouldSortBuckets: true,
      analyticsType: 'HISTOGRAM',
      minHeight: 'medium',
    },
    {
      type: 'bar',
      apiId: this.apiId,
      title: 'MCP Resources Usage',
      tooltip: 'Visualizes the breakdown of used MCP resources across time',
      aggregations: [
        {
          type: AggregationTypes.FIELD,
          field: AggregationFields.MCP_RESOURCES_READ,
        },
      ],
      shouldSortBuckets: true,
      analyticsType: 'HISTOGRAM',
      minHeight: 'medium',
    },
    {
      type: 'bar',
      apiId: this.apiId,
      title: 'MCP Prompts Usage',
      tooltip: 'Visualizes the breakdown of used MCP prompts across time',
      aggregations: [
        {
          type: AggregationTypes.FIELD,
          field: AggregationFields.MCP_PROMPTS_GET,
        },
      ],
      shouldSortBuckets: true,
      analyticsType: 'HISTOGRAM',
      minHeight: 'medium',
    },
    {
      type: 'pie',
      apiId: this.apiId,
      title: 'MCP Error Codes',
      tooltip: 'Displays the distribution of MCP error codes returned by the API',
      groupByField: 'additional-metrics.long_mcp-proxy_response-error-code',
      analyticsType: 'GROUP_BY',
      minHeight: 'medium',
      ranges: [
        { label: 'INVALID_REQUEST', value: '-32600', color: '#2B72FB' },
        { label: 'METHOD_NOT_FOUND', value: '-32601', color: '#64BDC6' },
        { label: 'INVALID_PARAMS', value: '-32602', color: '#EECA34' },
        { label: 'INTERNAL_ERROR', value: '-32603', color: '#FA4B42' },
        { label: 'RESOURCE_NOT_FOUND', value: '-32002', color: '#FE6A35' },
        { label: 'PARSE_ERROR', value: '-32700', color: '#24d911' },
      ],
    },
  ];

  private rightColumnWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'pie',
      apiId: this.apiId,
      title: 'HTTP Status Repartition',
      tooltip: 'Displays the distribution of HTTP status codes returned by the API',
      groupByField: 'status',
      analyticsType: 'GROUP_BY',
      minHeight: 'medium',
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
      minHeight: 'medium',
    },
  ];

  apiPlans$ = this.planService
    .list(this.activatedRoute.snapshot.params.apiId, undefined, ['PUBLISHED', 'DEPRECATED', 'CLOSED'], undefined, ['-flow'], 1, 9999)
    .pipe(
      map(plans => plans.data),
      shareReplay(1),
    );

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
    this.topRowTransformed$ = this.topRowWidgets.map(widgetConfig => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    this.leftColumnTransformed$ = this.leftColumnWidgets.map(widgetConfig => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    this.rightColumnTransformed$ = this.rightColumnWidgets.map(widgetConfig => {
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

  navigateToLogs(): void {
    const queryParams = this.activatedRoute.snapshot.queryParams;

    const queryParamsForLogs = {
      from: this.getFromAndToTimestamps(queryParams).fromTimestamp,
      to: this.getFromAndToTimestamps(queryParams).toTimestamp,
      statuses: queryParams.httpStatuses,
      planIds: queryParams.plans,
      applicationIds: queryParams.applications,
    };

    this.router.navigate(['../runtime-logs'], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsForLogs,
    });
  }

  private updateQueryParamsFromFilters(filters: ApiAnalyticsProxyFilters): void {
    const queryParams = this.createQueryParamsFromFilters(filters);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'replace',
    });
  }

  private createQueryParamsFromFilters(filters: ApiAnalyticsProxyFilters): Record<string, any> {
    const params: Record<string, any> = {};

    if (filters.period === 'custom' && filters.from && filters.to) {
      params.from = filters.from;
      params.to = filters.to;
      params.period = 'custom';
    } else {
      params.period = filters.period;
    }

    if (filters.httpStatuses?.length) {
      params.httpStatuses = filters.httpStatuses.join(',');
    }

    if (filters.plans?.length) {
      params.plans = filters.plans.join(',');
    }

    if (filters.applications?.length) {
      params.applications = filters.applications.join(',');
    }

    return params;
  }

  private mapQueryParamsToUrlParamsData(queryParams: unknown): ApiAnalyticsWidgetUrlParamsData {
    const params = queryParams as QueryParamsBase;
    const normalizedPeriod = params.period || '1d';
    const filters = this.getFilterFields(params);

    if (normalizedPeriod === 'custom' && params.from && params.to) {
      return <ApiAnalyticsWidgetUrlParamsData>{
        timeRangeParams: {
          from: +params.from,
          to: +params.to,
          interval: this.calculateCustomInterval(+params.from, +params.to),
        },
        ...filters,
      };
    }

    const timeFrame = timeFrames.find(tf => tf.id === normalizedPeriod) || timeFrames.find(tf => tf.id === '1d');
    return {
      timeRangeParams: timeFrame.timeFrameRangesParams(),
      ...filters,
    };
  }

  private mapQueryParamsToFilters(queryParams: unknown): ApiAnalyticsProxyFilters {
    const params = queryParams as QueryParamsBase;
    const normalizedPeriod = params.period || '1d';
    const filters = this.getFilterFields(params);

    if (normalizedPeriod === 'custom' && params.from && params.to) {
      return <ApiAnalyticsProxyFilters>{
        period: normalizedPeriod,
        from: +params.from,
        to: +params.to,
        ...filters,
      };
    }

    return {
      period: normalizedPeriod,
      from: null,
      to: null,
      ...filters,
    };
  }

  private getFilterFields(queryParams: QueryParamsBase) {
    return {
      httpStatuses: this.processFilter(queryParams.httpStatuses),
      plans: this.processFilter(queryParams.plans),
      applications: this.processFilter(queryParams.applications),
    };
  }

  private processFilter(value: string | string[] | undefined): string[] | undefined {
    if (value === undefined) {
      return undefined;
    }
    return Array.isArray(value) ? value : value.split(',');
  }

  private calculateCustomInterval(from: number, to: number, nbValuesByBucket = 30): number {
    const range: number = to - from;
    return Math.floor(range / nbValuesByBucket);
  }

  private getFromAndToTimestamps(queryParams: any) {
    let fromTimestamp: number;
    let toTimestamp: number;
    if (queryParams.period === 'custom' && queryParams.from && queryParams.to) {
      fromTimestamp = +queryParams.from;
      toTimestamp = +queryParams.to;
    } else {
      const timeFrame = timeFrames.find(tf => tf.id === queryParams.period);
      const timeRangeParams = timeFrame.timeFrameRangesParams();
      fromTimestamp = timeRangeParams.from;
      toTimestamp = timeRangeParams.to;
    }
    return { fromTimestamp, toTimestamp };
  }
}
