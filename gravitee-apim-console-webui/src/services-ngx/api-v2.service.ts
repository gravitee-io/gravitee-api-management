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
import {
  Api,
  ApiSearchQuery,
  ApiSortByParam,
  ApisResponse,
  ApiSubscribersResponse,
  CreateApi,
  UpdateApi,
} from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ApiV2Service {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  create(newApi: CreateApi): Observable<Api> {
    return this.http.post<Api>(`${this.constants.env.v2BaseURL}/apis`, newApi);
  }

  get(id: string): Observable<Api> {
    return this.http.get<Api>(`${this.constants.env.v2BaseURL}/apis/${id}`);
  }

  update(apiId: string, api: UpdateApi): Observable<Api> {
    return this.http.put<Api>(`${this.constants.env.v2BaseURL}/apis/${apiId}`, api);
  }

  delete(apiId: string, closePlan = false): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}${closePlan ? '?closePlan=true' : ''}`);
  }

  start(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_start`, {});
  }

  stop(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_stop`, {});
  }

  export(apiId: string): Observable<Blob> {
    return this.http.get(`${this.constants.env.v2BaseURL}/apis/${apiId}/_export/definition`, {
      responseType: 'blob',
    });
  }

  search(searchQuery?: ApiSearchQuery, sortBy?: ApiSortByParam, page = 1, perPage = 10): Observable<ApisResponse> {
    return this.http.post<ApisResponse>(`${this.constants.env.v2BaseURL}/apis/_search`, searchQuery, {
      params: {
        page,
        perPage,
        ...(sortBy ? { sortBy } : {}),
      },
    });
  }

  updatePicture(apiId: string, newImage: string): Observable<void> {
    return this.http.put<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/picture`, newImage);
  }

  updateBackground(apiId: string, newImage: string): Observable<void> {
    return this.http.put<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/background`, newImage);
  }

  deletePicture(apiId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/picture`);
  }

  deleteBackground(apiId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/background`);
  }

  getSubscribers(apiId: string, name?: string, page = 1, perPage = 10): Observable<ApiSubscribersResponse> {
    return this.http.get<ApiSubscribersResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscribers`, {
      params: {
        page,
        perPage,
        ...(name ? { name } : {}),
      },
    });
  }
}
