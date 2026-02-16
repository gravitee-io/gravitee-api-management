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
import { BehaviorSubject, Observable, switchMap, filter } from 'rxjs';

import { Constants } from '../entities/Constants';
import { AnalyticsRequestsCount } from '../entities/management-api-v2/analytics/analyticsRequestsCount';
import { AnalyticsAverageConnectionDuration } from '../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { AnalyticsAverageMessagesPerRequest } from '../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest';
import { AnalyticsResponseStatusRanges } from '../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { AnalyticsResponseTimeOverTime } from '../entities/management-api-v2/analytics/analyticsResponseTimeOverTime';
import { TimeRangeParams } from '../shared/utils/timeFrameRanges';
import { ApiAnalyticsMessageFilters } from '../management/api/api-traffic-v4/analytics/components/api-analytics-message-filters-bar/api-analytics-message-filters-bar.configuration';
import { HistogramAnalyticsResponse } from '../entities/management-api-v2/analytics/analyticsHistogram';
import { GroupByField, GroupByResponse } from '../entities/management-api-v2/analytics/analyticsGroupBy';
import { AnalyticsStatsResponse, StatsField } from '../entities/management-api-v2/analytics/analyticsStats';
import { ApiMetricsDetailResponse } from '../entities/management-api-v2/analytics/apiMetricsDetailResponse';

export interface UrlQueryParamsData {
  field?: GroupByField | StatsField;
  order?: string;
  ranges?: string;
  query?: string;
  terms?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApiAnalyticsV2Service {
  public readonly defaultFilters: ApiAnalyticsMessageFilters = { period: '1d', from: null, to: null };
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
      filter(data => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/requests-count?from=${from}&to=${to}`;
        return this.http.get<AnalyticsRequestsCount>(url);
      }),
    );
  }

  getAverageConnectionDuration(apiId: string): Observable<AnalyticsAverageConnectionDuration> {
    return this.timeRangeFilter().pipe(
      filter(data => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-connection-duration?from=${from}&to=${to}`;
        return this.http.get<AnalyticsAverageConnectionDuration>(url);
      }),
    );
  }

  getAverageMessagesPerRequest(apiId: string): Observable<AnalyticsAverageMessagesPerRequest> {
    return this.timeRangeFilter().pipe(
      filter(data => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-messages-per-request?from=${from}&to=${to}`;
        return this.http.get<AnalyticsAverageMessagesPerRequest>(url);
      }),
    );
  }

  getResponseStatusRanges(apiId: string): Observable<AnalyticsResponseStatusRanges> {
    return this.timeRangeFilter().pipe(
      filter(data => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-status-ranges?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseStatusRanges>(url);
      }),
    );
  }

  getResponseTimeOverTime(apiId: string): Observable<AnalyticsResponseTimeOverTime> {
    return this.timeRangeFilter().pipe(
      filter(data => !!data),
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-time-over-time?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseTimeOverTime>(url);
      }),
    );
  }

  getHistogramAnalytics(
    apiId: string,
    aggregations: string,
    { from, to, interval }: TimeRangeParams,
    urlParamsData: UrlQueryParamsData = {},
  ) {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics?type=HISTOGRAM&from=${from}&to=${to}&interval=${interval}&aggregations=${aggregations}${this.buildUrlParams({ ...urlParamsData })}`;
    return this.http.get<HistogramAnalyticsResponse>(url);
  }

  getGroupBy(apiId: string, { from, to, interval }: TimeRangeParams, urlParamsData: UrlQueryParamsData = {}) {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics?type=GROUP_BY&from=${from}&to=${to}&interval=${interval}${this.buildUrlParams({ ...urlParamsData })}`;
    return this.http.get<GroupByResponse>(url);
  }

  getStats(apiId: string, { from, to, interval }: TimeRangeParams, urlParamsData: UrlQueryParamsData = {}) {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics?type=STATS&from=${from}&to=${to}&interval=${interval}${this.buildUrlParams({ ...urlParamsData })}`;
    return this.http.get<AnalyticsStatsResponse>(url);
  }

  getApiMetricsDetail(apiId: string, requestId: string): Observable<ApiMetricsDetailResponse> {
    return this.http.get<ApiMetricsDetailResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/${requestId}`);
  }

  buildUrlParams(params: UrlQueryParamsData) {
    const { order, ranges, field, query, terms } = params;

    let url = '';

    if (field) {
      url += `&field=${field}`;
    }

    if (order) {
      url += `&order=${order}`;
    }

    if (ranges) {
      url += `&ranges=${ranges}`;
    }

    if (query) {
      url += `&query=${query}`;
    }

    if (terms) {
      url += `&terms=${terms}`;
    }

    return url;
  }
}
