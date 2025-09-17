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

import {
  ApiAnalyticsNativeFilterBarComponent,
  ApiAnalyticsNativeFilters,
} from '../components/api-analytics-native-filter-bar/api-analytics-native-filter-bar.component';
import { ApiAnalyticsWidgetComponent, ApiAnalyticsWidgetConfig } from '../components/api-analytics-widget/api-analytics-widget.component';
import { ApiAnalyticsWidgetService } from '../api-analytics-widget.service';
import { ApiAnalyticsDashboardWidgetConfig } from '../api-analytics-proxy/api-analytics-proxy.component';
import { GioChartPieModule } from '../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { AggregationTypes, AggregationFields } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { ApiAnalyticsBaseService, BaseAnalyticsFilters } from '../api-analytics-base.service';

// Extend the native filters to work with base service
// Native only supports: plans + timeframe (period/from/to)
interface ExtendedNativeFilters extends BaseAnalyticsFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  plans: string[] | null; // Required to match ApiAnalyticsNativeFilters
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
  private baseService = inject(ApiAnalyticsBaseService);
  
  // Define supported filters for native component: plans + timeframe only
  private readonly supportedFilters: (keyof ExtendedNativeFilters)[] = ['period', 'from', 'to', 'plans'];

  public topRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public leftColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public rightColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public bottomRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];

  public activeFilters: Signal<ApiAnalyticsNativeFilters> = computed(() => 
    this.baseService.mapQueryParamsToFilters<ExtendedNativeFilters>(
      this.activatedRouteQueryParams(),
      this.supportedFilters
    )
  );

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

  apiPlans$ = this.baseService.getApiPlans$(this.apiId);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiAnalyticsWidgetService: ApiAnalyticsWidgetService,
  ) {
    effect(() => {
      this.apiAnalyticsWidgetService.setUrlParamsData(
        this.baseService.mapQueryParamsToUrlParamsData<ExtendedNativeFilters>(
          this.activatedRouteQueryParams(),
          this.supportedFilters
        )
      );
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
    this.baseService.onFiltersChange(filters as ExtendedNativeFilters, this.supportedFilters);
  }

  onRefreshFilters(): void {
    this.baseService.onRefreshFilters(this.activeFilters() as ExtendedNativeFilters, this.supportedFilters);
  }

  ngOnDestroy(): void {
    this.baseService.clearStatsCache();
  }

}
