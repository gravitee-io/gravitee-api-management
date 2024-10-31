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
import { AnalyticsRequestsCount } from '../entities/management-api-v2/analytics/analyticsRequestsCount';
import { AnalyticsAverageConnectionDuration } from '../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { AnalyticsAverageMessagesPerRequest } from '../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest';
import { AnalyticsResponseStatusRanges } from '../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { AnalyticsResponseStatusOvertime } from '../entities/management-api-v2/analytics/analyticsResponseStatusOvertime';
import { AnalyticsResponseTimeOverTime } from '../entities/management-api-v2/analytics/analyticsResponseTimeOverTime';

@Injectable({
  providedIn: 'root',
})
export class ApiAnalyticsV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getRequestsCount(apiId: string): Observable<AnalyticsRequestsCount> {
    return this.http.get<AnalyticsRequestsCount>(`${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/requests-count`);
  }

  getAverageConnectionDuration(apiId: string): Observable<AnalyticsAverageConnectionDuration> {
    return this.http.get<AnalyticsAverageConnectionDuration>(
      `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-connection-duration`,
    );
  }

  getAverageMessagesPerRequest(apiId: string): Observable<AnalyticsAverageMessagesPerRequest> {
    return this.http.get<AnalyticsAverageMessagesPerRequest>(
      `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/average-messages-per-request`,
    );
  }

  getResponseStatusRanges(apiId: string): Observable<AnalyticsResponseStatusRanges> {
    return this.http.get<AnalyticsResponseStatusRanges>(`${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-status-ranges`);
  }

  getResponseStatusOvertime(apiId: string, from: number, to: number): Observable<AnalyticsResponseStatusOvertime> {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-status-overtime?from=${from}&to=${to}`;
    return this.http.get<AnalyticsResponseStatusOvertime>(url);
  }

  getResponseTimeOverTime(apiId: string, from: number, to: number): Observable<AnalyticsResponseTimeOverTime> {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics/response-time-over-time?from=${from}&to=${to}`;
    return this.http.get<AnalyticsResponseTimeOverTime>(url);
  }
}
