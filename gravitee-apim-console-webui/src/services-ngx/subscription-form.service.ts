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

import { Constants } from '../entities/Constants';
import { SubscriptionForm, UpdateSubscriptionForm } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionFormService {
  constructor(
    private http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public getSubscriptionForm(): Observable<SubscriptionForm> {
    return this.http.get<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms`);
  }

  public updateSubscriptionForm(id: string, content: UpdateSubscriptionForm): Observable<SubscriptionForm> {
    return this.http.put<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms/${id}`, content);
  }

  public enableSubscriptionForm(id: string): Observable<SubscriptionForm> {
    return this.http.post<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms/${id}/_enable`, {});
  }

  public disableSubscriptionForm(id: string): Observable<SubscriptionForm> {
    return this.http.post<SubscriptionForm>(`${this.constants.env.v2BaseURL}/subscription-forms/${id}/_disable`, {});
  }
}
