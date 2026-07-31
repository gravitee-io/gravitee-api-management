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

/**
 * A filter-bar condition on its way to the logs search.
 *
 * Deliberately wider than the shared `RequestFilter`, which describes the analytics contract: the logs
 * catalog advertises names that contract's `FilterName` does not carry, and the `CONTAINS` operator it does
 * not list. Narrowing to it would force a cast at every call site and would say something untrue about what
 * this endpoint accepts.
 */
export type LogsSearchFilter = {
  name: string;
  operator: string;
  value: string | string[];
};

export type SearchLogsParam = {
  page?: number;
  perPage?: number;
  timeRange?: TimeRange;
  period?: string;
  from?: string;
  to?: string;
  requestId?: string;
  /**
   * Filter bar conditions, forwarded as-is. Deliberately not a per-field shape: the previous one enumerated a
   * handful of known filters and silently dropped every other active chip (APIM-14817). The catalog decides
   * which filters exist — this service only reshapes them for the wire.
   */
  filters?: LogsSearchFilter[];
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

/**
 * Reshapes one filter-bar condition into the wire filter for its operator.
 *
 * The search API models a filter as a `oneOf` discriminated on `operator`: `IN` carries an array, `GTE`/`LTE`
 * carry a number, everything else carries a single string. The filter bar always yields string values, and
 * collapses a single-element list to a scalar — hence the coercion here.
 *
 * A condition that cannot be reshaped raises rather than being dropped. The number input makes a non-numeric
 * bound unreachable from the dialog, but a condition also arrives from a shared or bookmarked URL, where the
 * value is whatever the link carries. Dropping it there would leave the chip looking active while contributing
 * nothing to the search — the failure this screen exists to stop being possible (APIM-14817).
 */
function toLogFilter({ name, operator, value }: LogsSearchFilter): LogFilter | null {
  const values = (Array.isArray(value) ? value : [value]).filter(v => v != null && `${v}`.length > 0).map(v => `${v}`);
  if (values.length === 0) {
    return null;
  }

  switch (operator) {
    case 'IN':
      return { name, operator, value: values };
    case 'GTE':
    case 'LTE': {
      const numeric = Number(values[0]);
      if (!Number.isFinite(numeric)) {
        throw new Error(`Filter ${name} with operator ${operator} requires a number, got "${values[0]}".`);
      }
      return { name, operator, value: numeric };
    }
    default:
      return { name, operator, value: values[0] };
  }
}

function buildFilters(param?: SearchLogsParam): LogFilter[] {
  const filters = (param?.filters ?? []).map(toLogFilter).filter((f): f is LogFilter => f !== null);

  // The detail page searches by request id alone, outside the filter bar.
  if (param?.requestId) {
    filters.push({ name: 'REQUEST_ID', operator: 'EQ', value: param.requestId });
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
