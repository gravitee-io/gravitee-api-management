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
import { AggregationFields, AggregationTypes } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { ApplicationService } from '../../../../../services-ngx/application.service';

interface QueryParamsBase {
  from?: string;
  to?: string;
  period?: string;
  plans?: string;
  applications?: string;
  terms: string;
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
  private applicationService = inject(ApplicationService);

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

  private leftColumnWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'line',
      apiId: this.apiId,
      title: 'Message Production Rate',
      tooltip: 'Messages published from clients to gateway and from gateway to broker over time',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.DOWNSTREAM_PUBLISH_MESSAGES_TOTAL,
          label: 'From Clients',
        },
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.UPSTREAM_PUBLISH_MESSAGES_TOTAL,
          label: 'To Broker',
        },
      ],
    },
    {
      type: 'line',
      apiId: this.apiId,
      title: 'Data Production Rate',
      tooltip: 'Data volume published from clients to gateway and from gateway to broker over time',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.DOWNSTREAM_PUBLISH_MESSAGE_BYTES,
          label: 'From Clients',
        },
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.UPSTREAM_PUBLISH_MESSAGE_BYTES,
          label: 'To Broker',
        },
      ],
    },
  ];

  private rightColumnWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'line',
      apiId: this.apiId,
      title: 'Message Consumption Rate',
      tooltip: 'Messages consumed from broker by gateway and delivered to clients over time',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.UPSTREAM_SUBSCRIBE_MESSAGES_TOTAL,
          label: 'From Broker',
        },
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.DOWNSTREAM_SUBSCRIBE_MESSAGES_TOTAL,
          label: 'To Clients',
        },
      ],
    },
    {
      type: 'line',
      apiId: this.apiId,
      title: 'Data Consumption Rate',
      tooltip: 'Data volume consumed from broker by gateway and delivered to clients over time',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.UPSTREAM_SUBSCRIBE_MESSAGE_BYTES,
          label: 'From Broker',
        },
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.DOWNSTREAM_SUBSCRIBE_MESSAGE_BYTES,
          label: 'To Clients',
        },
      ],
    },
  ];

  private bottomRowWidgets: ApiAnalyticsDashboardWidgetConfig[] = [
    {
      type: 'bar',
      apiId: this.apiId,
      title: 'Authentication Success vs. Failure',
      tooltip: 'Total authentication successes and failures over time (combined downstream + upstream)',
      analyticsType: 'HISTOGRAM',
      aggregations: [
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.DOWNSTREAM_AUTHENTICATION_SUCCESSES_TOTAL,
          label: 'Downstream Success',
        },
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.DOWNSTREAM_AUTHENTICATION_FAILURES_TOTAL,
          label: 'Downstream Failure',
        },
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.UPSTREAM_AUTHENTICATION_SUCCESSES_TOTAL,
          label: 'Upstream Success',
        },
        {
          type: AggregationTypes.TREND,
          field: AggregationFields.UPSTREAM_AUTHENTICATION_FAILURES_TOTAL,
          label: 'Upstream Failure',
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

  applications$ = this.applicationService.list(undefined, undefined, undefined, 1, 200).pipe(
    map((response) => response.data),
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

    this.leftColumnTransformed$ = this.leftColumnWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    this.rightColumnTransformed$ = this.rightColumnWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    this.bottomRowTransformed$ = this.bottomRowWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });
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

    let plans: string = null;
    let applications: string = null;

    if (filters.plans?.length) {
      params.plans = filters.plans.join(',');
      plans = filters.plans.map((planId) => `plan-id:${planId}`).join(',');
    }
    if (filters.applications?.length) {
      params.applications = filters.applications.join(',');
      applications = filters.applications.map((appId) => `app-id:${appId}`).join(',');
    }
    params.terms = [plans, applications].filter((term) => term).join(',');

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

    const filterBucket: Record<string, string[]> = {
      'app-id': [],
      'plan-id': [],
    };

    if (filters.terms?.length) {
      for (const term of filters.terms) {
        const [key, value] = term.split(':', 2);

        if (value && filterBucket[key] && !filterBucket[key].includes(value)) {
          filterBucket[key].push(value);
        }
      }
    }

    if (normalizedPeriod === 'custom' && params.from && params.to) {
      return <ApiAnalyticsNativeFilters>{
        period: normalizedPeriod,
        from: +params.from,
        to: +params.to,
        plans: filterBucket['plan-id'],
        applications: filterBucket['app-id'],
      };
    }

    return {
      period: normalizedPeriod,
      from: null,
      to: null,
      plans: filterBucket['plan-id'],
      applications: filterBucket['app-id'],
    };
  }

  private getFilterFields(queryParams: QueryParamsBase) {
    return {
      plans: this.processFilter(queryParams.plans),
      applications: this.processFilter(queryParams.applications),
      terms: this.processFilter(queryParams.terms),
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
}
