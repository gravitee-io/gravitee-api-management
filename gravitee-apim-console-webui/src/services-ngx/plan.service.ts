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
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { castArray } from 'lodash';

import { Constants } from '../entities/Constants';
import { Api } from '../entities/api';
import { NewPlan, Plan, PlanStatus } from '../entities/plan';

@Injectable({
  providedIn: 'root',
})
export class PlanService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  public getApiPlans(apiId: string, status?: PlanStatus | PlanStatus[], security?: string): Observable<Plan[]> {
    let params = new HttpParams();

    if (status) {
      params = params.append('status', castArray(status).join(','));
    }

    if (security) {
      params = params.append('security', security);
    }

    return this.http.get<Plan[]>(`${this.constants.env.baseURL}/apis/${apiId}/plans`, { params });
  }

  // FIXME: after migration, remove broadcast and change api to apiId
  public update(api: Api, plan: Plan): Observable<Plan> {
    return this.http.put<Plan>(`${this.constants.env.baseURL}/apis/${api.id}/plans/${plan.id}`, plan);
  }

  public create(api: Api, plan: NewPlan): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.baseURL}/apis/${api.id}/plans`, plan);
  }

  public get(apiId: string, planId: string): Observable<Plan> {
    return this.http.get<Plan>(`${this.constants.env.baseURL}/apis/${apiId}/plans/${planId}`);
  }

  public publish(api: Api, plan: Plan): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.baseURL}/apis/${api.id}/plans/${plan.id}/_publish`, plan);
  }

  public deprecate(api: Api, plan: Plan): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.baseURL}/apis/${api.id}/plans/${plan.id}/_deprecate`, plan);
  }

  public close(api: Api, plan: Plan): Observable<Plan> {
    return this.http.post<Plan>(`${this.constants.env.baseURL}/apis/${api.id}/plans/${plan.id}/_close`, {});
  }
}
