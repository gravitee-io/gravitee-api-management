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
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { CustomUserField } from '../entities/customUserFields';

@Injectable({
  providedIn: 'root',
})
export class CustomUserFieldsService {
  constructor(
    private readonly httpClient: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(): Observable<CustomUserField[]> {
    return this.httpClient.get<CustomUserField[]>(`${this.constants.org.baseURL}/configuration/custom-user-fields`);
  }

  create(field: CustomUserField): Observable<CustomUserField> {
    return this.httpClient.post<CustomUserField>(`${this.constants.org.baseURL}/configuration/custom-user-fields`, field);
  }

  update(field: CustomUserField): Observable<CustomUserField> {
    return this.httpClient.put<CustomUserField>(`${this.constants.org.baseURL}/configuration/custom-user-fields/${field.key}`, field);
  }

  delete(key: string) {
    return this.httpClient.delete(`${this.constants.org.baseURL}/configuration/custom-user-fields/${key}`);
  }
}
