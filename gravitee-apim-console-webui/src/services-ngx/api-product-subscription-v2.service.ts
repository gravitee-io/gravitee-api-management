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
import { inject, Injectable } from '@angular/core';
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
export class ApiProductSubscriptionV2Service {
  private readonly http = inject(HttpClient);
  private readonly constants = inject<Constants>(Constants);

  list(
    apiProductId: string,
    page = '1',
    perPage = '10',
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apiKey?: string,
    expands?: ('plan' | 'application')[],
  ): Observable<ApiSubscriptionsResponse> {
    return this.http.get<ApiSubscriptionsResponse>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions`, {
      params: {
        page,
        perPage,
        ...(statuses && statuses.length > 0 ? { statuses: statuses.join(',') } : {}),
        ...(applicationIds && applicationIds.length > 0 ? { applicationIds: applicationIds.join(',') } : {}),
        ...(planIds && planIds.length > 0 ? { planIds: planIds.join(',') } : {}),
        ...(apiKey ? { apiKey } : {}),
        ...(expands ? { expands: expands.join(',') } : {}),
      },
    });
  }

  exportAsCSV(
    apiProductId: string,
    page = '1',
    perPage = '10',
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apiKey?: string,
  ): Observable<Blob> {
    return this.http.get(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/_export`, {
      responseType: 'blob',
      params: {
        page,
        perPage,
        ...(statuses && statuses.length > 0 ? { statuses: statuses.join(',') } : {}),
        ...(applicationIds && applicationIds.length > 0 ? { applicationIds: applicationIds.join(',') } : {}),
        ...(planIds && planIds.length > 0 ? { planIds: planIds.join(',') } : {}),
        ...(apiKey ? { apiKey } : {}),
      },
    });
  }

  getById(apiProductId: string, subscriptionId: string, expands: string[] = []): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}`, {
      params: { ...(expands.length > 0 ? { expands: expands.join(',') } : {}) },
    });
  }

  create(apiProductId: string, createSubscription: CreateSubscription): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions`, createSubscription);
  }

  update(apiProductId: string, subscriptionId: string, updateSubscription: UpdateSubscription): Observable<Subscription> {
    return this.http.put<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}`,
      updateSubscription,
    );
  }

  transfer(apiProductId: string, subscriptionId: string, planId: string): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/_transfer`,
      { planId },
    );
  }

  pause(subscriptionId: string, apiProductId: string): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/_pause`,
      {},
    );
  }

  resume(subscriptionId: string, apiProductId: string): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/_resume`,
      {},
    );
  }

  resumeFailure(subscriptionId: string, apiProductId: string): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/_resumeFailure`,
      {},
    );
  }

  close(subscriptionId: string, apiProductId: string): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/_close`,
      {},
    );
  }

  accept(subscriptionId: string, apiProductId: string, acceptSubscription: AcceptSubscription): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/_accept`,
      acceptSubscription,
    );
  }

  reject(subscriptionId: string, apiProductId: string, reason: string): Observable<Subscription> {
    return this.http.post<Subscription>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/_reject`,
      { reason },
    );
  }

  verify(apiProductId: string, verifySubscription: VerifySubscription): Observable<VerifySubscriptionResponse> {
    return this.http.post(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/_verify`, verifySubscription);
  }

  listApiKeys(apiProductId: string, subscriptionId: string, page = 1, perPage = 10): Observable<SubscriptionApiKeysResponse> {
    return this.http.get<SubscriptionApiKeysResponse>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/api-keys`,
      { params: { page, perPage } },
    );
  }

  renewApiKey(apiProductId: string, subscriptionId: string, customApiKey: string): Observable<ApiKey> {
    return this.http.post<ApiKey>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/api-keys/_renew`,
      { customApiKey },
    );
  }

  revokeApiKey(apiProductId: string, subscriptionId: string, apiKeyId: string): Observable<ApiKey> {
    return this.http.post<ApiKey>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/api-keys/${apiKeyId}/_revoke`,
      {},
    );
  }

  expireApiKey(apiProductId: string, subscriptionId: string, apiKeyId: string, expireAt: Date): Observable<ApiKey> {
    return this.http.put<ApiKey>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/api-keys/${apiKeyId}`,
      { expireAt },
    );
  }

  reactivateApiKey(apiProductId: string, subscriptionId: string, apiKeyId: string): Observable<ApiKey> {
    return this.http.post<ApiKey>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/subscriptions/${subscriptionId}/api-keys/${apiKeyId}/_reactivate`,
      {},
    );
  }
}
