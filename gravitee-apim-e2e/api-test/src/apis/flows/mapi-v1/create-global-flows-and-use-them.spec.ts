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
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@gravitee/utils/configuration';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { ApiEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { PlanSecurityType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanSecurityType';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { fetchGatewaySuccess } from '@gravitee/utils/apim-http';
import { PathOperatorOperatorEnum } from '@gravitee/management-webclient-sdk/src/lib/models/PathOperator';
import { PlanEntity } from '@gravitee/management-webclient-sdk/src/lib/models/PlanEntity';
import { OrganizationEntityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/OrganizationEntity';
import { OrganizationApi } from '@gravitee/management-webclient-sdk/src/lib/apis/OrganizationApi';
import { NewApiEntityFlowModeEnum } from '@gravitee/management-webclient-sdk/src/lib/models/NewApiEntity';
import { teardownApisAndApplications } from '@gravitee/utils/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
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
              operator: PathOperatorOperatorEnum.STARTS_WITH,
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
        flow_mode: NewApiEntityFlowModeEnum.BEST_MATCH,
        // With flow on root path
        flows: [
          {
            name: '',
            path_operator: {
              path: '/client/:clientName',
              operator: PathOperatorOperatorEnum.STARTS_WITH,
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
        security: PlanSecurityType.KEY_LESS,
        status: PlanStatus.PUBLISHED,
        flows: [
          // With flow on this plan
          {
            name: '',
            path_operator: {
              path: '/client/:clientName/keyless',
              operator: PathOperatorOperatorEnum.STARTS_WITH,
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
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
