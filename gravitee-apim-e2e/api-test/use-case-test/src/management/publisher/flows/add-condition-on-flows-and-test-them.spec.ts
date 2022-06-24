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
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiEntity } from '@management-models/ApiEntity';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanStatus } from '@management-models/PlanStatus';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { fetchGatewaySuccess } from '@lib/gateway';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { PlanEntity } from '@management-models/PlanEntity';
import { OrganizationEntityToJSON } from '@management-models/OrganizationEntity';
import { OrganizationApi } from '@management-apis/OrganizationApi';
import { NewApiEntityFlowModeEnum } from '@management-models/NewApiEntity';
import { teardownApisAndApplications } from '@lib/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());
const organizationApi = new OrganizationApi(forManagementAsAdminUser());

describe('Add condition on flows and test them', () => {
  let createdApi: ApiEntity;
  let createdKeylessPlan: PlanEntity;

  beforeAll(async () => {
    // Create Global Flow
    const organization = await organizationApi.get({ orgId });
    await organizationApi.update({
      orgId,
      updateOrganizationEntity: {
        ...OrganizationEntityToJSON(organization),
        flows: [
          fakeTestingFlow({
            name: 'Global',
            path: '/client',
          }),
          fakeTestingFlow({
            name: 'Global-Conditioned',
            path: '/client',
            condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
          }),
        ],
      },
    });

    // Create new API
    createdApi = await apisResource.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi({
        gravitee: '2.0.0',
        // With flow on root path
        flows: [
          fakeTestingFlow({
            name: 'API-Root',
            path: '/client/:clientName',
          }),
          fakeTestingFlow({
            name: 'API-Root-Conditioned',
            path: '/client/:clientName',
            condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
          }),
        ],
      }),
    });

    // Create first KeylessPlan
    createdKeylessPlan = await apisResource.createApiPlan({
      orgId,
      envId,
      api: createdApi.id,
      newPlanEntity: PlansFaker.newPlan({
        security: PlanSecurityType.KEYLESS,
        status: PlanStatus.PUBLISHED,
        flows: [
          fakeTestingFlow({
            name: 'API-Plan',
            path: '/client/:clientName/keyless',
          }),
          fakeTestingFlow({
            name: 'API-Plan-Conditioned',
            path: '/client/:clientName/keyless',
            condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
          }),
        ],
      }),
    });

    // Start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });

    // Wait for the effective deployment
    await fetchGatewaySuccess({ contextPath: createdApi.context_path });
  });

  describe('when condition is true', () => {
    test('Should return 200 OK on `GET /client/foo/keyless`', async () => {
      const res = await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}/client/foo/keyless`,
        headers: { 'Use-Conditioned-Flow': 'true' },
      });

      expect(res.headers.get('X-Test-Global-Flow')).toEqual('ok');
      expect(res.headers.get('X-Test-API-Root-Flow')).toEqual('ok');
      expect(res.headers.get('X-Test-API-Plan-Flow')).toEqual('ok');

      expect(res.headers.get('X-Test-Global-Conditioned-Flow')).toEqual('ok');
      expect(res.headers.get('X-Test-API-Root-Conditioned-Flow')).toEqual('ok');
      expect(res.headers.get('X-Test-API-Plan-Conditioned-Flow')).toEqual('ok');
    });
  });
  describe('when condition is false', () => {
    test('Should return 200 OK on `GET /client/foo/keyless`', async () => {
      const res = await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}/client/foo/keyless`,
      });

      expect(res.headers.get('X-Test-Global-Flow')).toEqual('ok');
      expect(res.headers.get('X-Test-API-Root-Flow')).toEqual('ok');
      expect(res.headers.get('X-Test-API-Plan-Flow')).toEqual('ok');

      expect(res.headers.get('X-Test-Global-Conditioned-Flow')).toBeNull();
      expect(res.headers.get('X-Test-API-Root-Conditioned-Flow')).toBeNull();
      expect(res.headers.get('X-Test-API-Plan-Conditioned-Flow')).toBeNull();
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});

// Create Flow and add Transform Headers policy to add header to validate the use of the flow
const fakeTestingFlow = ({ name, path, condition }: { name: string; path: string; condition?: string }) => {
  return {
    name: `[Testing flow] ${name}`,
    path_operator: {
      path,
      operator: PathOperatorOperatorEnum.STARTSWITH,
    },
    condition,
    consumers: [],
    methods: [],
    pre: [],
    post: [
      {
        name: 'Transform Headers',
        description: `Add header to validate "${name}" flow`,
        enabled: true,
        policy: 'transform-headers',
        configuration: {
          addHeaders: [{ name: `X-Test-${name}-Flow`, value: 'ok' }],
          scope: 'RESPONSE',
        },
      },
    ],
    enabled: true,
  };
};
