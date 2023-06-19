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
import { ApiSubscriptionsResponse, Subscription } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ApiSubscriptionV2Service {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  list(
    apiId: string,
    page = '1',
    perPage = '10',
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apikey?: string,
    expands?: ('plan' | 'application')[],
  ): Observable<ApiSubscriptionsResponse> {
    return this.http.get<ApiSubscriptionsResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions`, {
      params: {
        page,
        perPage,
        ...(statuses && statuses.length > 0 ? { statuses: statuses.join(',') } : {}),
        ...(applicationIds && applicationIds.length > 0 ? { applicationIds: applicationIds.join(',') } : {}),
        ...(planIds && planIds.length > 0 ? { planIds: planIds.join(',') } : {}),
        ...(apikey ? { apikey: apikey } : {}),
        ...(expands ? { expands: expands.join(',') } : {}),
      },
    });
  }

  getById(apiId: string, subscriptionId: string, expands: string[] = []): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}?expands=${expands}`);
  }
}
