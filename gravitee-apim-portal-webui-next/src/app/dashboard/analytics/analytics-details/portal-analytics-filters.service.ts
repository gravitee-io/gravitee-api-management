/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  FilterDefinition,
  FilterDefinitionProvider,
  FilterValuesProvider,
  FilterValuesQuery,
  FilterValuesResult,
} from '@gravitee/gravitee-dashboard';

import { ApiService } from '../../../../services/api.service';
import { ApplicationService } from '../../../../services/application.service';

@Injectable()
export class PortalAnalyticsFiltersService implements FilterDefinitionProvider, FilterValuesProvider {
  private readonly apiService = inject(ApiService);
  private readonly applicationService = inject(ApplicationService);

  getDefinitions(): Observable<FilterDefinition[]> {
    return of([
      { name: 'API', label: $localize`:@@analyticsFilterApi:API`, type: 'KEYWORD', operators: ['EQ', 'IN'] },
      { name: 'APPLICATION', label: $localize`:@@analyticsFilterApplication:Application`, type: 'KEYWORD', operators: ['EQ', 'IN'] },
      {
        name: 'HTTP_STATUS_CODE_GROUP',
        label: $localize`:@@analyticsFilterStatusCodeGroup:Status Code Group`,
        type: 'ENUM',
        operators: ['EQ', 'IN'],
        values: ['1XX', '2XX', '3XX', '4XX', '5XX'],
      },
      {
        name: 'HTTP_STATUS',
        label: $localize`:@@analyticsFilterStatusCode:Status Code`,
        type: 'NUMBER',
        range: { min: 100, max: 599 },
        operators: ['EQ', 'LTE', 'GTE'],
      },
    ]);
  }

  getValues(query: FilterValuesQuery): Observable<FilterValuesResult> {
    switch (query.filterName) {
      case 'API':
        return this.apiService.search(query.page, 'all', query.query ?? '', query.perPage).pipe(
          map(response => ({
            data: (response.data ?? []).map(api => ({ value: api.id, label: api.name })),
            hasNextPage: (response.metadata?.pagination?.current_page ?? 1) < (response.metadata?.pagination?.total_pages ?? 1),
          })),
        );
      case 'APPLICATION':
        return this.applicationService.list(query.page, query.perPage).pipe(
          map(response => ({
            data: (response.data ?? []).map(app => ({ value: app.id, label: app.name })),
            hasNextPage: (response.metadata?.pagination?.current_page ?? 1) < (response.metadata?.pagination?.total_pages ?? 1),
          })),
        );
      default:
        return of({ data: [], hasNextPage: false });
    }
  }
}
