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
import { ApiPlansResponse, PlanStatus, CreatePlanV4, Plan } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ApiPlanV2Service {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}
  list(apiId: string, security?: string[], status?: PlanStatus[], page = 1, perPage = 10): Observable<ApiPlansResponse> {
    return this.http.get<ApiPlansResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans`, {
      params: {
        page,
        perPage,
        ...(security ? { security } : {}),
        ...(status ? { status } : {}),
      },
    });
  }

  public create(apiId: string, plan: CreatePlanV4): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans`, plan);
  }

  public publish(apiId: string, planId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans/${planId}/_publish`, {});
  }
}
