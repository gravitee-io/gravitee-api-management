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
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { PagedResult } from '../entities/pagedResult';
import { Application, ApplicationType } from '../entities/application/application';

@Injectable({
  providedIn: 'root',
})
export class ApplicationService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  getAll(
    params: {
      environmentId?: string;
    } = {},
  ): Observable<any[]> {
    let baseURL = this.constants.env.baseURL;

    if (params.environmentId) {
      baseURL = `${this.constants.org.baseURL}/environments/${params.environmentId}`;
    }

    return this.http.get<any[]>(`${baseURL}/applications`, {
      params: {
        status: 'active',
        exclude: ['picture', 'owner'],
      },
    });
  }

  list(status?: string, _query?: string, order?: string, page = 1, size = 10): Observable<PagedResult<Application>> {
    let query = _query;
    let applicationIds = undefined;
    // Search by application id when query is a valid id
    if (_query && _query.match(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/)) {
      query = undefined;
      applicationIds = [_query];
    }

    return this.http.get<PagedResult<Application>>(`${this.constants.env.baseURL}/applications/_paged`, {
      params: {
        page,
        size,
        ...(status ? { status } : {}),
        ...(query ? { query } : {}),
        ...(applicationIds ? { ids: applicationIds } : {}),
        ...(order ? { order } : {}),
      },
    });
  }

  findByIds(ids: string[], page = 1, size = 10): Observable<PagedResult<Application>> {
    let params = new HttpParams();
    params = params.append('page', page);
    params = params.append('size', size);

    if (ids?.length > 0) {
      params = params.appendAll({ ids });
    }
    return this.http.get<PagedResult<Application>>(`${this.constants.env.baseURL}/applications/_paged`, {
      params,
    });
  }

  restore(applicationId: string): Observable<Application> {
    return this.http.post<Application>(`${this.constants.env.baseURL}/applications/${applicationId}/_restore`, {});
  }

  getById(applicationId: string): Observable<Application> {
    return this.http.get<Application>(`${this.constants.env.baseURL}/applications/${applicationId}`);
  }

  getApplicationType(applicationId: string): Observable<ApplicationType> {
    return this.http.get<ApplicationType>(`${this.constants.env.baseURL}/applications/${applicationId}/configuration`);
  }

  update(application: Application): Observable<Application> {
    return this.http.put<Application>(`${this.constants.env.baseURL}/applications/${application.id}`, {
      name: application.name,
      description: application.description,
      domain: application.domain,
      groups: application.groups,
      settings: application.settings,
      ...(application.picture !== undefined ? { picture: application.picture } : {}),
      ...(application.background !== undefined ? { background: application.background } : {}),
      disable_membership_notifications: application.disable_membership_notifications,
      api_key_mode: application.api_key_mode,
    });
  }
}
