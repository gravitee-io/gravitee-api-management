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

export type LogApiType = 'HTTP_PROXY' | 'LLM_PROXY' | 'MCP_PROXY';

export type EnvironmentApiLog = {
  apiId: string;
  apiName?: string;
  apiType?: LogApiType;
  apiProductName?: string;
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
  period?: string;
  from?: string;
  to?: string;
  requestId?: string;
  apiIds?: string[];
  applicationIds?: string[];
  planIds?: string[];
  methods?: string[];
  statuses?: number[];
  entrypoints?: string[];
  transactionId?: string;
  uri?: string;
  responseTime?: number;
  errorKeys?: string[];
};

/** Parses a period string like '-1h', '-30m', '-3d' into milliseconds. Returns null for '0' (none) or unrecognized formats. */
export function periodToMs(period: string): number | null {
  if (!period || period === '0') return null;
  const match = /^-(\d+)([mhd])$/.exec(period);
  if (!match) {
    return null;
  }
  const amount = Number.parseInt(match[1], 10);
  const unit = match[2];
  const multipliers: Record<string, number> = { m: 60_000, h: 3_600_000, d: 86_400_000 };
  const multiplier = multipliers[unit];
  return multiplier ? amount * multiplier : null;
}

type LogFilter = { name: string; operator: string; value: string | string[] | number };

type SearchLogsRequestBody = {
  timeRange: TimeRange;
  filters?: LogFilter[];
};

function buildFilters(param?: SearchLogsParam): LogFilter[] {
  if (!param) return [];

  const arrayFilters: { name: string; values: string[] | undefined }[] = [
    { name: 'API', values: param.apiIds },
    { name: 'APPLICATION', values: param.applicationIds },
    { name: 'PLAN', values: param.planIds },
    { name: 'HTTP_METHOD', values: param.methods },
    { name: 'HTTP_STATUS', values: param.statuses?.map(String) },
    { name: 'ENTRYPOINT', values: param.entrypoints },
    { name: 'ERROR_KEY', values: param.errorKeys },
  ];

  const scalarFilters: { name: string; value: string | undefined }[] = [
    { name: 'REQUEST_ID', value: param.requestId },
    { name: 'TRANSACTION_ID', value: param.transactionId },
    { name: 'URI', value: param.uri },
  ];

  const filters: LogFilter[] = [
    ...arrayFilters.filter(f => f.values?.length).map(f => ({ name: f.name, operator: 'IN' as const, value: f.values! })),
    ...scalarFilters.filter(f => f.value).map(f => ({ name: f.name, operator: 'EQ' as const, value: f.value! })),
  ];

  if (param.responseTime != null && param.responseTime > 0) {
    filters.push({ name: 'RESPONSE_TIME', operator: 'GTE', value: param.responseTime });
  }

  return filters;
}

const WIDE_SEARCH_WINDOW_MS = 315_576_000_000;
const SEVEN_DAYS_MS = 7 * 86_400_000;

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

    const filters = buildFilters(param);
    const body: SearchLogsRequestBody = { timeRange: this.resolveTimeRange(param) };

    if (filters.length > 0) {
      body.filters = filters;
    }

    return this.http.post<SearchLogsResponse>(`${this.constants.env.v2BaseURL}/logs/search`, body, { params });
  }

  /**
   * Resolves the time range for a search request.
   * Precedence: explicit timeRange > from/to > period > requestId wide window > default (7 days).
   */
  private resolveTimeRange(param?: SearchLogsParam): TimeRange {
    if (param?.timeRange) {
      return param.timeRange;
    }

    const now = new Date();

    // Explicit from/to dates take priority over period
    if (param?.from && param?.to) {
      return { from: param.from, to: param.to };
    }
    if (param?.from) {
      return { from: param.from, to: now.toISOString() };
    }

    // Period 'None' — default to last 7 days
    if (param?.period === '0') {
      return { from: new Date(now.getTime() - SEVEN_DAYS_MS).toISOString(), to: now.toISOString() };
    }

    // Period shorthand (e.g. '-1h')
    if (param?.period) {
      const ms = periodToMs(param.period);
      if (ms) {
        return { from: new Date(now.getTime() - ms).toISOString(), to: now.toISOString() };
      }
    }

    // Wide window for requestId detail page searches
    if (param?.requestId) {
      const wideWindowStart = new Date(now.getTime() - WIDE_SEARCH_WINDOW_MS);
      return { from: wideWindowStart.toISOString(), to: now.toISOString() };
    }

    // Default: last 7 days
    return { from: new Date(now.getTime() - SEVEN_DAYS_MS).toISOString(), to: now.toISOString() };
  }
}
