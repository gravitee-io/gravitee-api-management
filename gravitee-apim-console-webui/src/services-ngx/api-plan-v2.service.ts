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
import { ApiPlansResponse, CreatePlan, Plan, PlanStatus, UpdatePlan } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ApiPlanV2Service {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}
  list(apiId: string, securities?: string[], statuses?: PlanStatus[], page = 1, perPage = 10): Observable<ApiPlansResponse> {
    return this.http.get<ApiPlansResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans`, {
      params: {
        page,
        perPage,
        ...(securities ? { securities: securities.join(',') } : {}),
        ...(statuses ? { statuses: statuses.join(',') } : {}),
      },
    });
  }

  public create(apiId: string, plan: CreatePlan): Observable<Plan> {
    if (plan.security.type === 'PUSH') {
      plan.security.type = 'SUBSCRIPTION';
    }
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans`, plan);
  }

  public get(apiId: string, planId: string): Observable<Plan> {
    return this.http.get<Plan>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans/${planId}`);
  }

  public update(apiId: string, planId: string, plan: UpdatePlan): Observable<Plan> {
    return this.http.put<Plan>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans/${planId}`, plan);
  }

  public publish(apiId: string, planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans/${planId}/_publish`, {});
  }

  public deprecate(apiId: string, planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans/${planId}/_deprecate`, {});
  }

  public close(apiId: string, planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/apis/${apiId}/plans/${planId}/_close`, {});
  }
}
