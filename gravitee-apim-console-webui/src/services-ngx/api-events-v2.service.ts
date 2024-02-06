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
import { PagedResult } from '../entities/management-api-v2';
import { Event, SearchApiEventParam } from '../entities/management-api-v2/event';

@Injectable({
  providedIn: 'root',
})
export class ApiEventsV2Service {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  searchApiEvents(apiId: string, queryParam?: SearchApiEventParam): Observable<PagedResult<Event>> {
    let params = new HttpParams();
    params = params.append('page', queryParam?.page ?? 1);
    params = params.append('perPage', queryParam?.perPage ?? 10);

    if (queryParam?.from) params = params.append('from', queryParam.from);
    if (queryParam?.to) params = params.append('to', queryParam.to);
    if (queryParam?.types) params = params.append('types', queryParam.types);

    return this.http.get<PagedResult<Event>>(`${this.constants.env.v2BaseURL}/apis/${apiId}/events`, { params });
  }
}
