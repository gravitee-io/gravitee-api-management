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
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiEntity } from '@management-models/ApiEntity';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanEntity } from '@management-models/PlanEntity';
import { RuleMethodsEnum } from '@management-models/Rule';
import { noContent, succeed } from '@lib/jest-utils';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { fetchGatewaySuccess } from '@lib/gateway';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { PlanStatus } from '@management-models/PlanStatus';

const apiManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());
const orgId = 'DEFAULT';
const envId = 'DEFAULT';
let createdApi: ApiEntity;
let createdPlan: PlanEntity;

describe('Mock policy', () => {
  describe('On api v1 - based on paths', () => {
    beforeAll(async () => {
      const newPlanEntity = PlansFaker.newPlan({ status: PlanStatus.PUBLISHED });

      createdApi = await apiManagementApiAsApiUser.createApi({ orgId, envId, newApiEntity: ApisFaker.newApi() });

      createdPlan = await apiManagementApiAsApiUser.createApiPlan({ orgId, envId, api: createdApi.id, newPlanEntity });

      await noContent(
        apiManagementApiAsApiUser.doApiLifecycleActionRaw({ orgId, envId, api: createdApi.id, action: LifecycleAction.START }),
      );
    });

    afterAll(async () => {
      await apiManagementApiAsApiUser.doApiLifecycleActionRaw({ orgId, envId, api: createdApi.id, action: LifecycleAction.STOP });
      await apiManagementApiAsApiUser.deleteApiPlan({ orgId, envId, api: createdApi.id, plan: createdPlan.id });
      await apiManagementApiAsApiUser.deleteApi({ orgId, envId, api: createdApi.id });
    });

    test('should create, deploy and call a mock policy', async () => {
      let mockContent = JSON.stringify({
        message: 'This is a mocked response',
      });
      const updateApiEntity = UpdateApiEntityFromJSON({
        ...createdApi,
        paths: {
          '/': [
            {
              methods: [RuleMethodsEnum.GET],
              description: 'Description of the Mock Gravitee Policy',
              enabled: true,
              mock: {
                headers: [
                  {
                    name: 'test-value',
                    value: 'value123',
                  },
                ],
                status: '200',
                content: mockContent,
              },
            },
          ],
        },
        path_mappings: ['/'],
      });

      createdApi = await succeed(
        apiManagementApiAsApiUser.updateApiRaw({
          api: createdApi.id,
          updateApiEntity,
          orgId,
          envId,
        }),
      );
      await succeed(apiManagementApiAsApiUser.deployApiRaw({ orgId, envId, api: createdApi.id }));

      await fetchGatewaySuccess({
        contextPath: createdApi.context_path,
        expectedResponseValidator: async (response) => {
          const body = await response.json();
          return body.message === JSON.parse(mockContent).message;
        },
      });
    });
  });

  describe('On api v2 - based on flows', () => {
    let mockContent = JSON.stringify({
      message: 'This is a mocked response from flow',
    });

    beforeAll(async () => {
      const newPlanEntity = PlansFaker.newPlan({ status: PlanStatus.PUBLISHED });

      createdApi = await apiManagementApiAsApiUser.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi({
          gravitee: '2.0.0',
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
              pre: [
                {
                  name: 'Mock',
                  policy: 'mock',
                  description: 'This mock policy was created by a test',
                  enabled: true,
                  configuration: {
                    status: '200',
                    content: mockContent,
                  },
                },
              ],
              post: [],
              enabled: true,
            },
          ],
        }),
      });

      createdPlan = await apiManagementApiAsApiUser.createApiPlan({ orgId, envId, api: createdApi.id, newPlanEntity });

      await noContent(
        apiManagementApiAsApiUser.doApiLifecycleActionRaw({ orgId, envId, api: createdApi.id, action: LifecycleAction.START }),
      );
    });

    afterAll(async () => {
      await apiManagementApiAsApiUser.doApiLifecycleAction({ orgId, envId, api: createdApi.id, action: LifecycleAction.STOP });
      await apiManagementApiAsApiUser.deleteApiPlan({ orgId, envId, api: createdApi.id, plan: createdPlan.id });
      await apiManagementApiAsApiUser.deleteApi({ orgId, envId, api: createdApi.id });
    });

    test('should create, deploy and call a mock policy', async () => {
      await succeed(apiManagementApiAsApiUser.deployApiRaw({ orgId, envId, api: createdApi.id }));

      await fetchGatewaySuccess({ contextPath: createdApi.context_path })
        .then((res) => res.json())
        .then((json) => {
          expect(json).toEqual(JSON.parse(mockContent));
        });
    });
  });
});
