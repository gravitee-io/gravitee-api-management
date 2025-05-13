/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { CreateSubscription, Subscription, SubscriptionStatusEnum, UpdateSubscription } from '../entities/subscription/subscription';
import { SubscriptionsResponse } from '../entities/subscription/subscriptions-response';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionService {
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  list(queryParams: {
    apiId?: string;
    applicationId?: string;
    statuses: SubscriptionStatusEnum[] | null;
    size?: number;
  }): Observable<SubscriptionsResponse> {
    const params = {
      ...(queryParams.apiId ? { apiId: queryParams.apiId } : {}),
      ...(queryParams.applicationId ? { applicationId: queryParams.applicationId } : {}),
      ...(queryParams.statuses ? { statuses: queryParams.statuses } : { statuses: [] }),
      ...(queryParams.size ? { size: queryParams.size } : {}),
    };
    return this.http.get<SubscriptionsResponse>(`${this.configService.baseURL}/subscriptions`, {
      params,
    });
  }

  get(subscriptionId: string): Observable<Subscription> {
    return this.http.get<Subscription>(
      `${this.configService.baseURL}/subscriptions/${subscriptionId}?include=keys&include=consumerConfiguration`,
    );
  }

  subscribe(createSubscription: CreateSubscription): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.configService.baseURL}/subscriptions`, createSubscription);
  }

  update(subscriptionId: string, updatedSubscription: UpdateSubscription) {
    return this.http.put<Subscription>(`${this.configService.baseURL}/subscriptions/${subscriptionId}`, updatedSubscription);
  }

  resumeConsumerStatus(subscriptionId: string): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.configService.baseURL}/subscriptions/${subscriptionId}/_resumeFailure`, null);
  }
}
