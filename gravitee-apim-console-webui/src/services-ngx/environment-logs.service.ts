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

/**
 * Matches the backend EnvironmentApiLog schema from openapi-logs.yaml.
 */
export type EnvironmentApiLog = {
  apiId: string;
  timestamp: string;
  id: string;
  requestId: string;
  method: string;
  clientIdentifier?: string;
  plan?: { id: string; name?: string };
  application?: { id: string; name?: string };
  transactionId?: string;
  status: number;
  requestEnded: boolean;
  gatewayResponseTime?: number;
  gateway?: string;
  uri?: string;
  endpoint?: string;
  message?: string;
  errorKey?: string;
  errorComponentName?: string;
  errorComponentType?: string;
  warnings?: Array<{ componentType?: string; componentName?: string; key?: string; message?: string }>;
  additionalMetrics?: Record<string, unknown>;
};

export type SearchLogsResponse = {
  data: EnvironmentApiLog[];
  pagination: {
    page: number;
    perPage: number;
    pageCount: number;
    pageItemsCount: number;
    totalCount: number;
  };
};

export type TimeRange = {
  from: string;
  to: string;
};

export type SearchLogsParam = {
  page?: number;
  perPage?: number;
  timeRange?: TimeRange;
  /** Filter by a specific request ID (maps to backend FilterName.REQUEST_ID) */
  requestId?: string;
};

@Injectable({
  providedIn: 'root',
})
export class EnvironmentLogsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  searchLogs(param?: SearchLogsParam): Observable<SearchLogsResponse> {
    let params = new HttpParams();
    params = params.append('page', param?.page ?? 1);
    params = params.append('perPage', param?.perPage ?? 10);

    const now = new Date();
    // Default to last 24 hours. Note: when filtering by requestId, callers should pass
    // an explicit timeRange if the log may be older than 24 hours.
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);

    const filters = [];
    if (param?.requestId) {
      filters.push({ name: 'REQUEST_ID', operator: 'EQ', value: param.requestId });
    }

    const body: Record<string, unknown> = {
      timeRange: param?.timeRange ?? {
        from: oneDayAgo.toISOString(),
        to: now.toISOString(),
      },
    };
    if (filters.length > 0) {
      body['filters'] = filters;
    }

    return this.http.post<SearchLogsResponse>(`${this.constants.env.v2BaseURL}/logs/search`, body, { params });
  }
}
