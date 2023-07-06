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
import http, { RefinedParams } from 'k6/http';
import { k6Options } from '@env/environment';
import { HttpHelper } from '@helpers/http.helper';
import { OUT_OF_SCENARIO } from '@clients/GatewayClient';
import { NewPlanEntityV4 } from '@models/v4/NewPlanEntityV4';

const baseUrl = k6Options.apim.managementBaseUrl;
const plansUrl = `${baseUrl}/v2/environments/${k6Options.apim.environment}/apis/:apiId/plans`;

export class PlansV4Client {
  static createPlan(api: string, plan: NewPlanEntityV4, params: RefinedParams<any>) {
    return http.post(HttpHelper.replacePathParams(plansUrl, [':apiId'], [api]), JSON.stringify(plan), {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static publishPlan(api: string, plan: string, params: RefinedParams<any>) {
    return http.post(
      `${HttpHelper.replacePathParams(plansUrl, [':apiId'], [api])}/${plan}/_publish`,
      {},
      {
        tags: { name: OUT_OF_SCENARIO },
        ...params,
      },
    );
  }

  static deletePlan(api: string, plan: string, params: RefinedParams<any>) {
    return http.del(
      `${HttpHelper.replacePathParams(plansUrl, [':apiId'], [api])}/${plan}`,
      {},
      {
        tags: { name: OUT_OF_SCENARIO },
        ...params,
      },
    );
  }
}
