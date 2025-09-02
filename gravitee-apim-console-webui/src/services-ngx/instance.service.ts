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
import { Instance } from '../entities/instance/instance';
import { MonitoringData } from '../entities/instance/monitoringData';
import { SearchResult } from '../entities/instance/searchResult';
import { BaseInstance } from '../entities/management-api-v2/analytics/apiMetricsDetailResponse';

@Injectable({
  providedIn: 'root',
})
export class InstanceService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  search(includeStopped?: boolean, from?: number, to?: number, page?: number, size?: number): Observable<SearchResult> {
    if (includeStopped === undefined) {
      includeStopped = false;
    }

    if (from === undefined) {
      from = 0;
    }

    if (to === undefined) {
      to = 0;
    }

    if (page === undefined) {
      page = 0;
    }

    if (size === undefined) {
      size = 100;
    }

    return this.http.get<SearchResult>(
      `${this.constants.env.baseURL}/instances/?includeStopped=${includeStopped}&from=${from}&to=${to}&page=${page}&size=${size}`,
    );
  }

  get(id: string): Observable<Instance> {
    return this.http.get<Instance>(`${this.constants.env.baseURL}/instances/${id}`);
  }

  getByGatewayId(id: string): Observable<BaseInstance> {
    return this.http.get<Instance>(`${this.constants.env.v2BaseURL}/instances/${id}`);
  }

  getMonitoringData(id: string, gatewayId: string): Observable<MonitoringData> {
    return this.http.get<MonitoringData>(`${this.constants.env.baseURL}/instances/${id}/monitoring/${gatewayId}`);
  }
}
