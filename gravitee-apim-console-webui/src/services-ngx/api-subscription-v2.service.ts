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
import {
  AcceptSubscription,
  ApiSubscriptionsResponse,
  CreateSubscription,
  Subscription,
  UpdateSubscription,
  VerifySubscription,
  VerifySubscriptionResponse,
} from '../entities/management-api-v2';
import { ApiKey, SubscriptionApiKeysResponse } from '../entities/management-api-v2/api-key';

@Injectable({
  providedIn: 'root',
})
export class ApiSubscriptionV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(
    apiId: string,
    page = '1',
    perPage = '10',
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apiKey?: string,
    expands?: ('plan' | 'application')[],
  ): Observable<ApiSubscriptionsResponse> {
    return this.http.get<ApiSubscriptionsResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions`, {
      params: {
        page,
        perPage,
        ...(statuses && statuses.length > 0 ? { statuses: statuses.join(',') } : {}),
        ...(applicationIds && applicationIds.length > 0 ? { applicationIds: applicationIds.join(',') } : {}),
        ...(planIds && planIds.length > 0 ? { planIds: planIds.join(',') } : {}),
        ...(apiKey ? { apiKey: apiKey } : {}),
        ...(expands ? { expands: expands.join(',') } : {}),
      },
    });
  }

  exportAsCSV(
    apiId: string,
    page = '1',
    perPage = '10',
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apiKey?: string,
  ): Observable<Blob> {
    return this.http.get(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/_export`, {
      responseType: 'blob',
      params: {
        page,
        perPage,
        ...(statuses && statuses.length > 0 ? { statuses: statuses.join(',') } : {}),
        ...(applicationIds && applicationIds.length > 0 ? { applicationIds: applicationIds.join(',') } : {}),
        ...(planIds && planIds.length > 0 ? { planIds: planIds.join(',') } : {}),
        ...(apiKey ? { apiKey: apiKey } : {}),
      },
    });
  }

  getById(apiId: string, subscriptionId: string, expands: string[] = []): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}?expands=${expands}`);
  }

  update(apiId: string, subscriptionId: string, updateSubscription: UpdateSubscription): Observable<Subscription> {
    return this.http.put<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}`, updateSubscription);
  }

  transfer(apiId: string, subscriptionId: string, planId: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_transfer`, {
      planId,
    });
  }

  pause(subscriptionId: string, apiId: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_pause`, {});
  }

  resume(subscriptionId: string, apiId: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_resume`, {});
  }
  resumeFailure(subscriptionId: string, apiId: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_resumeFailure`, {});
  }

  create(apiId: string, createSubscription: CreateSubscription): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions`, createSubscription);
  }

  close(subscriptionId: string, apiId: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_close`, {});
  }

  verify(apiId: string, verifySubscription: VerifySubscription): Observable<VerifySubscriptionResponse> {
    return this.http.post(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/_verify`, verifySubscription);
  }

  accept(subscriptionId: string, apiId: string, acceptSubscription: AcceptSubscription): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_accept`,
      acceptSubscription,
    );
  }

  reject(subscriptionId: string, apiId: string, reason: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_reject`, {
      reason,
    });
  }

  /**
   * API KEY
   */
  listApiKeys(apiId: string, subscriptionId: string, page = 1, perPage = 10): Observable<SubscriptionApiKeysResponse> {
    return this.http.get<SubscriptionApiKeysResponse>(
      `${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/api-keys`,
      { params: { page, perPage } },
    );
  }

  renewApiKey(apiId: string, subscriptionId: string, customApiKey: string): Observable<ApiKey> {
    return this.http.post<ApiKey>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/api-keys/_renew`, {
      customApiKey,
    });
  }

  revokeApiKey(apiId: string, subscriptionId: string, apiKeyId: string): Observable<ApiKey> {
    return this.http.post<ApiKey>(
      `${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/api-keys/${apiKeyId}/_revoke`,
      {},
    );
  }

  expireApiKey(apiId: string, subscriptionId: string, apiKeyId: string, expireAt: Date): Observable<ApiKey> {
    return this.http.put<ApiKey>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/api-keys/${apiKeyId}`, {
      expireAt,
    });
  }

  reactivateApiKey(apiId: string, subscriptionId: string, apiKeyId: string): Observable<ApiKey> {
    return this.http.post<ApiKey>(
      `${this.constants.env.v2BaseURL}/apis/${apiId}/subscriptions/${subscriptionId}/api-keys/${apiKeyId}/_reactivate`,
      {},
    );
  }
}
