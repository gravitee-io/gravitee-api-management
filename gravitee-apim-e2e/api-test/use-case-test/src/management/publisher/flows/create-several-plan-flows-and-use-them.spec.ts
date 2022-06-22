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
import { forManagementAsApiUser } from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiEntity, ApiEntityFlowModeEnum, ApiEntityToJSON } from '@management-models/ApiEntity';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanStatus } from '@management-models/PlanStatus';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { Subscription } from '@management-models/Subscription';
import { ApiKeyEntity } from '@management-models/ApiKeyEntity';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { fetchGatewaySuccess } from '@lib/gateway';
import { FlowMethodsEnum } from '@management-models/Flow';
import { UpdatePlanEntityFromJSON } from '@management-models/UpdatePlanEntity';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { PlanEntity, PlanEntityToJSON } from '@management-models/PlanEntity';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { succeed } from '@lib/jest-utils';
import { teardownApisAndApplications } from '@lib/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const applicationsResource = new ApplicationsApi(forManagementAsApiUser());
const applicationSubscriptionsResource = new ApplicationSubscriptionsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());

describe('Create several plan flows and use them', () => {
  let createdApi: ApiEntity;
  let createdApplication: ApplicationEntity;
  let createdApiKey: ApiKeyEntity;
  let createdKeylessPlan: PlanEntity;
  let createdApiKeyPlan: PlanEntity;
  let createdSubscription: Subscription;

  beforeAll(async () => {
    // Create new API
    createdApi = await apisResource.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi({
        gravitee: '2.0.0',
        // With flow on root path
        flows: [
          {
            name: '',
            path_operator: {
              path: '/plan',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [],
            pre: [],
            post: [
              // Add policy to test this flow and check flow mode
              {
                name: 'Transform Headers',
                description: 'Add header to validate flow and flow mode',
                enabled: true,
                policy: 'transform-headers',
                configuration: {
                  addHeaders: [
                    { name: 'X-Test', value: 'api-root-flow-ok' },
                    { name: 'X-Flow-Mode', value: 'DEFAULT' },
                  ],
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
          // With static path flow and mock policy
          {
            name: '',
            path_operator: {
              path: '/plan/keyless/foo',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [FlowMethodsEnum.GET],
            pre: [
              {
                name: 'Mock',
                policy: 'mock',
                description: 'This mock policy was created by a test',
                enabled: true,
                configuration: {
                  status: '200',
                  content: 'This is Keyless Foo plan',
                },
              },
            ],
            post: [],
            enabled: true,
          },
          // With dynamic path flow and mock policy
          {
            name: '',
            path_operator: {
              path: '/plan/keyless/:planName',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [FlowMethodsEnum.GET],
            pre: [
              {
                name: 'Mock',
                policy: 'mock',
                description: 'This mock policy was created by a test',
                enabled: true,
                configuration: {
                  status: '200',
                  content: 'This is Keyless :planName plan',
                },
              },
            ],
            post: [],
            enabled: true,
          },
        ],
      }),
    });

    // Create second plan with ApiKey
    createdApiKeyPlan = await apisResource.createApiPlan({
      orgId,
      envId,
      api: createdApi.id,
      newPlanEntity: PlansFaker.newPlan({ security: PlanSecurityType.APIKEY, status: PlanStatus.PUBLISHED }),
    });

    // Update it to add flow and mock policy
    const apiKeyPlan = await apiPlansResource.getApiPlan({ envId, orgId, plan: createdApiKeyPlan.id, api: createdApi.id });
    apiKeyPlan.flows = [
      {
        name: '',
        path_operator: {
          path: '/plan/apikey/foo',
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
              content: 'This is ApiKey plan',
            },
          },
        ],
        post: [],
        enabled: true,
      },
    ];
    await apiPlansResource.updateApiPlan({
      envId,
      orgId,
      plan: createdApiKeyPlan.id,
      api: createdApi.id,
      updatePlanEntity: UpdatePlanEntityFromJSON(PlanEntityToJSON(apiKeyPlan)),
    });

    // Create an application
    createdApplication = await applicationsResource.createApplication({
      envId,
      orgId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });

    // Subscribe application to ApiKey plan
    createdSubscription = await applicationSubscriptionsResource.createSubscriptionWithApplication({
      envId,
      orgId,
      plan: createdApiKeyPlan.id,
      application: createdApplication.id,
    });

    // Get the subscription api key
    createdApiKey = (
      await applicationSubscriptionsResource.getApiKeysForApplicationSubscription({
        envId,
        orgId,
        application: createdApplication.id,
        subscription: createdSubscription.id,
      })
    )[0];

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

  describe('With `Default` Flow Mode', function () {
    describe('Call on the Keyless plan', () => {
      test('Should return 200 OK on `GET /plan/keyless/foo`', async () => {
        const res = await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}/plan/keyless/foo`,
        });

        expect(res.headers.get('X-Test')).toEqual('api-root-flow-ok');
        expect(await res.text()).toEqual('This is Keyless :planName plan'); // 📝 Default mode effect.
      });

      test('Should return 200 OK on `GET /plan/keyless/dynamicPlanName`', async () => {
        const res = await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}/plan/keyless/dynamicPlanName`,
        });

        expect(await res.text()).toEqual('This is Keyless :planName plan');
      });
    });

    describe('Call on the ApiKey plan', () => {
      test('Should return 200 OK on `GET /plan/apikey/foo`', async () => {
        const res = await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}/plan/apikey/foo?${new URLSearchParams({ 'api-key': createdApiKey.key })}`,
        });

        expect(await res.text()).toEqual('This is ApiKey plan');
      });
    });
  });

  describe('With `Best Match` Flow Mode', function () {
    beforeAll(async () => {
      // Set flow mode to Best Match and update api
      createdApi.flow_mode = ApiEntityFlowModeEnum.BESTMATCH;
      createdApi.flows[0].post[0].configuration.addHeaders = [
        { name: 'X-Test', value: 'api-root-flow-ok' },
        { name: 'X-Flow-Mode', value: 'BEST_MATCH' },
      ];
      await apisResource.updateApi({
        envId,
        orgId,
        api: createdApi.id,
        updateApiEntity: UpdateApiEntityFromJSON(ApiEntityToJSON(createdApi)),
      });

      // Redeploy the api
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));

      // Wait for the effective redeployment
      await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}/plan/keyless/foo`,
        expectedResponseValidator: (response) => response.headers.get('X-Flow-Mode') === 'BEST_MATCH',
      });
    });

    describe('Call on the Keyless plan', () => {
      test('Should return 200 OK on `GET /plan/keyless/foo`', async () => {
        const res = await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}/plan/keyless/foo`,
        });

        expect(await res.text()).toEqual('This is Keyless Foo plan'); // 📝 Best match effect. Closer paths have priority
      });

      test('Should return 200 OK on `GET /plan/keyless/dynamicPlanName`', async () => {
        const res = await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}/plan/keyless/dynamicPlanName`,
        });

        expect(await res.text()).toEqual('This is Keyless :planName plan');
      });
    });

    describe('Call on the ApiKey plan', () => {
      test('Should return 200 OK on `GET /plan/apikey/foo`', async () => {
        const res = await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}/plan/apikey/foo?${new URLSearchParams({ 'api-key': createdApiKey.key })}`,
        });
        expect(res.headers.get('X-Test')).toEqual('api-root-flow-ok');
        expect(await res.text()).toEqual('This is ApiKey plan');
      });
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id], [createdApplication.id]);
  });
});
