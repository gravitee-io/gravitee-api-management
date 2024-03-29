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
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { QualityRule } from '../entities/qualityRule';

@Injectable({
  providedIn: 'root',
})
export class QualityRuleService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(): Observable<QualityRule[]> {
    return this.http.get<QualityRule[]>(`${this.constants.env.baseURL}/configuration/quality-rules`);
  }

  add(newQualityRule: QualityRule): Observable<QualityRule> {
    return this.http.post<QualityRule>(`${this.constants.env.baseURL}/configuration/quality-rules`, newQualityRule);
  }

  update(qualityRuleId: string, editedQualityRule: QualityRule): Observable<QualityRule> {
    return this.http.put<QualityRule>(`${this.constants.env.baseURL}/configuration/quality-rules/${qualityRuleId}`, editedQualityRule);
  }

  delete(qualityRuleId: string): Observable<QualityRule> {
    return this.http.delete<QualityRule>(`${this.constants.env.baseURL}/configuration/quality-rules/${qualityRuleId}`);
  }
}
