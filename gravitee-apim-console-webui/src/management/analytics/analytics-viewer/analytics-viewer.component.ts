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
import { GraviteeDashboardComponent, Widget, GraviteeDashboardService, Filter } from '@gravitee/gravitee-dashboard';

import { inject, Component } from '@angular/core';

import { ApiFilterService } from './filters/api-filter.service';
import { ApplicationFilterService } from './filters/application-filter.service';

import { Constants } from '../../../entities/Constants';

@Component({
  selector: 'analytics-viewer',
  imports: [GraviteeDashboardComponent],
  templateUrl: './analytics-viewer.component.html',
  styleUrl: './analytics-viewer.component.scss',
})
export class AnalyticsViewerComponent {
  widgets: Widget[] = inject(GraviteeDashboardService).getWidgets();
  readonly baseURL = inject(Constants).env.v2BaseURL;

  private readonly apisResultsLoader = inject(ApiFilterService).resultsLoader;
  private readonly applicationsResultsLoader = inject(ApplicationFilterService).resultsLoader;

  filters: Filter[] = [
    {
      key: 'API',
      label: 'API',
      dataLoader: this.apisResultsLoader,
    },
    {
      key: 'APPLICATION',
      label: 'Application',
      dataLoader: this.applicationsResultsLoader,
    },
  ];
}
