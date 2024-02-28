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
import { map } from 'lodash';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { TopApi } from '../management/settings/top-apis/migrated/top-apis.model';

@Injectable({
  providedIn: 'root',
})
export class TopApiService {
  private readonly topApisURL = `${this.constants.env.baseURL}/configuration/top-apis/`;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private httpClient: HttpClient,
  ) {}

  getList(): Observable<TopApi[]> {
    return this.httpClient.get<TopApi[]>(this.topApisURL);
  }

  create(apiId: string): Observable<TopApi[]> {
    return this.httpClient.post<TopApi[]>(this.topApisURL, { api: apiId });
  }

  update(topApis: TopApi[]): Observable<TopApi[]> {
    return this.httpClient.put<TopApi[]>(
      this.topApisURL,
      map(topApis, (topApi: TopApi) => {
        return { api: topApi.api };
      }),
    );
  }

  delete(apiId: string): Observable<void> {
    return this.httpClient.delete<void>(this.topApisURL + apiId);
  }
}
