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
import { check } from 'k6';
import http from 'k6/http';
import { ADMIN_USER, authorizationHeaderFor, k6Options } from '@env/environment';
import { ApiEntity, LifecycleAction } from '@models/v3/ApiEntity';
import { ApisClient } from '@clients/v3/ApisClient';
import { failIf } from '@helpers/k6.helper';
import { PlansClient } from '@clients/v3/PlansClient';
import { PlansFixture } from '@fixtures/v3/plans.fixture';
import { PlanEntity, PlanStatus } from '@models/v3/PlanEntity';
import { ApisFixture } from '@fixtures/v3/apis.fixture';
import { HttpHelper } from '@helpers/http.helper';
import { GatewayTestData } from '@lib/test-api';
import { GatewayClient } from '@clients/GatewayClient';
import { PathOperatorOperatorEnum } from '@models/v3/Flow';

export const options = k6Options;

export function setup(): GatewayTestData {
  const api = ApisFixture.newApi({
    flows: [
      {
        name: '',
        path_operator: {
          path: '/',
          operator: PathOperatorOperatorEnum.STARTS_WITH,
        },
        condition: '',
        consumers: [],
        methods: [],
        pre: [
          {
            name: 'Assign content',
            description: '',
            enabled: true,
            policy: 'policy-assign-content',
            configuration: {
              scope: 'REQUEST',
              body: '{\n  "foo": "bar"\n}',
            },
          },
        ],
        post: [
          {
            name: 'JSON to XML',
            description: '',
            enabled: true,
            policy: 'json-xml',
            configuration: {
              rootElement: 'root',
              scope: 'RESPONSE',
            },
          },
        ],
        enabled: true,
      },
    ],
  });
  const apiCreationResponse = ApisClient.createApi(api, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(apiCreationResponse.status !== 201, 'Could not create API');
  const createdApi = HttpHelper.parseBody<ApiEntity>(apiCreationResponse);

  const planCreationResponse = PlansClient.createPlan(createdApi.id, PlansFixture.newPlan({ status: PlanStatus.PUBLISHED }), {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(planCreationResponse.status !== 201, 'Could not create plan');
  const createdPlan = HttpHelper.parseBody<PlanEntity>(planCreationResponse);

  const changeLifecycleResponse = ApisClient.changeLifecycle(createdApi.id, LifecycleAction.START, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(changeLifecycleResponse.status !== 204, 'Could not change lifecycle');

  GatewayClient.waitForApiAvailability({ contextPath: api.contextPath });

  return { api: createdApi, plan: createdPlan, waitGateway: { contextPath: api.contextPath } };
}

export default (data: GatewayTestData) => {
  const res = http.get(k6Options.apim.gatewayBaseUrl + data.waitGateway.contextPath);
  check(res, {
    'status is 200': () => res.status === 200,
    'contains header': () => res.headers['Content-Type'] === 'application/xml;charset=UTF-8',
  });
};

export function teardown(data: GatewayTestData) {
  ApisClient.changeLifecycle(data.api.id, LifecycleAction.STOP, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  PlansClient.deletePlan(data.api.id, data.plan.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  ApisClient.deleteApi(data.api.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
}
