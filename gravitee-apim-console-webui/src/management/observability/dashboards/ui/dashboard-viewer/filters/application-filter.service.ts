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
import { SelectOption } from '@gravitee/gravitee-dashboard';

import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ResultsLoaderInput, ResultsLoaderOutput } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { ApplicationService } from '../../../../../../services-ngx/application.service';

@Injectable({
  providedIn: 'root',
})
export class ApplicationFilterService {
  private readonly itemsPerPage = 10;
  private readonly applicationService = inject(ApplicationService);

  resultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> => {
    return this.applicationService.list('ACTIVE', input.searchTerm, undefined, input.page, this.itemsPerPage).pipe(
      map(response => ({
        data: response.data.map(cluster => ({ value: cluster.id, label: cluster.name }) satisfies SelectOption),
        hasNextPage: response.page.total_pages > input.page,
      })),
    );
  };
}
