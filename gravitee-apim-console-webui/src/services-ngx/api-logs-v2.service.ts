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
import { HttpClient, HttpParams, HttpContext } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import {
  AggregatedMessageLog,
  ApiLogsParam,
  ApiLogsResponse,
  ConnectionLog,
  ConnectionLogDetail,
  PagedResult,
  Pagination,
} from '../entities/management-api-v2';
import { ACCEPT_404 } from '../shared/interceptors/http-error.interceptor';

@Injectable({
  providedIn: 'root',
})
export class ApiLogsV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  searchConnectionLogs(apiId: string, queryParam?: ApiLogsParam): Observable<ApiLogsResponse> {
    let params = new HttpParams();
    params = params.append('page', queryParam?.page ?? 1);
    params = params.append('perPage', queryParam?.perPage ?? 10);

    if (queryParam?.from) params = params.append('from', queryParam.from);
    if (queryParam?.to) params = params.append('to', queryParam.to);
    if (queryParam?.entrypointIds) params = params.append('entrypointIds', queryParam.entrypointIds);
    if (queryParam?.applicationIds) params = params.append('applicationIds', queryParam.applicationIds);
    if (queryParam?.planIds) params = params.append('planIds', queryParam.planIds);
    if (queryParam?.methods) params = params.append('methods', queryParam.methods);
    if (queryParam?.mcpMethods) params = params.append('mcpMethods', queryParam.mcpMethods);
    if (queryParam?.statuses) params = params.append('statuses', queryParam.statuses);

    return this.http.get<ApiLogsResponse>(`${this.constants.env?.v2BaseURL}/apis/${apiId}/logs`, {
      params,
    });
  }

  searchConnectionLogDetail(apiId: string, requestId: string): Observable<ConnectionLogDetail> {
    // On the details page, a 404 is a valid case (log not found) and should not trigger a snack-bar.
    const context = new HttpContext().set(ACCEPT_404, true);
    return this.http.get<ConnectionLogDetail>(`${this.constants.env?.v2BaseURL}/apis/${apiId}/logs/${requestId}`, { context });
  }

  searchMessageLogs(apiId: string, requestId: string, page = 1, perPage = 10): Observable<PagedResult<AggregatedMessageLog>> {
    return this.http.get<PagedResult<AggregatedMessageLog>>(`${this.constants.env?.v2BaseURL}/apis/${apiId}/logs/${requestId}/messages`, {
      params: {
        page,
        perPage,
      },
    });
  }

  private appendAdditionalFilter(httpParams: HttpParams, field: string, value: string): HttpParams {
    return httpParams.append('additional', `${field};${value}`);
  }

  searchApiMessageLogs(
    apiId: string,
    params?: {
      connectorId?: string;
      connectorType?: string;
      operation?: string;
      requestId?: string;
      from?: number;
      to?: number;
      page?: number;
      perPage?: number;
      applicationIds?: string[];
      statuses?: number[];
      callbackUrls?: string[];
      requiresAdditional?: boolean;
    },
  ): Observable<{ data: ConnectionLog[]; pagination: Pagination }> {
    let httpParams = new HttpParams();

    if (params?.page != null) httpParams = httpParams.set('page', String(params.page));
    if (params?.perPage != null) httpParams = httpParams.set('perPage', String(params.perPage));
    if (params?.connectorId) httpParams = httpParams.set('connectorId', params.connectorId);
    if (params?.connectorType) httpParams = httpParams.set('connectorType', params.connectorType);
    if (params?.operation) httpParams = httpParams.set('operation', params.operation);
    if (params?.requestId) httpParams = httpParams.set('requestId', params.requestId);
    if (params?.from != null) httpParams = httpParams.set('from', String(params.from));
    if (params?.to != null) httpParams = httpParams.set('to', String(params.to));
    if (params?.requiresAdditional != null) {
      httpParams = httpParams.set('requiresAdditional', String(params.requiresAdditional));
    }

    if (params?.applicationIds && params.applicationIds.length > 0) {
      const applicationIdValues = params.applicationIds.join(',');
      httpParams = this.appendAdditionalFilter(httpParams, 'keyword_webhook_app-id', applicationIdValues);
    }

    if (params?.statuses && params.statuses.length > 0) {
      const statusValues = params.statuses.map(String).join(',');
      httpParams = this.appendAdditionalFilter(httpParams, 'int_webhook_resp-status', statusValues);
    }

    if (params?.callbackUrls && params.callbackUrls.length > 0) {
      const callbackUrlValues = params.callbackUrls.join(',');
      httpParams = this.appendAdditionalFilter(httpParams, 'string_webhook_url', callbackUrlValues);
    }

    return this.http.get<{ data: ConnectionLog[]; pagination: Pagination }>(
      `${this.constants.env?.v2BaseURL}/apis/${apiId}/logs/messages`,
      { params: httpParams },
    );
  }
}
