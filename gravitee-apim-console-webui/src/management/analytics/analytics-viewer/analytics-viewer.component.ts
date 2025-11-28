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
import { GraviteeDashboardComponent, Widget, GraviteeDashboardService, Filter, SelectOption } from '@gravitee/gravitee-dashboard';

import { inject, Component } from '@angular/core';

import { Constants } from '../../../entities/Constants';
import { map } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ResultsLoaderInput, ResultsLoaderOutput } from '../../../shared/components/gio-select-search/gio-select-search.component';
import { ClusterService } from '../../../services-ngx/cluster.service';

@Component({
  selector: 'analytics-viewer',
  imports: [GraviteeDashboardComponent, GraviteeDashboardComponent],
  templateUrl: './analytics-viewer.component.html',
  styleUrl: './analytics-viewer.component.scss',
})
export class AnalyticsViewerComponent {
  widgets: Widget[] = inject(GraviteeDashboardService).getWidgets();
  readonly baseURL = inject(Constants).env.v2BaseURL;
  private apiV2Service = inject(ApiV2Service);

  private clusterService = inject(ClusterService);
  clusterResultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> => {
    return this.clusterService.list(undefined, undefined, input.page, 2).pipe(
      map((response) => ({
        data: response.data.map((cluster) => ({ value: cluster.id, label: cluster.name } as SelectOption)),
        hasNextPage: response.pagination.pageCount > input.page,
      })),
    );
  };

  filters: Filter[] = [
    {
      key: 'API',
      label: 'Api list',
      data$: this.apiV2Service
        .search({})
        .pipe(map((apis) => apis.data.map((app) => ({ value: app.id, label: app.id.substring(0, 10) } as SelectOption)))),
    },
    {
      key: 'PLAN',
      label: 'Plan',
      data$: of([{ value: 'plan-1', label: 'Plan 1' }]),
    },
  ];
}
