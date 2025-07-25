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

import { Component } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';

import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
import { AggregationFields, AggregationTypes } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { WidgetConfig } from '../../../../../entities/management-api-v2/analytics/analytics';
import { WidgetComponent } from '../components/widget/widget.component';

@Component({
  selector: 'api-analytics-proxy',
  imports: [CommonModule, MatCardModule, GioLoaderModule, GioCardEmptyStateModule, ApiAnalyticsFiltersBarComponent, WidgetComponent],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  public tableWidgets: WidgetConfig[] = [
    {
      type: 'table',
      apiId: this.activatedRoute.snapshot.params.apiId,
      title: 'Top Applications',
      tooltip: 'Applications ranked by total API calls over time',
      shouldSortBuckets: false,
      groupByField: 'application-id',
    },
  ];

  public chartWidgets: WidgetConfig[] = [
    {
      type: 'pie',
      apiId: this.activatedRoute.snapshot.params.apiId,
      title: 'HTTP Status Repartition',
      tooltip: 'Displays the distribution of HTTP status codes returned by the API',
      groupByField: 'status',
    },
    {
      type: 'line',
      apiId: this.activatedRoute.snapshot.params.apiId,
      aggregations: [
        {
          type: AggregationTypes.FIELD,
          field: AggregationFields.STATUS,
        },
      ],
      title: 'Response Status Over Time',
      tooltip: 'Visualizes the breakdown of HTTP status codes (2xx, 4xx, 5xx) across time',
      shouldSortBuckets: true,
    },
    {
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
      shouldSortBuckets: false,
    },
  ];

  constructor(private readonly activatedRoute: ActivatedRoute) {}
}
