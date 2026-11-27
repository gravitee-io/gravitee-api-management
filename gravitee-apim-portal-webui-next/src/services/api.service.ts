/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { Api } from '../entities/api/api';
import { ApisResponse } from '../entities/api/apis-response';
import { PortalApiViewParam } from '../entities/api/portal-api-view-param';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  search(page = 1, q = '', size = 9): Observable<ApisResponse> {
    return this.http.post<ApisResponse>(`${this.configService.portalBaseURL}/apis/_search`, null, {
      params: {
        page,
        size,
        q,
      },
    });
  }

  details(apiId: string): Observable<Api> {
    return this.http.get<Api>(`${this.configService.portalBaseURL}/apis/${apiId}`, {
      params: {
        [PortalApiViewParam.QUERY_PARAM_NAME]: PortalApiViewParam.DOCUMENTATION,
      },
    });
  }

  /**
   * The details of an API the user already subscribed to. A user's subscriptions are shared by every portal, so the API may not be
   * one this portal lists.
   */
  detailsForSubscribedApi(apiId: string): Observable<Api> {
    return this.http.get<Api>(`${this.configService.baseURL}/apis/${apiId}`, {
      params: {
        [PortalApiViewParam.QUERY_PARAM_NAME]: PortalApiViewParam.DOCUMENTATION,
      },
    });
  }
}
