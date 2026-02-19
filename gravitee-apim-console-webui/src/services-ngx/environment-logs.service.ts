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
  /** Period shorthand, e.g. '-1h', '-1d'. Converted to timeRange if timeRange is not set. */
  period?: string;
  /** Filter by a specific request ID (maps to backend FilterName.REQUEST_ID) */
  requestId?: string;
  apiIds?: string[];
  applicationIds?: string[];
  planIds?: string[];
  methods?: string[];
  statuses?: number[];
  entrypoints?: string[];
  transactionId?: string;
  uri?: string;
  /** Only return logs with response time >= this value in ms */
  responseTime?: number;
};

/** Parses a period string like '-1h', '-30m', '-3d' into milliseconds. Returns null for '0' (none). */
export function periodToMs(period: string): number | null {
  if (!period || period === '0') return null;
  const match = period.match(/^-(\d+)([mhd])$/);
  if (!match) {
    // eslint-disable-next-line angular/log -- standalone utility; AngularJS $log N/A
    console.warn(`[EnvironmentLogsService] Unrecognized period format: "${period}". Falling back to default time range.`);
    return null;
  }
  const amount = parseInt(match[1], 10);
  const unit = match[2];
  const multipliers: Record<string, number> = { m: 60_000, h: 3_600_000, d: 86_400_000 };
  return amount * (multipliers[unit] ?? 0);
}

type LogFilter = { name: string; operator: string; value: string | string[] | number };

/** Builds the filters array for the backend search request body. */
function buildFilters(param?: SearchLogsParam): LogFilter[] {
  if (!param) return [];
  const filters: LogFilter[] = [];

  if (param.apiIds?.length) {
    filters.push({ name: 'API', operator: 'IN', value: param.apiIds });
  }
  if (param.applicationIds?.length) {
    filters.push({ name: 'APPLICATION', operator: 'IN', value: param.applicationIds });
  }
  if (param.planIds?.length) {
    filters.push({ name: 'PLAN', operator: 'IN', value: param.planIds });
  }
  if (param.methods?.length) {
    filters.push({ name: 'HTTP_METHOD', operator: 'IN', value: param.methods });
  }
  if (param.statuses?.length) {
    filters.push({ name: 'HTTP_STATUS', operator: 'IN', value: param.statuses.map(String) });
  }
  if (param.entrypoints?.length) {
    filters.push({ name: 'ENTRYPOINT', operator: 'IN', value: param.entrypoints });
  }
  if (param.requestId) {
    filters.push({ name: 'REQUEST_ID', operator: 'IN', value: [param.requestId] });
  }
  if (param.transactionId) {
    filters.push({ name: 'TRANSACTION_ID', operator: 'EQ', value: param.transactionId });
  }
  if (param.uri) {
    filters.push({ name: 'URI', operator: 'EQ', value: param.uri });
  }
  if (param.responseTime != null) {
    filters.push({ name: 'RESPONSE_TIME', operator: 'GTE', value: param.responseTime });
  }

  return filters;
}

@Injectable({
  providedIn: 'root',
})
export class EnvironmentLogsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) { }

  searchLogs(param?: SearchLogsParam): Observable<SearchLogsResponse> {
    let params = new HttpParams();
    params = params.append('page', param?.page ?? 1);
    params = params.append('perPage', param?.perPage ?? 10);

    const now = new Date();
    let timeRange = param?.timeRange;

    if (!timeRange && param?.period) {
      const ms = periodToMs(param.period);
      if (ms) {
        timeRange = {
          from: new Date(now.getTime() - ms).toISOString(),
          to: now.toISOString(),
        };
      }
    }

    // Default to last 24 hours when no period or timeRange specified
    if (!timeRange) {
      const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
      timeRange = {
        from: oneDayAgo.toISOString(),
        to: now.toISOString(),
      };
    }

    const filters = buildFilters(param);
    const body: Record<string, unknown> = { timeRange };
    if (filters.length > 0) {
      body['filters'] = filters;
    }

    return this.http.post<SearchLogsResponse>(`${this.constants.env.v2BaseURL}/logs/search`, body, { params });
  }
}
