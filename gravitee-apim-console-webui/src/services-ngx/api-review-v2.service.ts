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
import { switchMap } from 'rxjs/operators';

import { ApiV2Service } from './api-v2.service';

import { Constants } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class ApiReviewV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
    private readonly apiV2Service: ApiV2Service,
  ) {}

  ask(apiId: string, message?: string): Observable<void> {
    return this.http
      .post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/reviews/_ask`, { message })
      .pipe(switchMap(() => this.apiV2Service.refreshLastApiFetch()));
  }

  accept(apiId: string, message?: string): Observable<void> {
    return this.http
      .post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/reviews/_accept`, { message })
      .pipe(switchMap(() => this.apiV2Service.refreshLastApiFetch()));
  }

  reject(apiId: string, message?: string): Observable<void> {
    return this.http
      .post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/reviews/_reject`, { message })
      .pipe(switchMap(() => this.apiV2Service.refreshLastApiFetch()));
  }
}
