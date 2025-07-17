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
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { ChartWidgetComponent, ChartWidgetConfig } from '../components/chart-widget/chart-widget.component';
import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { onlyApiV4Filter } from '../../../../../util/apiFilter.operator';
import { AggregationFields, AggregationTypes } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';

@Component({
  selector: 'api-analytics-proxy',
  imports: [CommonModule, MatCardModule, GioLoaderModule, GioCardEmptyStateModule, ApiAnalyticsFiltersBarComponent, ChartWidgetComponent],
  templateUrl: './api-analytics-proxy.component.html',
  styleUrl: './api-analytics-proxy.component.scss',
})
export class ApiAnalyticsProxyComponent {
  private isAnalyticsEnabled$: Observable<boolean> = this.apiService.getLastApiFetch(this.activatedRoute.snapshot.params.apiId).pipe(
    onlyApiV4Filter(),
    map((api) => {
      return api.analytics.enabled;
    }),
  );

  public isAnalyticsEnabled = toSignal(this.isAnalyticsEnabled$, { initialValue: false });

  public chartWidgetConfigs: ChartWidgetConfig[] = [
    {
      apiId: this.activatedRoute.snapshot.params.apiId,
      aggregationType: AggregationTypes.FIELD,
      aggregationField: AggregationFields.STATUS,
      title: 'Response Status Over Time',
      tooltip: 'Visualizes the breakdown of HTTP status codes (2xx, 4xx, 5xx) across time',
      shouldSortBuckets: true,
    },
    {
      apiId: this.activatedRoute.snapshot.params.apiId,
      aggregationType: AggregationTypes.AVG,
      aggregationField: AggregationFields.GATEWAY_RESPONSE_TIME_MS,
      title: 'Response Time Over Time',
      tooltip: 'Measures latency trend for gateway and downstream systems (API) ',
    },
  ];

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}
}
