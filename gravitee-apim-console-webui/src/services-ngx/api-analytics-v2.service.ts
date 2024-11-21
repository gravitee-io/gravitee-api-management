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

import { Constants } from '../entities/Constants';
import { AnalyticsRequestsCount } from '../entities/management-api-v2/analytics/analyticsRequestsCount';
import { AnalyticsAverageConnectionDuration } from '../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { AnalyticsAverageMessagesPerRequest } from '../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest';
import { AnalyticsResponseStatusRanges } from '../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { AnalyticsResponseStatusOvertime } from '../entities/management-api-v2/analytics/analyticsResponseStatusOvertime';
import { AnalyticsResponseTimeOverTime } from '../entities/management-api-v2/analytics/analyticsResponseTimeOverTime';
import { timeFrameRangesParams, TimeRangeParams } from '../shared/utils/timeFrameRanges';

export interface DefaultFilters {
  period: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApiAnalyticsV2Service {
  public readonly defaultFilters: DefaultFilters = { period: '1d' };
  public readonly defaultTimeRangeFilter: TimeRangeParams = timeFrameRangesParams(this.defaultFilters.period);
  private timeRangeFilter$: BehaviorSubject<TimeRangeParams> = new BehaviorSubject<TimeRangeParams>(this.defaultTimeRangeFilter);

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
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/requests-count?from=${from}&to=${to}`;
        return this.http.get<AnalyticsRequestsCount>(url);
      }),
    );
  }

  getAverageConnectionDuration(apiId: string): Observable<AnalyticsAverageConnectionDuration> {
    return this.timeRangeFilter().pipe(
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-connection-duration?from=${from}&to=${to}`;
        return this.http.get<AnalyticsAverageConnectionDuration>(url);
      }),
    );
  }

  getAverageMessagesPerRequest(apiId: string): Observable<AnalyticsAverageMessagesPerRequest> {
    return this.timeRangeFilter().pipe(
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-messages-per-request?from=${from}&to=${to}`;
        return this.http.get<AnalyticsAverageMessagesPerRequest>(url);
      }),
    );
  }

  getResponseStatusRanges(apiId: string): Observable<AnalyticsResponseStatusRanges> {
    return this.timeRangeFilter().pipe(
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-status-ranges?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseStatusRanges>(url);
      }),
    );
  }

  getResponseStatusOvertime(apiId: string): Observable<AnalyticsResponseStatusOvertime> {
    return this.timeRangeFilter().pipe(
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-status-overtime?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseStatusOvertime>(url);
      }),
    );
  }

  getResponseTimeOverTime(apiId: string): Observable<AnalyticsResponseTimeOverTime> {
    return this.timeRangeFilter().pipe(
      switchMap(({ from, to }) => {
        const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-time-over-time?from=${from}&to=${to}`;
        return this.http.get<AnalyticsResponseTimeOverTime>(url);
      }),
    );
  }
}
