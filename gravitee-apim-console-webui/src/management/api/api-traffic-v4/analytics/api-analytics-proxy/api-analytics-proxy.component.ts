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

import { Component, OnInit } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';

import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
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
import { ApiAnalyticsWidgetService } from '../api-analytics-widget.service';
import { GioChartPieModule } from '../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';

type WidgetDisplayConfig = {
  title: string;
  tooltip: string;
  shouldSortBuckets?: boolean;
  type: ApiAnalyticsWidgetType;
};

interface Range {
  label: string;
  value: string;
  color?: string;
}

type WidgetDataConfig = {
  apiId: string;
  analyticsType: 'GROUP_BY' | 'HISTOGRAM';
  aggregations?: AnalyticsHistogramAggregation[];
  groupByField?: GroupByField;
  ranges?: Range[];
};

export type ApiAnalyticsDashboardWidgetConfig = WidgetDisplayConfig & WidgetDataConfig;

@Component({
  selector: 'api-analytics-proxy',
  imports: [
    CommonModule,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsFiltersBarComponent,
    ApiAnalyticsWidgetComponent,
    GioChartPieModule,
  ],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent implements OnInit {
  public leftColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public rightColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];

  private readonly apiId: string = this.activatedRoute.snapshot.params.apiId;

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
    },
    {
      type: 'table',
      apiId: this.apiId,
      title: 'Hits by Host (HTTP Header)',
      tooltip: 'Distribution of calls by host header (useful if you run APIs on subdomains or multi-tenant hosts)',
      shouldSortBuckets: false,
      groupByField: 'host',
      analyticsType: 'GROUP_BY',
    },
  ];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiAnalyticsWidgetService: ApiAnalyticsWidgetService,
  ) {}

  ngOnInit(): void {
    this.leftColumnTransformed$ = this.leftColumnWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });

    this.rightColumnTransformed$ = this.rightColumnWidgets.map((widgetConfig) => {
      return this.apiAnalyticsWidgetService.getApiAnalyticsWidgetConfig$(widgetConfig);
    });
  }
}
