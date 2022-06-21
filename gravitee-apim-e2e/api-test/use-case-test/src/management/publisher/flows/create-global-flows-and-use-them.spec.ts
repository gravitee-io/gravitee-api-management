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
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { Subscription } from '@management-models/Subscription';
import { ApiKeyEntity } from '@management-models/ApiKeyEntity';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { fetchGatewaySuccess } from '@lib/gateway';
import { FlowMethodsEnum } from '@management-models/Flow';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { PlanEntity } from '@management-models/PlanEntity';
import { OrganizationEntityToJSON } from '@management-models/OrganizationEntity';
import { OrganizationApi } from '@management-apis/OrganizationApi';
import { NewApiEntityFlowModeEnum } from '@management-models/NewApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());
const organizationApi = new OrganizationApi(forManagementAsAdminUser());

describe('Create global flows and use them', () => {
  let createdApi: ApiEntity;
  let createdKeylessPlan: PlanEntity;

  beforeAll(async () => {
    // Create Global Flow with /env
    const organization = await organizationApi.get({ orgId });
    await organizationApi.update({
      orgId,
      updateOrganizationEntity: {
        ...OrganizationEntityToJSON(organization),
        flows: [
          // Add global flow
          {
            name: '',
            path_operator: {
              path: '/client',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [],
            pre: [],
            post: [
              // Add policy to test this flow
              {
                name: 'Transform Headers',
                description: 'Add header to validate flow',
                enabled: true,
                policy: 'transform-headers',
                configuration: {
                  addHeaders: [{ name: 'X-Test-Global-Flow', value: 'ok' }],
                  scope: 'RESPONSE',
                },
              },
            ],
            enabled: true,
          },
        ],
      },
    });

    // Create new API
    createdApi = await apisResource.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi({
        gravitee: '2.0.0',
        flow_mode: NewApiEntityFlowModeEnum.BESTMATCH,
        // With flow on root path
        flows: [
          {
            name: '',
            path_operator: {
              path: '/client/:clientName',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [],
            pre: [],
            post: [
              // Add policy to test this flow
              {
                name: 'Transform Headers',
                description: 'Add header to validate flow',
                enabled: true,
                policy: 'transform-headers',
                configuration: {
                  addHeaders: [{ name: 'X-Test-API-Root-Flow', value: 'ok' }],
                  scope: 'RESPONSE',
                },
              },
            ],
            enabled: true,
          },
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
          // With flow on this plan
          {
            name: '',
            path_operator: {
              path: '/client/:clientName/keyless',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [],
            pre: [],
            post: [
              // Add policy to test this plan flow
              {
                name: 'Transform Headers',
                description: 'Add header to validate flow',
                enabled: true,
                policy: 'transform-headers',
                configuration: {
                  addHeaders: [{ name: 'X-Test-API-Plan-Flow', value: 'ok' }],
                  scope: 'RESPONSE',
                },
              },
            ],
            enabled: true,
          },
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

  test('Should return 200 OK on `GET /`', async () => {
    const res = await fetchGatewaySuccess({
      contextPath: `${createdApi.context_path}`,
    });

    expect(res.headers.get('X-Test-Global-Flow')).not.toEqual('ok');
    expect(res.headers.get('X-Test-API-Root-Flow')).not.toEqual('ok');
    expect(res.headers.get('X-Test-API-Plan-Flow')).not.toEqual('ok');
  });

  test('Should return 200 OK on `GET /client`', async () => {
    const res = await fetchGatewaySuccess({
      contextPath: `${createdApi.context_path}/client`,
    });

    expect(res.headers.get('X-Test-Global-Flow')).toEqual('ok');
    expect(res.headers.get('X-Test-API-Root-Flow')).not.toEqual('ok');
    expect(res.headers.get('X-Test-API-Plan-Flow')).not.toEqual('ok');
  });

  test('Should return 200 OK on `GET /client/foo`', async () => {
    const res = await fetchGatewaySuccess({
      contextPath: `${createdApi.context_path}/client/foo`,
    });

    expect(res.headers.get('X-Test-Global-Flow')).toEqual('ok');
    expect(res.headers.get('X-Test-API-Root-Flow')).toEqual('ok');
    expect(res.headers.get('X-Test-API-Plan-Flow')).not.toEqual('ok');
  });

  test('Should return 200 OK on `GET /client/foo/keyless`', async () => {
    const res = await fetchGatewaySuccess({
      contextPath: `${createdApi.context_path}/client/foo/keyless`,
    });

    expect(res.headers.get('X-Test-Global-Flow')).toEqual('ok');
    expect(res.headers.get('X-Test-API-Root-Flow')).toEqual('ok');
    expect(res.headers.get('X-Test-API-Plan-Flow')).toEqual('ok');
  });

  afterAll(async () => {
    if (createdApi) {
      // Stop API
      await apisResource.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.STOP,
      });

      // Close Keyless plan
      await apiPlansResource.closeApiPlan({
        envId,
        orgId,
        plan: createdKeylessPlan.id,
        api: createdApi.id,
      });

      // Delete API
      await apisResource.deleteApi({
        envId,
        orgId,
        api: createdApi.id,
      });
    }
  });
});
