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
import { BehaviorSubject, Observable, switchMap } from 'rxjs';
import { filter } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { AnalyticsRequestsCount } from '../entities/management-api-v2/analytics/analyticsRequestsCount';
import { AnalyticsAverageConnectionDuration } from '../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { AnalyticsAverageMessagesPerRequest } from '../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest';
import { AnalyticsResponseStatusRanges } from '../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { AnalyticsResponseStatusOvertime } from '../entities/management-api-v2/analytics/analyticsResponseStatusOvertime';
import { AnalyticsResponseTimeOverTime } from '../entities/management-api-v2/analytics/analyticsResponseTimeOverTime';
import { TimeRangeParams } from '../shared/utils/timeFrameRanges';
import { ApiAnalyticsFilters } from '../management/api/api-traffic-v4/analytics/components/api-analytics-filters-bar/api-analytics-filters-bar.configuration';

export type V4AnalyticsType = 'COUNT' | 'STATS' | 'GROUP_BY' | 'DATE_HISTO';

export interface V4AnalyticsParams {
  from: number;
  to: number;
  type: V4AnalyticsType;
  field?: string;
  interval?: number;
  size?: number;
  order?: string;
}

export type V4AnalyticsResponse =
  | { type: 'COUNT'; count: number }
  | { type: 'STATS'; count: number; min: number; max: number; avg: number; sum: number }
  | { type: 'GROUP_BY'; values: Record<string, number>; metadata: Record<string, Record<string, unknown>> }
  | {
      type: 'DATE_HISTO';
      timestamp: number[];
      values: { field: string; buckets: number[]; metadata: Record<string, unknown> }[];
    };

@Injectable({
  providedIn: 'root',
})
export class ApiAnalyticsV2Service {
  public readonly defaultFilters: ApiAnalyticsFilters = { period: '1d', from: null, to: null };
  private timeRangeFilter$: BehaviorSubject<TimeRangeParams> = new BehaviorSubject<TimeRangeParams>(null);

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public timeRangeFilter(): Observable<TimeRangeParams> {
    return this.timeRangeFilter$.asObservable();
  }
  public setTimeRangeFilter(timeRangeParams: TimeRangeParams) {
    this.timeRangeFilter$.next(timeRangeParams);
  }

  getRequestsCount(apiId: string): Observable<AnalyticsRequestsCount> {
    return this.timeRangeFilter().pipe(
      filter((data) => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/requests-count?from=${from}&to=${to}`;
        return this.http.get<AnalyticsRequestsCount>(url);
      }),
    );
  }

  getAverageConnectionDuration(apiId: string): Observable<AnalyticsAverageConnectionDuration> {
    return this.timeRangeFilter().pipe(
      filter((data) => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-connection-duration?from=${from}&to=${to}`;
        return this.http.get<AnalyticsAverageConnectionDuration>(url);
      }),
    );
  }

  getAverageMessagesPerRequest(apiId: string): Observable<AnalyticsAverageMessagesPerRequest> {
    return this.timeRangeFilter().pipe(
      filter((data) => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-messages-per-request?from=${from}&to=${to}`;
        return this.http.get<AnalyticsAverageMessagesPerRequest>(url);
      }),
    );
  }

  getResponseStatusRanges(apiId: string): Observable<AnalyticsResponseStatusRanges> {
    return this.timeRangeFilter().pipe(
      filter((data) => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-status-ranges?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseStatusRanges>(url);
      }),
    );
  }

  getResponseStatusOvertime(apiId: string): Observable<AnalyticsResponseStatusOvertime> {
    return this.timeRangeFilter().pipe(
      filter((data) => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-status-overtime?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseStatusOvertime>(url);
      }),
    );
  }

  getResponseTimeOverTime(apiId: string): Observable<AnalyticsResponseTimeOverTime> {
    return this.timeRangeFilter().pipe(
      filter((data) => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-time-over-time?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseTimeOverTime>(url);
      }),
    );
  }

  /**
   * V4 API unified analytics endpoint. Data source: *-v4-metrics-* index only.
   */
  getV4Analytics(apiId: string, params: V4AnalyticsParams): Observable<V4AnalyticsResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('type', params.type);
    searchParams.set('from', String(params.from));
    searchParams.set('to', String(params.to));
    if (params.field) {
      searchParams.set('field', params.field);
    }
    if (params.interval != null) {
      searchParams.set('interval', String(params.interval));
    }
    if (params.size != null) {
      searchParams.set('size', String(params.size));
    }
    if (params.order) {
      searchParams.set('order', params.order);
    }
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics?${searchParams.toString()}`;
    return this.http.get<V4AnalyticsResponse>(url);
  }
}
