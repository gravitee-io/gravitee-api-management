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
import { Observable, of } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, shareReplay } from 'rxjs/operators';

import { timeFrames } from '../../../../../shared/utils/timeFrameRanges';
import {
  ApiAnalyticsNativeFilterBarComponent,
  ApiAnalyticsNativeFilters,
} from '../components/api-analytics-native-filter-bar/api-analytics-native-filter-bar.component';
import { ApiAnalyticsWidgetComponent, ApiAnalyticsWidgetConfig } from '../components/api-analytics-widget/api-analytics-widget.component';
import { ApiAnalyticsWidgetService, ApiAnalyticsWidgetUrlParamsData } from '../api-analytics-widget.service';
import { ApiAnalyticsDashboardWidgetConfig } from '../api-analytics-proxy/api-analytics-proxy.component';
import { GioChartPieModule } from '../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { ApiPlanV2Service } from '../../../../../services-ngx/api-plan-v2.service';
import { GioWidgetLayoutState } from '../../../../../shared/components/gio-widget-layout/gio-widget-layout.component';
import { GioChartLineData, GioChartLineOptions } from '../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { GioChartBarData, GioChartBarOptions } from '../../../../../shared/components/gio-chart-bar/gio-chart-bar.component';
import { AggregationTypes, AggregationFields } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';

interface QueryParamsBase {
  from?: string;
  to?: string;
  period?: string;
  plans?: string;
  applications?: string[];
}

@Component({
  selector: 'api-analytics-native',
  imports: [
    CommonModule,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsNativeFilterBarComponent,
    ApiAnalyticsWidgetComponent,
    GioChartPieModule,
  ],
  templateUrl: './api-analytics-native.component.html',
  styleUrl: './api-analytics-native.component.scss',
})
export class ApiAnalyticsNativeComponent implements OnInit, OnDestroy {
  private readonly apiId: string = this.activatedRoute.snapshot.params.apiId;
  private activatedRouteQueryParams = toSignal(this.activatedRoute.queryParams);
  private planService = inject(ApiPlanV2Service);

