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

export enum ApiSpecGenState {
  AVAILABLE = 'AVAILABLE',
  UNAVAILABLE = 'UNAVAILABLE',
  STARTED = 'STARTED',
  GENERATING = 'GENERATING',
}

export interface ApiSpecGenRequestState {
  state: ApiSpecGenState;
}

@Injectable({
  providedIn: 'root', // This makes it available application-wide.
})
export class ApiSpecGenService {
  constructor(
    private readonly httpClient: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public getState(apiId: string): Observable<ApiSpecGenRequestState> {
    return this.httpClient.get<ApiSpecGenRequestState>(`${this.constants.env.v2BaseURL}/apis/${apiId}/spec-gen/_state`);
  }

  public postJob(apiId: string): Observable<ApiSpecGenRequestState> {
    return this.httpClient.post<ApiSpecGenRequestState>(`${this.constants.env.v2BaseURL}/apis/${apiId}/spec-gen/_start`, null);
  }
}
