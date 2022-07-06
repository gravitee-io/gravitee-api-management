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
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanEntity } from '@management-models/PlanEntity';
import { noContent, succeed } from '@lib/jest-utils';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { fetchGatewaySuccess } from '@lib/gateway';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { PlanStatus } from '@management-models/PlanStatus';
import { UpdatePlanEntityFromJSON } from '@management-models/UpdatePlanEntity';

const apiManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());
const orgId = 'DEFAULT';
const envId = 'DEFAULT';
let createdApi: ApiEntity;
let createdPlan: PlanEntity;

describe('Redeploy Api', () => {
  describe('On api deployed with latency policy', () => {
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
                  name: 'Latency',
                  description: 'This latency policy was created by a test',
                  enabled: true,
                  policy: 'latency',
                  configuration: {
                    time: 8,
                    timeUnit: 'SECONDS',
                  },
                  condition: "{#request.params['activeLatency'] != null && #request.params['activeLatency'][0] == 'true'}",
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
      await succeed(apiManagementApiAsApiUser.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    afterAll(async () => {
      await apiManagementApiAsApiUser.doApiLifecycleAction({ orgId, envId, api: createdApi.id, action: LifecycleAction.STOP });
      await apiManagementApiAsApiUser.deleteApiPlan({ orgId, envId, api: createdApi.id, plan: createdPlan.id });
      await apiManagementApiAsApiUser.deleteApi({ orgId, envId, api: createdApi.id });
    });

    test('should redeploy with active request', async () => {
      // Fist fetch Gateway to be sure api is deployed
      await fetchGatewaySuccess({ contextPath: createdApi.context_path + '?activeLatency=false' });

      // Update a Plan to desynchronize Api definition
      await apiManagementApiAsApiUser.updateApiPlan({
        envId,
        orgId,
        api: createdApi.id,
        plan: createdPlan.id,
        updatePlanEntity: UpdatePlanEntityFromJSON({
          ...createdPlan,
          publishedAt: null,
          needRedeployAt: null,
          selection_rule: 'true',
        }),
      });

      // Execute a request during which we will redeploy the api (only one try, no delay before call)
      const fetchUniqueGatewayCall = fetchGatewaySuccess({
        contextPath: createdApi.context_path + '?activeLatency=true',
        maxRetries: 1,
      });

      // Redeploy the api
      await succeed(apiManagementApiAsApiUser.deployApiRaw({ orgId, envId, api: createdApi.id }));

      // Expect request success
      await fetchUniqueGatewayCall
        .then((res) => res.json())
        .then((json) => {
          expect(json.message).toBe('Hello, World!');
        });
    });
  });
});
