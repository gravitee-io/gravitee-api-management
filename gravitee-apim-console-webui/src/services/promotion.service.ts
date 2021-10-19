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
import { IHttpService, IPromise, IQService } from 'angular';

import { Constants } from '../entities/Constants';
import { Promotion, PromotionRequest, PromotionTarget } from '../entities/promotion';
import { PromotionSearchParams } from '../entities/promotion/promotionSearchParams';

export class PromotionService {
  constructor(private $http: IHttpService, private Constants: Constants, private $q: IQService) {
    'ngInject';
  }

  listPromotionTargets(): IPromise<PromotionTarget[]> {
    return this.$http.get<PromotionTarget[]>(`${this.Constants.env.baseURL}/promotion-targets`).then((response) => response.data);
  }

  promote(apiId: string, promotionTarget: { id: string; name: string }): IPromise<Promotion> {
    const promotionRequest: PromotionRequest = {
      targetEnvCockpitId: promotionTarget.id,
      targetEnvName: promotionTarget.name,
    };
    return this.$http
      .post<Promotion>(`${this.Constants.env.baseURL}/apis/${apiId}/_promote`, promotionRequest)
      .then((response) => response.data);
  }

  processPromotion(promotionId: string, isAccepted: boolean) {
    return this.$http
      .post<Promotion>(`${this.Constants.org.baseURL}/promotions/${promotionId}/_process`, isAccepted)
      .then((response) => response.data);
  }

  listPromotion(searchParams: PromotionSearchParams) {
    return this.$http
      .post<Array<Promotion>>(`${this.Constants.org.baseURL}/promotions/_search`, null, {
        params: searchParams,
      })
      .then((response) => response.data);
  }
}
