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
import { Filter, GraviteeDashboardComponent, Widget } from '@gravitee/gravitee-dashboard';

import { Component, inject, input } from '@angular/core';

import { ApiFilterService } from './filters/api-filter.service';
import { ApplicationFilterService } from './filters/application-filter.service';

import { Constants } from '../../../../../entities/Constants';

@Component({
  selector: 'dashboard-viewer',
  imports: [GraviteeDashboardComponent],
  templateUrl: './dashboard-viewer.component.html',
  styleUrl: './dashboard-viewer.component.scss',
})
export class DashboardViewerComponent {
  widgets = input<Widget[]>([]);
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
