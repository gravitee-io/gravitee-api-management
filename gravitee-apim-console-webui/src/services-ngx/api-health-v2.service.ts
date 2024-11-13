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
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { ActiveFilter } from '../management/api/health-check-dashboard-v4/components/filters/api-health-check-dashboard-v4-filters.component';
import { Constants } from '../entities/Constants';
import {
  ApiAvailability,
  ApiAverageResponseTime,
  ApiHealthResponseTimeOvertime,
  FieldParameter,
} from '../entities/management-api-v2/api/v4/healthCheck';
import { timeFrameRangesParams, TimeRangeParams } from '../shared/utils/timeFrameRanges';

@Injectable({
  providedIn: 'root',
})
export class ApiHealthV2Service {
  public readonly defaultFilter: ActiveFilter = { timeframe: '1d' };
  public readonly defaultTimeRangeParams: TimeRangeParams = timeFrameRangesParams(this.defaultFilter.timeframe);
  private activeTimeRangeParams$: BehaviorSubject<TimeRangeParams> = new BehaviorSubject<TimeRangeParams>(this.defaultTimeRangeParams);

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private httpClient: HttpClient,
  ) {}

  public activeFilter(): Observable<TimeRangeParams> {
    return this.activeTimeRangeParams$.asObservable();
  }

  public setActiveFilter(timeRangeParams: TimeRangeParams) {
    this.activeTimeRangeParams$.next(timeRangeParams);
  }

  public getApiHealthResponseTimeOvertime(apiId: string, from: number, to: number): Observable<ApiHealthResponseTimeOvertime> {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/health/average-response-time-overtime?from=${from}&to=${to}`;
    return this.httpClient.get<ApiHealthResponseTimeOvertime>(url);
  }

  public getApiAvailability(apiId: string, from: number, to: number, field: 'endpoint' | 'gateway'): Observable<ApiAvailability> {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/health/availability?from=${from}&to=${to}&field=${field}`;
    return this.httpClient.get<ApiAvailability>(url);
  }

  public getApiAverageResponseTime(apiId: string, from: number, to: number, field: FieldParameter): Observable<ApiAverageResponseTime> {
    const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/health/average-response-time?from=${from}&to=${to}&field=${field}`;
    return this.httpClient.get<ApiAverageResponseTime>(url);
  }
}
