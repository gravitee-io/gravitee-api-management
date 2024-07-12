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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Constants } from '../entities/Constants';
import {
  EnvironmentFlow,
  EnvironmentFlowsSortByParam,
  fakeEnvironmentFlow,
  fakePagedResult,
  PagedResult,
} from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class EnvironmentFlowsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(searchQuery?: string, sortBy?: EnvironmentFlowsSortByParam, page = 1, perPage = 10): Observable<PagedResult<EnvironmentFlow>> {
    // TODO: implement this method when endpoint is available
    return of(
      fakePagedResult([
        fakeEnvironmentFlow({
          description: `Search query: ${searchQuery}, sortBy: ${sortBy}, page: ${page}, perPage: ${perPage}`,
        }),
      ]),
    );
  }
}
