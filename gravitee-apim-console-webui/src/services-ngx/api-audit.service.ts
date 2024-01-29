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
import { MetadataPage } from '../entities/MetadataPage';
import { Audit } from '../entities/audit/Audit';

export type ApiAuditFilters = {
  event?: string;
  from?: number;
  to?: number;
};

@Injectable({
  providedIn: 'root',
})
export class ApiAuditService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  getEvents(apiId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.constants.env.baseURL}/apis/${apiId}/audit/events`);
  }

  getAudit(apiId: string, filters: ApiAuditFilters, page = 1, size = 10): Observable<MetadataPage<Audit>> {
    return this.http.get<MetadataPage<Audit>>(`${this.constants.env.baseURL}/apis/${apiId}/audit?`, {
      params: this.sanitizeAuditParams(filters, page, size),
    });
  }

  private sanitizeAuditParams(filters: ApiAuditFilters, page: number, size: number) {
    return {
      page,
      size,
      ...(filters.event ? { event: filters.event } : {}),
      ...(filters.from ? { from: filters.from } : {}),
      ...(filters.to ? { to: filters.to } : {}),
    };
  }
}
