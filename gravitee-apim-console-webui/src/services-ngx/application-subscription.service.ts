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
import { inject, Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { ApplicationService } from './application.service';

import { Constants } from '../entities/Constants';
import { ApplicationSubscriptionApiKey } from '../entities/subscription/ApplicationSubscriptionApiKey';
import { Subscription } from '../entities/subscription/subscription';
import { NewSubscriptionEntity } from '../entities/application';

@Injectable({
  providedIn: 'root',
})
export class ApplicationSubscriptionService {
  private readonly applicationService = inject(ApplicationService);

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getSubscription(applicationId: string, subscriptionId: string): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.constants.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}`);
  }

  subscribe(applicationId: string, planId: string, subscription: NewSubscriptionEntity): Observable<Subscription> {
    return this.http
      .post<Subscription>(`${this.constants.env.baseURL}/applications/${applicationId}/subscriptions`, subscription, {
        params: {
          plan: planId,
        },
      })
      .pipe(tap(() => this.applicationService.refreshLastApplicationFetch()));
  }

  closeSubscription(applicationId: string, subscriptionId: string): Observable<void> {
    return this.http
      .delete<void>(`${this.constants.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}`)
      .pipe(tap(() => this.applicationService.refreshLastApplicationFetch()));
  }

  getApiKeys(applicationId: string, subscriptionId: string): Observable<ApplicationSubscriptionApiKey[]> {
    return this.http.get<ApplicationSubscriptionApiKey[]>(
      `${this.constants.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}/apikeys`,
    );
  }

  renewApiKey(applicationId: string, subscriptionId: string): Observable<void> {
    return this.http.post<void>(
      `${this.constants.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}/apikeys/_renew`,
      {},
    );
  }

  revokeApiKey(applicationId: string, subscriptionId: string, apiKeyId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.constants.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}/apikeys/${apiKeyId}`,
    );
  }

  update(applicationId: string, subscriptionId: string, subscriptionToUpdate: Subscription) {
    return this.http.put<Subscription>(
      `${this.constants.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}`,
      subscriptionToUpdate,
    );
  }
}
