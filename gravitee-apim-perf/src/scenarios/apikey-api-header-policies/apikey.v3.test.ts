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
import { PlanEntity, PlanSecurityType, PlanStatus, PlanValidationType } from '@models/v3/PlanEntity';
import { ApisFixture } from '@fixtures/v3/apis.fixture';
import { HttpHelper } from '@helpers/http.helper';
import { GatewayTestData } from '@lib/test-api';
import { GatewayClient } from '@clients/GatewayClient';
import { PathOperatorOperatorEnum } from '@models/v3/Flow';
import { ApplicationsV4Client } from '@clients/v4/ApplicationsV4Client';
import { ApplicationEntityV4 } from '@models/v4/ApplicationEntityV4';
import { SubscriptionEntity } from '@models/v3/SubscriptionEntity';
import { ApiKeyEntity } from '@models/v3/ApiKeyEntity';

/**
 * Create an API that add two headers on the request and remove one on the response
 * Used with an APIKEY plan.
 * Expects 200 status and to contain the right header
 */
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
            name: 'Transform Headers',
            description: '',
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
              scope: 'REQUEST',
            },
          },
        ],
        post: [
          {
            name: 'Transform Headers',
            description: '',
            enabled: true,
            policy: 'transform-headers',
            configuration: {
              scope: 'RESPONSE',
              removeHeaders: ['header-added-1'],
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

  const planCreationResponse = PlansClient.createPlan(
    createdApi.id,
    PlansFixture.newPlan({ status: PlanStatus.PUBLISHED, security: PlanSecurityType.API_KEY, validation: PlanValidationType.AUTO }),
    {
      headers: {
        'Content-Type': 'application/json',
        ...authorizationHeaderFor(ADMIN_USER),
      },
    },
  );
  failIf(planCreationResponse.status !== 201, 'Could not create plan');
  const createdPlan = HttpHelper.parseBody<PlanEntity>(planCreationResponse);

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
  failIf(apiKeysResponse.status !== 200, 'Could not get api keys');
  const apiKeys = HttpHelper.parseBody<ApiKeyEntity[]>(apiKeysResponse);
  failIf(apiKeys.length < 1, 'Could not retrieve api keys');

  const changeLifecycleResponse = ApisClient.changeLifecycle(createdApi.id, LifecycleAction.START, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(changeLifecycleResponse.status !== 204, 'Could not change lifecycle');

  GatewayClient.waitForApiAvailability({ contextPath: api.contextPath + `?api-key=${apiKeys[0]?.key}` });

  return {
    api: createdApi,
    plan: createdPlan,
    waitGateway: { contextPath: api.contextPath },
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
    'contains header': () => res.json('headers.header-added-1') === 'value-1',
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
