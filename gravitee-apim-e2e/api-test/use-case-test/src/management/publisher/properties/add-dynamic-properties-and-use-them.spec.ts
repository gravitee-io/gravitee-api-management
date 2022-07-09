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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApiEntity, ApiEntityToJSON } from '@management-models/ApiEntity';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { teardownApisAndApplications } from '@lib/management';
import { DynamicPropertyServiceProviderEnum } from '@management-models/DynamicPropertyService';
import { fetchGatewaySuccess } from '@lib/gateway';
import { setWiremockState } from '@lib/wiremock';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());

const timeBetweenRetries = 3000;

describe.skip('Add dynamic properties and use them', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ security: PlanSecurityType.KEYLESS, status: PlanStatus.PUBLISHED })],
      }),
    });

    await apisResource.updateApi({
      orgId,
      envId,
      api: createdApi.id,
      updateApiEntity: {
        ...UpdateApiEntityFromJSON(ApiEntityToJSON(createdApi)),
        services: {
          dynamic_property: {
            enabled: true,
            schedule: '*/1 * * * * *',
            provider: DynamicPropertyServiceProviderEnum.HTTP,
            configuration: {
              url: `${process.env.WIREMOCK_BASE_URL}/properties`,
              specification: `
                [
                  {
                    "operation": "default",
                    "spec": {}
                  }
                ]
              `,
              useSystemProxy: false,
              method: 'GET',
            },
          },
        },
        flows: [
          {
            name: '',
            path_operator: {
              path: '/',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [],
            pre: [],
            post: [
              {
                name: 'Transform Headers',
                description: '',
                enabled: true,
                policy: 'transform-headers',
                configuration: {
                  addHeaders: [
                    {
                      name: 'x-version',
                      value: '{#properties.version}',
                    },
                  ],
                  scope: 'RESPONSE',
                },
              },
            ],
            enabled: true,
          },
        ],
      },
    });

    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Before property update', () => {
    test('should assign property to header', async () => {
      await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}`,
        expectedResponseValidator: (response) => {
          return response.headers.get('x-version') === '1.0.0';
        },
        timeBetweenRetries,
      });
    });
  });

  describe('After property update', () => {
    beforeAll(async () => {
      await setWiremockState('properties', 'Updated');
    });

    test('should assign property to header', async () => {
      await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}`,
        expectedResponseValidator: (response) => {
          return response.headers.get('x-version') === '1.0.1';
        },
        timeBetweenRetries,
      });
    });

    afterAll(async () => {
      await setWiremockState('properties', 'Started');
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
