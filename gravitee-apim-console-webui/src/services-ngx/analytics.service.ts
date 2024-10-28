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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { isNil } from 'lodash';

import { Constants } from '../entities/Constants';
import { AnalyticsRequestParam } from '../entities/analytics/analyticsRequestParam';
import {
  AnalyticsCountResponse,
  AnalyticsGroupByResponse,
  AnalyticsStatsResponse,
  AnalyticsV4TopApisResponse,
} from '../entities/analytics/analyticsResponse';
import { AnalyticsResponseStatusRanges } from '../entities/management-api-v2/analytics/analyticsResponseStatusRanges';

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getStats(params: AnalyticsRequestParam): Observable<AnalyticsStatsResponse> {
    const url =
      `${this.constants.env.baseURL}/analytics?type=stats` +
      `&field=${params.field}` +
      `&interval=${params.interval}` +
      `&from=${params.from}` +
      `&to=${params.to}`;
    return this.http.get<AnalyticsStatsResponse>(url);
  }

  getGroupBy(params: AnalyticsRequestParam): Observable<AnalyticsGroupByResponse> {
    const queryParams = Object.entries(params ?? [])
      .map(([key, value]) => (isNil(value) ? null : `${key}=${value}`))
      .filter((v) => v != null);

    const url = `${this.constants.env.baseURL}/analytics?type=group_by&${queryParams.join('&')}`;
    return this.http.get<AnalyticsGroupByResponse>(url);
  }

  getCount(params: AnalyticsRequestParam): Observable<AnalyticsCountResponse> {
    const url =
      `${this.constants.env.baseURL}/analytics?type=count` +
      `&field=${params.field}` +
      `&interval=${params.interval}` +
      `&from=${params.from}` +
      `&to=${params.to}`;
    return this.http.get<AnalyticsCountResponse>(url);
  }

  getV4ApiResponseStatus(from: number, to: number): Observable<AnalyticsResponseStatusRanges> {
    const url = `${this.constants.env.v2BaseURL}/analytics/response-status-ranges?from=${from}&to=${to}`;
    return this.http.get<AnalyticsResponseStatusRanges>(url);
  }

  getV4TopApis(from: number, to: number): Observable<AnalyticsV4TopApisResponse> {
    const url = `${this.constants.env.v2BaseURL}/analytics/top-hits?from=${from}&to=${to}`;
    return this.http.get<AnalyticsV4TopApisResponse>(url);
  }
}