  public topRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public leftColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public rightColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public bottomRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];

  public activeFilters: Signal<ApiAnalyticsNativeFilters> = computed(() => this.mapQueryParamsToFilters(this.activatedRouteQueryParams()));

  private topRowWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'multi-stats',
      apiId: this.apiId,
      title: 'Active Connections',
      tooltip: 'Number of active connections from clients and to broker',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.VALUE,
          field: AggregationFields.DOWNSTREAM_ACTIVE_CONNECTIONS,
          label: 'From Clients',
        },
        {
          type: AggregationTypes.VALUE,
          field: AggregationFields.UPSTREAM_ACTIVE_CONNECTIONS,
          label: 'To Broker',
        },
      ],
    },
    {
      type: 'multi-stats',
      apiId: this.apiId,
      title: 'Messages Produced',
      tooltip: 'Messages published from clients to gateway and from gateway to broker',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.DELTA,
          field: AggregationFields.DOWNSTREAM_PUBLISH_MESSAGES_TOTAL,
          label: 'From Clients',
        },
        {
          type: AggregationTypes.DELTA,
          field: AggregationFields.UPSTREAM_PUBLISH_MESSAGES_TOTAL,
          label: 'To Broker',
        },
      ],
    },
    {
      type: 'multi-stats',
      apiId: this.apiId,
      title: 'Messages Consumed',
      tooltip: 'Messages consumed from broker by gateway and delivered to clients',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.DELTA,
          field: AggregationFields.UPSTREAM_SUBSCRIBE_MESSAGES_TOTAL,
          label: 'From Broker',
        },
        {
          type: AggregationTypes.DELTA,
          field: AggregationFields.DOWNSTREAM_SUBSCRIBE_MESSAGES_TOTAL,
          label: 'To Clients',
        },
      ],
    },
  ];

  apiPlans$ = this.planService
    .list(this.activatedRoute.snapshot.params.apiId, undefined, ['PUBLISHED', 'DEPRECATED', 'CLOSED'], undefined, 1, 9999)
    .pipe(
      map((plans) => plans.data),
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
    this.topRowTransformed$ = this.topRowWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    // Initialize other widgets with mock data (to be replaced later)
    this.leftColumnTransformed$ = [
      of(this.createMockLineWidget('Message Production Rate', 'Message production rate over time')),
      of(this.createMockLineWidget('Data Production Rate', 'Data production rate over time')),
    ];

    this.rightColumnTransformed$ = [
      of(this.createMockLineWidget('Message Consumption Rate', 'Message consumption rate over time')),
      of(this.createMockLineWidget('Data Consumption Rate', 'Data consumption rate over time')),
    ];

    this.bottomRowTransformed$ = [
      of(this.createMockStackedBarWidget('Authentication Success vs. Failure', 'Authentication success and failure rates over time')),
    ];
  }

  onFiltersChange(filters: ApiAnalyticsNativeFilters): void {
    this.updateQueryParamsFromFilters(filters);
  }

  onRefreshFilters(): void {
    this.apiAnalyticsWidgetService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(this.activeFilters()));
  }

  ngOnDestroy(): void {
    this.apiAnalyticsWidgetService.clearStatsCache();
  }

  private updateQueryParamsFromFilters(filters: ApiAnalyticsNativeFilters): void {
    const queryParams = this.createQueryParamsFromFilters(filters);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'replace',
    });
  }

  private createQueryParamsFromFilters(filters: ApiAnalyticsNativeFilters): QueryParamsBase {
    const params: Partial<QueryParamsBase> = {};

    if (filters.period === 'custom' && filters.from && filters.to) {
      params.from = filters.from.toString();
      params.to = filters.to.toString();
      params.period = 'custom';
    } else {
      params.period = filters.period;
    }

    if (filters.plans?.length) {
      params.plans = filters.plans.join(',');
    }

    return params as QueryParamsBase;
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

    const timeFrame = timeFrames.find((tf) => tf.id === normalizedPeriod) || timeFrames.find((tf) => tf.id === '1d');
    return {
      timeRangeParams: timeFrame.timeFrameRangesParams(),
      httpStatuses: [],
      ...filters,
    };
  }

  private mapQueryParamsToFilters(queryParams: unknown): ApiAnalyticsNativeFilters {
    const params = queryParams as QueryParamsBase;
    const normalizedPeriod = params.period || '1d';
    const filters = this.getFilterFields(params);

    if (normalizedPeriod === 'custom' && params.from && params.to) {
      return <ApiAnalyticsNativeFilters>{
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

  // TODO: Remove these mock methods when real API is available
  private createMockStatsWidget(title: string, value: number, unit: string): ApiAnalyticsWidgetConfig {
    return {
      title,
      tooltip: `Mock ${title.toLowerCase()} data`,
      state: 'success' as GioWidgetLayoutState,
      widgetType: 'stats',
      widgetData: { stats: value, statsUnit: unit },
    };
  }

  private createMockLineWidget(title: string, tooltip: string): ApiAnalyticsWidgetConfig {
    // Different datasets for each series to make them visually distinct (15 data points each)
    const fromClientData = [45, 52, 38, 67, 44, 59, 72, 48, 63, 41, 56, 39, 74, 51, 42];

    const toBrokerData = [62, 48, 71, 34, 56, 43, 68, 52, 39, 65, 47, 73, 41, 58, 44];

    const lineData: GioChartLineData[] = [
      {
        name: 'From Client',
        values: fromClientData,
      },
      {
        name: 'To Broker',
        values: toBrokerData,
      },
    ];

    const lineOptions: GioChartLineOptions = {
      pointStart: Date.now() - 14 * 24 * 60 * 60 * 1000, // 14 days ago
      pointInterval: 24 * 60 * 60 * 1000, // 1 day in milliseconds
      enableMarkers: true,
      useSharpCorners: true,
    };

    return {
      title,
      tooltip,
      state: 'success' as GioWidgetLayoutState,
      widgetType: 'line',
      widgetData: { data: lineData, options: lineOptions },
    };
  }

  private createMockStackedBarWidget(title: string, tooltip: string): ApiAnalyticsWidgetConfig {
    // Generate hourly data for the last 24 hours
    const generateHourlyData = () => {
      // Hardcoded success data for 24 hours (1000-3000 range)
      const successData = [
        1245, 1567, 2134, 1876, 2456, 1834, 2678, 2123, 1945, 2345, 1687, 2789, 2567, 1923, 2145, 1756, 2434, 1865, 2678, 1543, 2234, 1876,
        2456, 1834,
      ];

      // Hardcoded failure data (1-5% of success values)
      const failureData = [37, 47, 64, 56, 74, 55, 80, 64, 58, 70, 51, 84, 77, 58, 64, 53, 73, 56, 80, 46, 67, 56, 74, 55];

      const categories = [];

      for (let i = 0; i < 24; i++) {
        // Create hourly categories (12:00, 13:00, etc.)
        const hour = new Date(Date.now() - (23 - i) * 60 * 60 * 1000).getHours();
        categories.push(`${hour.toString().padStart(2, '0')}:00`);
      }

      return { successData, failureData, categories };
    };

    const { successData, failureData, categories } = generateHourlyData();

    const barData: GioChartBarData[] = [
      {
        name: 'Success',
        values: successData,
        color: '#00D4AA',
      },
      {
        name: 'Failure',
        values: failureData,
        color: '#FF6B6B',
      },
    ];

    const barOptions: GioChartBarOptions = {
      categories: categories,
      stacked: true,
      reverseStack: true, // Put failure on top
      customTooltip: {
        formatter: function () {
          const x = this.x; // x-axis value
          const pointIndex = this.point.index;
          const success = successData[pointIndex];
          const failure = failureData[pointIndex];
          const failureRate = ((failure / (success + failure)) * 100).toFixed(2);

          return `
            <div style="text-align: left;">
              <strong>Time:</strong> ${x}<br/>
              <strong style="color: #00D4AA;">Success:</strong> <span style="color: #00D4AA;">${success.toLocaleString()}</span><br/>
              <strong style="color: #FF6B6B;">Failure:</strong> <span style="color: #FF6B6B;">${failure.toLocaleString()}</span><br/>
              <strong>Failure Rate:</strong> ${failureRate}%
            </div>
          `;
        },
      },
    };

    return {
      title,
      tooltip,
      state: 'success' as GioWidgetLayoutState,
      widgetType: 'bar',
      widgetData: { data: barData, options: barOptions },
    };
  }
}
