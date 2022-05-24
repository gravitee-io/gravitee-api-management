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
import { forManagementAsApiUser, forManagementAsAppUser } from '@client-conf/*';
import { afterAll, beforeAll, describe, test } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { succeed } from '@lib/jest-utils';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanEntity } from '@management-models/PlanEntity';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { PlanStatus } from '@management-models/PlanStatus';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { fetchGatewaySuccess, fetchGatewayUnauthorized } from '@lib/gateway';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { Subscription } from '@management-models/Subscription';
import { ApiKeyEntity } from '@management-models/ApiKeyEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());
const applicationSubscriptionsResource = new ApplicationSubscriptionsApi(forManagementAsAppUser());
const applicationsResource = new ApplicationsApi(forManagementAsAppUser());

let createdApi: ApiEntity;
let createdApiKeyPlan: PlanEntity;
let createdFreePlan: PlanEntity;
let createdApiKey: ApiKeyEntity;
let createdApplication: ApplicationEntity;
let createdSubscription: Subscription;

describe('Gateway - Plans deployment', () => {
  beforeAll(async () => {
    // create an API with a published API key plan
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({ plans: [PlansFaker.plan({ security: PlanSecurityType.APIKEY, status: PlanStatus.PUBLISHED })] }),
    });

    // start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Without free plan', () => {
    test('Gateway should return HTTP 401', async () => {
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });
  });

  describe('With 1 free plan', () => {
    beforeAll(async () => {
      createdFreePlan = await apisResource.createApiPlan({
        envId,
        orgId,
        api: createdApi.id,
        newPlanEntity: PlansFaker.newPlan(),
      });
    });

    test('gateway should return HTTP 401 cause plan is not published', async () => {
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });

    test('should deploy the API', async () => {
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('gateway should return HTTP 401 cause plan is still not published', async () => {
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });

    test('should publish plan', async () => {
      await succeed(apiPlansResource.publishApiPlanRaw({ envId, orgId, plan: createdFreePlan.id, api: createdApi.id }));
    });

    test('gateway should return HTTP 401 cause published plan is not deployed', async () => {
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });

    test('should redeploy the API with published plan', async () => {
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('gateway should succeed with HTTP 200', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi.context_path });
    });

    test('should close plan', async () => {
      await succeed(apiPlansResource.closeApiPlanRaw({ envId, orgId, plan: createdFreePlan.id, api: createdApi.id }));
    });

    test('gateway should still succeed with HTTP 200 cause closed plan not deployed', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi.context_path });
    });

    test('should redeploy the API with closed plan', async () => {
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('gateway should return HTTP 401 cause free plan is closed', async () => {
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });
  });

  describe('With 1 additional API key plan', () => {
    beforeAll(async () => {
      createdApiKeyPlan = await apisResource.createApiPlan({
        envId,
        orgId,
        api: createdApi.id,
        newPlanEntity: PlansFaker.newPlan({ security: PlanSecurityType.APIKEY, status: PlanStatus.PUBLISHED }),
      });

      // create an application
      createdApplication = await applicationsResource.createApplication({
        envId,
        orgId,
        newApplicationEntity: ApplicationsFaker.newApplication(),
      });

      // subscribe application to plan
      createdSubscription = await applicationSubscriptionsResource.createSubscriptionWithApplication({
        envId,
        orgId,
        plan: createdApiKeyPlan.id,
        application: createdApplication.id,
      });

      // get the subscription api key
      createdApiKey = (
        await applicationSubscriptionsResource.getApiKeysForApplicationSubscription({
          envId,
          orgId,
          application: createdApplication.id,
          subscription: createdSubscription.id,
        })
      )[0];
    });

    test('gateway should return HTTP 401 cause published api key plan is not deployed', async () => {
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path, headers: { 'X-Gravitee-Api-Key': createdApiKey.key } });
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });

    test('should redeploy the API with published API key plan', async () => {
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('gateway should succeed with HTTP 200 on API key plan, but still not authorized on free plan', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi.context_path, headers: { 'X-Gravitee-Api-Key': createdApiKey.key } });
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });

    test('should recreate 1 published free plan', async () => {
      createdFreePlan = await apisResource.createApiPlan({
        envId,
        orgId,
        api: createdApi.id,
        newPlanEntity: PlansFaker.newPlan({ status: PlanStatus.PUBLISHED }),
      });
    });

    test('should redeploy the API with published API key plan and free plan', async () => {
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('gateway should succeed with HTTP 200 on API key plan, and on free plan', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi.context_path, headers: { 'X-Gravitee-Api-Key': createdApiKey.key } });
      await fetchGatewaySuccess({ contextPath: createdApi.context_path });
    });

    test('should close free plan', async () => {
      await succeed(apiPlansResource.closeApiPlanRaw({ envId, orgId, plan: createdFreePlan.id, api: createdApi.id }));
    });

    test('should redeploy the API with closed API key plan and free plan', async () => {
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('gateway should succeed with HTTP 200 on apikey plan, but no more on free plan', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi.context_path, headers: { 'X-Gravitee-Api-Key': createdApiKey.key } });
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });

    test('should close apikey plan', async () => {
      await succeed(apiPlansResource.closeApiPlanRaw({ envId, orgId, plan: createdApiKeyPlan.id, api: createdApi.id }));
    });

    test('should redeploy the API with closed API key plan and closed free plan', async () => {
      await succeed(apisResource.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('gateway should fail with HTTP 401 on both plans', async () => {
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path, headers: { 'X-Gravitee-Api-Key': createdApiKey.key } });
      await fetchGatewayUnauthorized({ contextPath: createdApi.context_path });
    });

    afterAll(async () => {
      if (createdApplication) {
        await applicationsResource.deleteApplication({
          envId,
          orgId,
          application: createdApplication.id,
        });
      }
    });
  });

  afterAll(async () => {
    if (createdApi) {
      // stop API
      await apisResource.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.STOP,
      });

      // close plan
      await apisResource.closeApiPlan({
        envId,
        orgId,
        plan: createdApi.plans[0].id,
        api: createdApi.id,
      });

      // delete API
      await apisResource.deleteApi({
        envId,
        orgId,
        api: createdApi.id,
      });
    }
  });
});
