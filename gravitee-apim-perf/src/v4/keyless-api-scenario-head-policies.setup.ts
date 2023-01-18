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
import { GatewayTestData, loadTestDataForSetup } from '../../lib/test-api';
import { forManagementAsApiUser } from '@gravitee/utils/configuration';
import { V4APIsApi } from 'gravitee-apim-e2e/lib/management-webclient-sdk/src/lib/apis/V4APIsApi';
import {
  ApiEntityV4,
  HttpListenerV4,
  LifecycleAction,
  PlanEntityV4,
  PlanSecurityTypeV4,
  PlanStatus,
} from 'gravitee-apim-e2e/lib/management-webclient-sdk/src/lib/models';
import { ApisV4Faker } from '@gravitee/fixtures/management/ApisV4Faker';
import faker from '@faker-js/faker';
import { PlansV4Faker } from '@gravitee/fixtures/management/PlansV4Faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apiManagementV4ApiAsApiUser = new V4APIsApi(forManagementAsApiUser());

export async function init(): Promise<GatewayTestData> {
  const api: ApiEntityV4 = await apiManagementV4ApiAsApiUser.createApi1({
    orgId: orgId,
    envId: envId,
    newApiEntityV4: ApisV4Faker.newApi({
      listeners: [
        ApisV4Faker.newHttpListener({
          paths: [
            {
              path: `/${faker.random.word()}-${faker.datatype.uuid()}-${Math.floor(Date.now() / 1000)}`,
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
                target: process.env.API_ENDPOINT_URL,
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
    }),
  });

  if (api && api.id) {
    // Create plan
    const plan: PlanEntityV4 = await apiManagementV4ApiAsApiUser.createApiPlan1({
      envId,
      orgId,
      api: api.id,
      newPlanEntityV4: PlansV4Faker.newPlan({
        status: PlanStatus.PUBLISHED,
        security: { type: PlanSecurityTypeV4.KEY_LESS },
      }),
    });

    await apiManagementV4ApiAsApiUser.doApiLifecycleAction1({
      orgId,
      envId,
      action: LifecycleAction.START,
      api: api.id,
    });

    return { api: api, plan: plan, waitGateway: { contextPath: (api.listeners[0] as HttpListenerV4).paths[0].path } };
  }

  throw new Error('Cannot create api');
}

export async function tearDown() {
  const data: GatewayTestData = await loadTestDataForSetup();
  const { api, plan } = data;
  await apiManagementV4ApiAsApiUser.doApiLifecycleAction1({ orgId, envId, api: api.id, action: LifecycleAction.STOP });
  await apiManagementV4ApiAsApiUser.deleteApiPlan1({ orgId, envId, api: api.id, plan: plan.id });
  await apiManagementV4ApiAsApiUser.deleteApi1({ orgId, envId, api: api.id });
  return `'API[${api.id}] deleted !`;
}
