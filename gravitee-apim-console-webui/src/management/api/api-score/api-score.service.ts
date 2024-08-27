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

import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { ApiScore, ApiScoringTriggerResponse } from './api-score.model';

import { Constants } from '../../../entities/Constants';

@Injectable()
export class ApiScoreService {
  private apiScoreWithIssues: ApiScore = {
    all: 10,
    errors: 1,
    warnings: 2,
    infos: 2,
    hints: 3,
    lastEvaluation: 1,
    scoreLists: [
      {
        name: 'API Definition',
        source: 'Gravitee API definition',
        issues: [
          {
            severity: 'warning',
            location: '11:14',
            recommendation: 'Info object must have “contact” object.',
            path: 'paths/.get/',
          },
        ],
      },
      {
        name: 'Documentation page name one',
        source: 'Swagger',
        issues: [
          {
            severity: 'error',
            location: '14:13',
            recommendation: 'Operation “description” must be present and non-empty string',
            path: 'paths/.get',
          },
          {
            severity: 'warning',
            location: '16:13',
            recommendation: 'Operation mush have non-empty “tags” array. Operation..',
            path: 'paths/.get',
          },
        ],
      },
      {
        name: 'Documentation page name two',
        source: 'AsyncAPI',
        issues: [
          {
            severity: 'hint',
            location: '10:44',
            recommendation: 'Info object must have “contact” object.',
            path: 'paths/.get',
          },
          {
            severity: 'hint',
            location: '10:88',
            recommendation: 'Operation “description” must be present string.',
            path: 'paths/.get',
          },
          {
            severity: 'hint',
            location: '10:66',
            recommendation: 'Operation  have non-empty “tags” array.',
            path: 'paths/.get',
          },
          {
            severity: 'warning',
            location: '10212:66',
            recommendation: 'Operation “tags” array.',
            path: 'paths/.get',
          },
        ],
      },
    ],
  };
  private apiScoreAllClear: ApiScore = {
    all: 0,
    errors: 0,
    warnings: 0,
    infos: 0,
    hints: 0,
    lastEvaluation: 1,
    scoreLists: [
      {
        name: 'API Definition',
        source: 'Gravitee API definition 2',
        issues: [],
      },
      {
        name: 'Documentation page name ot',
        source: 'Swagger',
        issues: [],
      },
      {
        name: 'Documentation page name two',
        source: 'AsyncAPI',
        issues: [],
      },
    ],
  };

  constructor(
    private readonly httpClient: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public getWithIssues(): Observable<ApiScore> {
    return of(this.apiScoreWithIssues);
  }

  public getAllClear(): Observable<ApiScore> {
    return of(this.apiScoreAllClear);
  }

  public evaluate(apiId: string): Observable<ApiScoringTriggerResponse> {
    return this.httpClient.post<ApiScoringTriggerResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_score`, null);
  }
}
