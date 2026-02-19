/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ApiPlansResponse, CreatePlan, Plan, PlanStatus, UpdatePlan } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ApiProductPlanService {
  private readonly http = inject(HttpClient);
  private readonly constants = inject(Constants);

  list(apiProductId: string, statuses?: PlanStatus[], page = 1, perPage = 10): Observable<ApiPlansResponse> {
    return this.http.get<ApiPlansResponse>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/plans`, {
      params: {
        page,
        perPage,
        ...(statuses ? { statuses: statuses.join(',') } : {}),
      },
    });
  }

  public get(apiProductId: string, planId: string): Observable<Plan> {
    return this.http.get<Plan>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}`);
  }

  public create(apiProductId: string, plan: CreatePlan): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/plans`, plan);
  }

  public update(apiProductId: string, planId: string, plan: UpdatePlan): Observable<Plan> {
    return this.http.put<Plan>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}`, plan);
  }

  public publish(apiProductId: string, planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}/_publish`, {});
  }

  public deprecate(apiProductId: string, planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}/_deprecate`, {});
  }

  public close(apiProductId: string, planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}/_close`, {});
  }
}
