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
import { Promotion, PromotionRequest, PromotionTarget } from '../entities/promotion';

export class PromotionService {
  constructor(private $http: IHttpService, private Constants: any, private $q: IQService) {
    'ngInject';
  }

  listPromotionTargets(): IPromise<PromotionTarget[]> {
    return this.$http.get<PromotionTarget[]>(`${this.Constants.env.baseURL}/promotion-targets`).then((response) => response.data);
  }

  promote(apiId: string, promotionTarget: PromotionTarget): IPromise<Promotion> {
    const promotionRequest: PromotionRequest = {
      targetEnvironmentId: promotionTarget.id,
      targetInstallationId: promotionTarget.installationId,
    };
    return this.$http
      .post<Promotion>(`${this.Constants.env.baseURL}/apis/${apiId}/_promote`, promotionRequest)
      .then((response) => response.data);
  }
}
