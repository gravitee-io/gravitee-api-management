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

const baseUrl = k6Options.apim.managementBaseUrl;
const apisUrl = `${baseUrl}/organizations/${k6Options.apim.organization}/environments/${k6Options.apim.environment}/v4/apis`;

export class ApisV4Client {
  static createApi(api: NewApiEntityV4, params: RefinedParams<any>) {
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
}
