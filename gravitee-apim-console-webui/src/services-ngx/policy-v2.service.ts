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
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { PolicyDocumentation, PolicyListItem, PolicySchema } from '../entities/policy';
import { PolicyPlugin } from '../entities/management-api-v2';
import { ApiProtocolType } from '../entities/management-api-v2/plugin/apiProtocolType';

@Injectable({
  providedIn: 'root',
})
export class PolicyV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(): Observable<PolicyPlugin[]> {
    return this.http.get<PolicyListItem[]>(`${this.constants.org.v2BaseURL}/plugins/policies`);
  }

  getSchema(policyId: string, apiProtocolType?: ApiProtocolType): Observable<PolicySchema> {
    let params = new HttpParams();
    if (apiProtocolType) {
      params = params.append('apiProtocolType', apiProtocolType);
    }
    return this.http.get<PolicySchema>(`${this.constants.org.v2BaseURL}/plugins/policies/${policyId}/schema`, { params });
  }

  getDocumentation(policyId: string, apiProtocolType?: ApiProtocolType): Observable<PolicyDocumentation> {
    let params = new HttpParams();
    if (apiProtocolType) {
      params = params.append('apiProtocolType', apiProtocolType);
    }

    const headers = new HttpHeaders();
    headers.set('Accept', 'application/json');

    return this.http.get<PolicyDocumentation>(`${this.constants.org.v2BaseURL}/plugins/policies/${policyId}/documentation-ext`, {
      params,
      headers,
    });
  }
}
