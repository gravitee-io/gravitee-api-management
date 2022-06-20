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
import { afterAll, beforeAll, describe } from '@jest/globals';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { Subscription } from '@management-models/Subscription';
import { ApiKeyEntity } from '@management-models/ApiKeyEntity';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { fetchGatewaySuccess, fetchGatewayUnauthorized } from '@lib/gateway';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { APISubscriptionsApi } from '@management-apis/APISubscriptionsApi';
import { APIPlansApi } from '@management-apis/APIPlansApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const applicationsResource = new ApplicationsApi(forManagementAsApiUser());
const applicationSubscriptionsResource = new ApplicationSubscriptionsApi(forManagementAsApiUser());
const apiSubscriptionResource = new APISubscriptionsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());

describe('Create an API with API-Key plan and use it', () => {
  let createdApi: ApiEntity;
  let createdApplicationSubscriptionApplication: ApplicationEntity;
  let createdApiSubscriptionApplication: ApplicationEntity;
  let createdApplicationSubscription: Subscription;
  let createdApiSubscription: Subscription;
  let createdApplicationSubscriptionApiKey: ApiKeyEntity;
  let createdApiSubscriptionApiKey: ApiKeyEntity;

  beforeAll(async () => {
    // create an API with a published API key plan
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ security: PlanSecurityType.APIKEY, status: PlanStatus.PUBLISHED })],
      }),
    });

    // publish the API
    await apisResource.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: {
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        description: createdApi.description,
        name: createdApi.name,
        proxy: createdApi.proxy,
        version: createdApi.version,
        visibility: createdApi.visibility,
      },
    });

    // start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });

    // create an application to subscribe to the API key plan from console application
    createdApplicationSubscriptionApplication = await applicationsResource.createApplication({
      envId,
      orgId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });

    // create an application to subscribe to the API key plan from console API
    createdApiSubscriptionApplication = await applicationsResource.createApplication({
      envId,
      orgId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });

    // subscribe application to plan through console application
    createdApplicationSubscription = await applicationSubscriptionsResource.createSubscriptionWithApplication({
      envId,
      orgId,
      plan: createdApi.plans[0].id,
      application: createdApplicationSubscriptionApplication.id,
    });

    // get the application subscription API key
    createdApplicationSubscriptionApiKey = (
      await applicationSubscriptionsResource.getApiKeysForApplicationSubscription({
        envId,
        orgId,
        application: createdApplicationSubscriptionApplication.id,
        subscription: createdApplicationSubscription.id,
      })
    )[0];

    // subscribe application to plan through console API
    createdApiSubscription = await apiSubscriptionResource.createSubscriptionToApi({
      envId,
      orgId,
      plan: createdApi.plans[0].id,
      application: createdApiSubscriptionApplication.id,
      api: createdApi.id,
    });

    // get console API subscription API key
    createdApiSubscriptionApiKey = (
      await apiSubscriptionResource.getApiKeysForApiSubscription({
        envId,
        orgId,
        api: createdApi.id,
        subscription: createdApiSubscription.id,
      })
    )[0];
  });

  describe('Gateway call with API key in HTTP header', () => {
    describe.each`
      case                                 | headers
      ${'no X-Gravitee-Api-Key header'}    | ${{}}
      ${'empty X-Gravitee-Api-Key header'} | ${{ 'X-Gravitee-Api-Key': '' }}
      ${'wrong X-Gravitee-Api-Key header'} | ${{ 'X-Gravitee-Api-Key': 'wrong key' }}
    `('Gateway call with $case', ({ headers }) => {
      test('Should return 401 unauthorized', async () => {
        await fetchGatewayUnauthorized({ contextPath: createdApi.context_path, headers });
      });
    });

    describe('Gateway call with correct X-Gravitee-Api-Key header using application subscription', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: createdApi.context_path,
          headers: { 'X-Gravitee-Api-Key': createdApplicationSubscriptionApiKey.key },
        });
      });
    });

    describe('Gateway call with correct X-Gravitee-Api-Key header using API subscription', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: createdApi.context_path,
          headers: { 'X-Gravitee-Api-Key': createdApiSubscriptionApiKey.key },
        });
      });
    });
  });

  describe('Gateway call with API key in query parameter', () => {
    describe.each`
      case                           | queryParams
      ${'no api-key query param'}    | ${{}}
      ${'empty api-key query param'} | ${{ 'api-key': '' }}
      ${'wrong api-key query param'} | ${{ 'api-key': 'wrong key' }}
    `('Gateway call with $case', ({ queryParams }) => {
      test('Should return 401 unauthorized', async () => {
        await fetchGatewayUnauthorized({ contextPath: `${createdApi.context_path}?${new URLSearchParams(queryParams)}` });
      });
    });

    describe('Gateway call with correct api-key query param using application subscription', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}?${new URLSearchParams({ 'api-key': createdApplicationSubscriptionApiKey.key })}`,
        });
      });
    });

    describe('Gateway call with correct api-key query param using API subscription', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}?${new URLSearchParams({ 'api-key': createdApiSubscriptionApiKey.key })}`,
        });
      });
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
      await apiPlansResource.closeApiPlan({
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

    // delete applications
    if (createdApplicationSubscriptionApplication) {
      await applicationsResource.deleteApplication({
        envId,
        orgId,
        application: createdApplicationSubscriptionApplication.id,
      });
    }

    if (createdApiSubscriptionApplication) {
      await applicationsResource.deleteApplication({
        envId,
        orgId,
        application: createdApiSubscriptionApplication.id,
      });
    }
  });
});
