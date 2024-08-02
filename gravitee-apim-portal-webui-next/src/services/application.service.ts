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
import { Application, ApplicationsResponse } from '../entities/application/application';

@Injectable({
  providedIn: 'root',
})
export class ApplicationService {
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  get(applicationId: string): Observable<Application> {
    return this.http.get<Application>(`${this.configService.baseURL}/applications/${applicationId}`);
  }

  list(params: { forSubscriptions?: boolean; page?: number; size?: number }): Observable<ApplicationsResponse> {
    const pageParam = params.page ? 'page=' + params.page : 'page=1';
    const perPageParam = params.size ? 'size=' + params.size : 'size=10';

    const paramList = [pageParam, perPageParam];

    if (params.forSubscriptions !== undefined) {
      paramList.push('forSubscriptions=' + params.forSubscriptions);
    }

    return this.http.get<ApplicationsResponse>(`${this.configService.baseURL}/applications?${paramList.join('&')}`);
  }
}
