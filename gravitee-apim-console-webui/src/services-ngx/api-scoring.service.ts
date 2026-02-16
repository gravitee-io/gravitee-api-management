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

import { HttpClient, HttpContext } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { ApiScoring, ApiScoringTriggerResponse } from '../management/api/scoring/api-scoring.model';
import { Constants } from '../entities/Constants';
import { ApisScoringOverview, ApisScoringResponse } from '../management/api-score/api-score.model';
import { ACCEPT_404 } from '../shared/interceptors/http-error.interceptor';

@Injectable()
export class ApiScoringService {
  constructor(
    private readonly httpClient: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public getApiScoring(apiId: string): Observable<ApiScoring> {
    const context = new HttpContext().set(ACCEPT_404, true);
    return this.httpClient.get<ApiScoring>(`${this.constants.env.v2BaseURL}/apis/${apiId}/scoring`, { context }).pipe(
      catchError(err => {
        if (err.status === 404) {
          return of(undefined);
        }
        throw err;
      }),
    );
  }

  public evaluate(apiId: string): Observable<ApiScoringTriggerResponse> {
    return this.httpClient.post<ApiScoringTriggerResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/scoring/_evaluate`, null);
  }

  public getApisScoringList(page: number, size: number): Observable<ApisScoringResponse> {
    return this.httpClient.get<ApisScoringResponse>(`${this.constants.env.v2BaseURL}/scoring/apis?page=${page}&perPage=${size}`);
  }

  public getApisScoringOverview(): Observable<ApisScoringOverview> {
    return this.httpClient.get<ApisScoringOverview>(`${this.constants.env.v2BaseURL}/scoring/overview`);
  }
}
