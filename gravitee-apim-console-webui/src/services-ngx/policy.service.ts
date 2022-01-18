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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { PolicyServiceAbstract } from '@gravitee/ui-policy-studio-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { PolicyDocumentation, PolicyListItem, PolicySchema } from '../entities/policy';

interface ListParams {
  expandSchema?: boolean;
  expandIcon?: boolean;
  withoutResource?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class PolicyService implements PolicyServiceAbstract {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  list(params: ListParams): Observable<PolicyListItem[]> {
    let httpParams = new HttpParams();

    if (params.expandSchema) {
      httpParams = httpParams.append('expand', 'schema');
    }
    if (params.expandIcon) {
      httpParams = httpParams.append('expand', 'icon');
    }
    if (params.withoutResource) {
      httpParams = httpParams.set('withResource', false);
    }

    return this.http.get<PolicyListItem[]>(`${this.constants.env.baseURL}/policies`, {
      params: httpParams,
    });
  }

  listSwaggerPolicies(): Observable<PolicyListItem[]> {
    return this.http.get<PolicyListItem[]>(`${this.constants.env.baseURL}/policies/swagger`);
  }

  getSchema(policyId: string): Observable<PolicySchema> {
    return this.http.get<PolicySchema>(`${this.constants.env.baseURL}/policies/${policyId}/schema`);
  }

  getDocumentation(policyId: string): Observable<PolicyDocumentation> {
    return this.http
      .get(`${this.constants.env.baseURL}/policies/${policyId}/documentation`, {
        responseType: 'text',
      })
      .pipe(map((buffer) => buffer.toString()));
  }
}
