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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import {
  CreateSharedPolicyGroup,
  UpdateSharedPolicyGroup,
  SharedPolicyGroup,
  SharedPolicyGroupsSortByParam,
  PagedResult,
} from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class SharedPolicyGroupsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(searchQuery?: string, sortBy?: SharedPolicyGroupsSortByParam, page = 1, perPage = 25): Observable<PagedResult<SharedPolicyGroup>> {
    let params = new HttpParams();
    params = params.append('page', page);
    params = params.append('perPage', perPage);
    if (searchQuery) {
      params = params.append('q', searchQuery);
    }
    if (sortBy) {
      params = params.append('sortBy', sortBy);
    }

    return this.http.get<PagedResult<SharedPolicyGroup>>(`${this.constants.env.v2BaseURL}/shared-policy-groups`, {
      params,
    });
  }

  get(id: string): Observable<SharedPolicyGroup> {
    return this.http.get<SharedPolicyGroup>(`${this.constants.env.v2BaseURL}/shared-policy-groups/${id}`);
  }

  create(createSharedPolicyGroup: CreateSharedPolicyGroup): Observable<SharedPolicyGroup> {
    return this.http.post<SharedPolicyGroup>(`${this.constants.env.v2BaseURL}/shared-policy-groups`, createSharedPolicyGroup);
  }

  update(id: string, updateSharedPolicyGroup: UpdateSharedPolicyGroup): Observable<SharedPolicyGroup> {
    return this.http.put<SharedPolicyGroup>(`${this.constants.env.v2BaseURL}/shared-policy-groups/${id}`, updateSharedPolicyGroup);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/shared-policy-groups/${id}`);
  }

  deploy(id: string): Observable<SharedPolicyGroup> {
    return this.http.post<SharedPolicyGroup>(`${this.constants.env.v2BaseURL}/shared-policy-groups/${id}/_deploy`, undefined);
  }

  undeploy(id: string): Observable<SharedPolicyGroup> {
    return this.http.post<SharedPolicyGroup>(`${this.constants.env.v2BaseURL}/shared-policy-groups/${id}/_undeploy`, undefined);
  }
}
