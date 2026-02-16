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
import { map } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { DeprecatedApplicationType } from '../entities/application-type/DeprecatedApplicationType';
import { ApplicationType } from '../entities/application-type/ApplicationType';

@Injectable({
  providedIn: 'root',
})
export class ApplicationTypesService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  deprecatedGetEnabledApplicationTypes(): Observable<DeprecatedApplicationType[]> {
    return this.http
      .get<any[]>(`${this.constants.env.baseURL}/configuration/applications/types`)
      .pipe(map(response => response.map(applicationType => new DeprecatedApplicationType(applicationType))));
  }

  getEnabledApplicationTypes(): Observable<ApplicationType[]> {
    return this.http.get<ApplicationType[]>(`${this.constants.env.baseURL}/configuration/applications/types`);
  }
}
