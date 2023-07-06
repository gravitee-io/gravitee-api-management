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
import { LifecycleAction } from '@models/v3/ApiEntity';
import { HttpHelper } from '@helpers/http.helper';
import { OUT_OF_SCENARIO } from '@clients/GatewayClient';
import { NewApiEntityV4 } from '@models/v4/NewApiEntityV4';
import { NewSubscriptionEntitV4 } from '@lib/models/v4/NewSubscriptionEntityV4';

const baseUrl = k6Options.apim.managementBaseUrl;
const apisUrl = `${baseUrl}/v2/environments/${k6Options.apim.environment}/apis`;

export class ApisV4Client {
  static createApi(api: NewApiEntityV4, params: RefinedParams<any>) {
    return http.post(apisUrl, JSON.stringify(api), {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static changeLifecycle(api: string, lifecycleAction: LifecycleAction, params: RefinedParams<any>) {
    return http.post(
      HttpHelper.replacePathParams(`${apisUrl}/:apiId/_${lifecycleAction.toLowerCase()}`, [':apiId'], [api]),
      {},
      {
        tags: { name: OUT_OF_SCENARIO },
        ...params,
      },
    );
  }

  static createSubscription(api: string, subscription: NewSubscriptionEntitV4, params: RefinedParams<any>) {
    return http.post(apisUrl + `/${api}/subscriptions`, JSON.stringify(subscription), {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static stopSubscription(api: string, subscription: string, params: RefinedParams<any>) {
    return http.post(apisUrl + `/${api}/subscriptions/${subscription}/status?status=CLOSED`, null, {
      tags: { name: OUT_OF_SCENARIO },
      ...params,
    });
  }

  static deleteApi(api: string, params: RefinedParams<any>) {
    return http.del(
      HttpHelper.replacePathParams(`${apisUrl}/:apiId?closePlans=true`, [':apiId'], [api]),
      {},
      {
        tags: { name: OUT_OF_SCENARIO },
        ...params,
      },
    );
  }
}
