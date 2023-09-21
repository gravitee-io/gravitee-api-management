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
import { ApiEntity, ApiEntityFlowModeEnum } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { ApiImportEntityToJSON, ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PlanSecurityType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanSecurityType';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { forManagementAsApiUser } from '@gravitee/utils/configuration';
import { teardownApisAndApplications } from '@gravitee/utils/management';
import { PathOperatorOperatorEnum } from '@gravitee/management-webclient-sdk/src/lib/models/PathOperator';
import { fetchGatewaySuccess } from '@gravitee/utils/apim-http';
import { Flow } from '@gravitee/management-webclient-sdk/src/lib/models/Flow';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());

describe('Expected flow is getting selected', () => {
  let mockPolicyFlow: Flow;
  let assignAttributePolicyFlow: Flow;

  beforeAll(async () => {
    mockPolicyFlow = {
      name: '',
      path_operator: {
        path: '/foo',
        operator: PathOperatorOperatorEnum.STARTS_WITH,
      },
      condition: '',
      consumers: [],
      methods: [],
      pre: [
        {
          name: 'Mock',
          description: '',
          enabled: true,
          policy: 'mock',
          configuration: {
            content: `{"requested_path":"{#request.path}","from_path":"{#context.attributes["from_path"]}"}`,
            status: '200',
          },
        },
      ],
      post: [],
      enabled: true,
    };

    assignAttributePolicyFlow = {
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
          name: 'Assign attributes',
          description: '',
          enabled: true,
          policy: 'policy-assign-attributes',
          configuration: {
            scope: 'REQUEST',
            attributes: [
              {
                name: 'from_path',
                value: 'root level',
              },
            ],
          },
        },
      ],
      post: [],
      enabled: true,
    };
  });

  describe('Flow mode: default', () => {
    let createdApi: ApiEntity;

    beforeAll(async () => {
      // create APIs with a published keyless plan with a mock and assign-attribute policy
      createdApi = await apisManagementApiAsApiUser.importApiDefinition({
        envId,
        orgId,
        body: ApiImportEntityToJSON(
          ApisFaker.apiImport({
            flow_mode: ApiEntityFlowModeEnum.DEFAULT,
            plans: [
              PlansFaker.plan({
                security: PlanSecurityType.KEY_LESS,
                status: PlanStatus.PUBLISHED,
                flows: [mockPolicyFlow, assignAttributePolicyFlow],
              }),
            ],
          }),
        ),
      });

      // Start it
      await apisManagementApiAsApiUser.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.START,
      });
    });

    test('should execute all matching flows (response containing both, mock and attribute)', async () => {
      await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}/foo`,
        expectedResponseValidator: async (response) => {
          const body = await response.json();
          expect(body).toEqual({
            requested_path: `${createdApi.context_path}/foo`,
            from_path: 'root level',
          });
          return true;
        },
      });
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi.id]);
    });
  });

  describe('Flow mode: Best match', () => {
    let createdApi: ApiEntity;

    beforeAll(async () => {
      // create APIs with a published keyless plan with a mock and assign-attribute policy
      createdApi = await apisManagementApiAsApiUser.importApiDefinition({
        envId,
        orgId,
        body: ApiImportEntityToJSON(
          ApisFaker.apiImport({
            flow_mode: ApiEntityFlowModeEnum.BEST_MATCH,
            plans: [
              PlansFaker.plan({
                security: PlanSecurityType.KEY_LESS,
                status: PlanStatus.PUBLISHED,
                flows: [mockPolicyFlow, assignAttributePolicyFlow],
              }),
            ],
          }),
        ),
      });

      // Start it
      await apisManagementApiAsApiUser.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.START,
      });
    });

    test('should only execute best matching flow (no attribute data in response)', async () => {
      await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}/foo`,
        expectedResponseValidator: async (response) => {
          const body = await response.json();
          expect(body.requested_path).toEqual(`${createdApi.context_path}/foo`);
          expect(body.from_path).not.toContain('root level');
          return true;
        },
      });
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi.id]);
    });
  });
});
