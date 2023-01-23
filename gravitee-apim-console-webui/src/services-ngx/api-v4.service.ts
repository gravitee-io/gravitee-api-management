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
import { ApiEntity, NewApiEntity } from '../entities/api-v4';

@Injectable({
  providedIn: 'root',
})
export class ApiV4Service {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  create(newApiEntity: NewApiEntity): Observable<ApiEntity> {
    return this.http.post<ApiEntity>(`${this.constants.env.baseURL}/v4/apis`, newApiEntity);
  }

  get(id: string): Observable<ApiEntity> {
    return this.http.get<ApiEntity>(`${this.constants.env.baseURL}/v4/apis/${id}`);
  }
}
