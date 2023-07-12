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
import { PlanSecurityTypeV4 } from '@models/v4/NewPlanEntityV4';
import { ApiEntityV4 } from '@models/v4/ApiEntityV4';
import { PlanEntityV4, PlanValidationTypeV4 } from '@models/v4/PlanEntityV4';
import { NewApiEntityV4TypeEnum } from '@models/v4/NewApiEntityV4';
import { ApplicationsV4Client } from '@clients/v4/ApplicationsV4Client';
import { ApplicationEntityV4 } from '@models/v4/ApplicationEntityV4';
import { ApisClient } from '@clients/v3/ApisClient';
import { SubscriptionEntity } from '@models/v3/SubscriptionEntity';
import { ApiKeyEntity } from '@models/v3/ApiKeyEntity';

/**
 * Create an API that assign a JSON body to the request and apply Transform JSON to XML on the response.
 * Used with an APIKEY plan.
 * Expects 200 status and to contains 'Content-Type: application/xml' header
 */
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
            name: 'Assign content',
            description: 'Assign foo bar json',
            enabled: true,
            policy: 'policy-assign-content',
            configuration: {
              scope: 'REQUEST',
              body: '{\n  "foo": "bar"\n}',
            },
          },
        ],
        response: [
          {
            name: 'JSON to XML',
            description: 'Transform JSON to XML',
            enabled: true,
            policy: 'json-xml',
            configuration: {
              rootElement: 'root',
              scope: 'RESPONSE',
            },
          },
        ],
      },
    ],
    type: NewApiEntityV4TypeEnum.PROXY,
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
    PlansV4Fixture.newPlan({
      security: { type: PlanSecurityTypeV4.API_KEY },
      validation: PlanValidationTypeV4.AUTO,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        ...authorizationHeaderFor(ADMIN_USER),
      },
    },
  );
  failIf(planCreationResponse.status !== 201, 'Could not create plan');
  const createdPlan = HttpHelper.parseBody<PlanEntityV4>(planCreationResponse);

  const publishPlanResponse = PlansV4Client.publishPlan(createdApi.id, createdPlan.id, {
    headers: {
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(publishPlanResponse.status !== 200, 'Could not publish plan');

  const appCreationResponse = ApplicationsV4Client.createApplication({
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(appCreationResponse.status !== 201, 'Could not create application');
  const createdApp = HttpHelper.parseBody<ApplicationEntityV4>(appCreationResponse);

  const subscriptionCreationResponse = ApisClient.createSubscriptions(createdApi.id, createdApp.id, createdPlan.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(subscriptionCreationResponse.status !== 201, 'Could not create subscription');
  const createdSubscription = HttpHelper.parseBody<SubscriptionEntity>(subscriptionCreationResponse);

  const apiKeysResponse = ApisClient.getApiKeys(createdApi.id, createdSubscription.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(apiKeysResponse.status !== 200, 'Could not get API Keys');
  const apiKeys = HttpHelper.parseBody<ApiKeyEntity[]>(apiKeysResponse);
  failIf(apiKeys.length < 1, 'Could not retrieve API Keys');

  const changeLifecycleResponse = ApisV4Client.changeLifecycle(createdApi.id, LifecycleAction.START, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(changeLifecycleResponse.status !== 204, 'Could not change lifecycle');

  GatewayClient.waitForApiAvailability({ contextPath: contextPath + `?api-key=${apiKeys[0]?.key}` });

  return {
    api: createdApi,
    plan: createdPlan,
    waitGateway: { contextPath: contextPath },
    subscription: createdSubscription,
    keys: apiKeys,
  };
}

export default (data: GatewayTestData) => {
  const res = http.get(k6Options.apim.gatewayBaseUrl + data.waitGateway.contextPath, {
    headers: {
      'X-Gravitee-Api-Key': data.keys[0].key,
    },
  });
  check(res, {
    'status is 200': () => res.status === 200,
    'contains header': () => res.headers['Content-Type'] === 'application/xml;charset=UTF-8',
  });
};

export function teardown(data: GatewayTestData) {
  ApisClient.stopSubscription(data.api.id, data.subscription.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  ApisClient.deleteApiKey(data.api.id, data.subscription.id, data.keys[0].id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  ApisV4Client.changeLifecycle(data.api.id, LifecycleAction.STOP, {
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
