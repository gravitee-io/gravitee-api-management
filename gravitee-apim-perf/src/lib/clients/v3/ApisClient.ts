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
import { NewApiEntity } from '@models/v3/NewApiEntity';
import { LifecycleAction } from '@models/v3/ApiEntity';
import { HttpHelper } from '@helpers/http.helper';
import { OUT_OF_SCENARIO } from '@clients/GatewayClient';

const baseUrl = k6Options.apim.managementBaseUrl;
const apisUrl = `${baseUrl}/organizations/${k6Options.apim.organization}/environments/${k6Options.apim.environment}/apis`;

export class ApisClient {
  static createApi(api: NewApiEntity, params: RefinedParams<any>) {
    return http.post(apisUrl, JSON.stringify(api), {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static changeLifecycle(api: string, lifecycleAction: LifecycleAction, params: RefinedParams<any>) {
    return http.post(
      HttpHelper.replacePathParams(`${apisUrl}/:apiId?action=${lifecycleAction}`, [':apiId'], [api]),
      {},
      {
        tags: { name: OUT_OF_SCENARIO },
        ...params,
      },
    );
  }

  static deleteApi(api: string, params: RefinedParams<any>) {
    return http.del(
      HttpHelper.replacePathParams(`${apisUrl}/:apiId`, [':apiId'], [api]),
      {},
      {
        tags: { name: OUT_OF_SCENARIO },
        ...params,
      },
    );
  }

  static createSubscriptions(api: string, app: string, plan: string, params: RefinedParams<any>) {
    return http.post(`${apisUrl}/${api}/subscriptions?application=${app}&plan=${plan}`, null, {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static stopSubscription(api: string, subscription: string, params: RefinedParams<any>) {
    return http.post(`${apisUrl}/${api}/subscriptions/${subscription}/status?status=CLOSED`, null, {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static getApiKeys(api: string, subscription: string, params: RefinedParams<any>) {
    return http.get(`${apisUrl}/${api}/subscriptions/${subscription}/apikeys`, {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static deleteApiKey(api: string, subscription: string, apikey: string, params: RefinedParams<any>) {
    return http.del(`${apisUrl}/${api}/subscriptions/${subscription}/apikeys/${apikey}`, null, {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }
}
