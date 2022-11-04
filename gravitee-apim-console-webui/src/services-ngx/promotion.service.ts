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
import { Promotion, PromotionRequest, PromotionTarget } from '../entities/promotion';
import { PromotionSearchParams } from '../entities/promotion/promotionSearchParams';

@Injectable({
  providedIn: 'root',
})
export class PromotionService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  listPromotionTargets(): Observable<PromotionTarget[]> {
    return this.http.get<PromotionTarget[]>(`${this.constants.env.baseURL}/promotion-targets`);
  }

  promote(apiId: string, promotionTarget: { id: string; name: string }): Observable<Promotion> {
    const promotionRequest: PromotionRequest = {
      targetEnvCockpitId: promotionTarget.id,
      targetEnvName: promotionTarget.name,
    };
    return this.http.post<Promotion>(`${this.constants.env.baseURL}/apis/${apiId}/_promote`, promotionRequest);
  }

  processPromotion(promotionId: string, isAccepted: boolean): Observable<Promotion> {
    return this.http.post<Promotion>(`${this.constants.org.baseURL}/promotions/${promotionId}/_process`, isAccepted);
  }

  listPromotion(searchParams: PromotionSearchParams): Observable<Promotion[]> {
    const params = {
      ...(searchParams.apiId ? { apiId: searchParams.apiId } : {}),
      ...(searchParams.statuses ? { statuses: searchParams.statuses } : {}),
    };

    return this.http.post<Array<Promotion>>(`${this.constants.org.baseURL}/promotions/_search`, null, {
      params,
    });
  }
}
