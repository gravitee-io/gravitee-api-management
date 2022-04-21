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

import { Audit } from '../entities/audit/Audit';
import { Constants } from '../entities/Constants';
import { MetadataPage } from '../entities/MetadataPage';

@Injectable({
  providedIn: 'root',
})
export class AuditService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  listByOrganization(
    filters: {
      event?: string;
      referenceType?: string;
      environmentId?: string;
      applicationId?: string;
      apiId?: string;
      from?: number;
      to?: number;
    } = {},
    page = 1,
    size = 10,
  ): Observable<MetadataPage<Audit>> {
    return this.http.get<MetadataPage<Audit>>(`${this.constants.org.baseURL}/audit`, {
      params: {
        page,
        size,
        ...(filters.event ? { event: filters.event } : {}),
        ...(filters.referenceType ? { type: filters.referenceType } : {}),
        ...(filters.environmentId && filters.referenceType === 'ENVIRONMENT' ? { environment: filters.environmentId } : {}),
        ...(filters.applicationId && filters.referenceType === 'APPLICATION' ? { application: filters.applicationId } : {}),
        ...(filters.apiId && filters.referenceType === 'API' ? { api: filters.apiId } : {}),
        ...(filters.from ? { from: filters.from } : {}),
        ...(filters.to ? { to: filters.to } : {}),
      },
    });
  }

  getAllEventsNameByOrganization(): Observable<string[]> {
    return this.http.get<string[]>(`${this.constants.org.baseURL}/audit/events`);
  }
}
