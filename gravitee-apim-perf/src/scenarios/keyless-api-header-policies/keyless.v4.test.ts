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
import { LifecycleAction } from '@models/v3/ApiEntity';
import { failIf } from '@helpers/k6.helper';
import { ApisFixture } from '@fixtures/v3/apis.fixture';
import { HttpHelper } from '@helpers/http.helper';
import { GatewayTestData } from '@lib/test-api';
import { GatewayClient } from '@clients/GatewayClient';
import { ApisV4Fixture } from '@fixtures/v4/apis.v4.fixture';
import { ApisV4Client } from '@clients/v4/ApisV4Client';
import { PlansV4Client } from '@clients/v4/PlansV4Client';
import { PlansV4Fixture } from '@fixtures/v4/plans.v4.fixture';
import { NewPlanEntityV4StatusEnum } from '@models/v4/NewPlanEntityV4';
import { ApiEntityV4 } from '@models/v4/ApiEntityV4';
import { PlanEntityV4 } from '@models/v4/PlanEntityV4';

export const options = k6Options;

export function setup(): GatewayTestData {
  const contextPath = ApisFixture.randomPath();
  const api = ApisV4Fixture.newApi({
    listeners: [
      ApisV4Fixture.newHttpListener({
        paths: [
          {
            path: contextPath,
          },
        ],
        entrypoints: [
          {
            type: 'http-proxy',
          },
        ],
      }),
    ],
    endpointGroups: [
      {
        name: 'default-group',
        type: 'http-proxy',
        endpoints: [
          {
            name: 'default',
            type: 'http-proxy',
            inheritConfiguration: false,
            configuration: {
              target: k6Options.apim.apiEndpointUrl,
            },
          },
        ],
      },
    ],
    flows: [
      {
        name: 'flow-1',
        enabled: true,
        request: [
          {
            name: 'Transform header',
            description: 'Add some headers',
            enabled: true,
            policy: 'transform-headers',
            configuration: {
              addHeaders: [
                {
                  name: 'header-added-1',
                  value: 'value-1',
                },
                {
                  name: 'header-added-2',
                  value: 'value-2',
                },
              ],
            },
          },
        ],
        response: [
          {
            name: 'Transform header',
            description: 'Add some headers to response',
            enabled: true,
            policy: 'transform-headers',
            configuration: {
              addHeaders: [
                {
                  name: 'response-header-added-1',
                  value: 'value-1',
                },
              ],
            },
          },
        ],
      },
    ],
  });
  const apiCreationResponse = ApisV4Client.createApi(api, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(apiCreationResponse.status !== 201, 'Could not create API');
  const createdApi = HttpHelper.parseBody<ApiEntityV4>(apiCreationResponse);

  const planCreationResponse = PlansV4Client.createPlan(
    createdApi.id,
    PlansV4Fixture.newPlan({ status: NewPlanEntityV4StatusEnum.PUBLISHED }),
    {
      headers: {
        'Content-Type': 'application/json',
        ...authorizationHeaderFor(ADMIN_USER),
      },
    },
  );
  failIf(planCreationResponse.status !== 201, 'Could not create plan');
  const createdPlan = HttpHelper.parseBody<PlanEntityV4>(planCreationResponse);

  const changeLifecycleResponse = ApisV4Client.changeLifecycle(createdApi.id, LifecycleAction.START, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(changeLifecycleResponse.status !== 204, 'Could not change lifecycle');

  GatewayClient.waitForApiAvailability({ contextPath: contextPath });

  return { api: createdApi, plan: createdPlan, waitGateway: { contextPath: contextPath } };
}

export default (data: GatewayTestData) => {
  const res = http.get(k6Options.apim.gatewayBaseUrl + data.waitGateway.contextPath);
  check(res, {
    'status is 200': () => res.status === 200,
    'contains header': () => res.headers['Content-Type'] === 'application/json',
    'contains header added header': () => res.headers['response-header-added-1'] === 'value-1',
  });
};

export function teardown(data: GatewayTestData) {
  ApisV4Client.changeLifecycle(data.api.id, LifecycleAction.STOP, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  PlansV4Client.deletePlan(data.api.id, data.plan.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  ApisV4Client.deleteApi(data.api.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
}
