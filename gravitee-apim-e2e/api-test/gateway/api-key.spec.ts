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
import { ApiEntity } from '@management-models/ApiEntity';
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
import { fetchGateway } from '../../lib/gateway';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const applicationsResource = new ApplicationsApi(forManagementAsApiUser());
const applicationSubscriptionsResource = new ApplicationSubscriptionsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());

describe('Gateway - Api Key', () => {
  let createdApi: ApiEntity;
  let createdApplication: ApplicationEntity;
  let createdSubscription: Subscription;
  let createdApiKey: ApiKeyEntity;

  beforeAll(async () => {
    // create an API with a published API key plan
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ security: PlanSecurityType.APIKEY, status: PlanStatus.PUBLISHED })],
      }),
    });

    // start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
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
      plan: createdApi.plans[0].id,
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

  describe('Gateway call with API key in HTTP header', () => {
    describe.each`
      case                                 | headers
      ${'no X-Gravitee-Api-Key header'}    | ${{}}
      ${'empty X-Gravitee-Api-Key header'} | ${{ 'X-Gravitee-Api-Key': '' }}
      ${'wrong X-Gravitee-Api-Key header'} | ${{ 'X-Gravitee-Api-Key': 'wrong key' }}
    `('Gateway call with $case', ({ headers }) => {
      test('Should return 401 unauthorized', async () => {
        await fetchGateway(createdApi.context_path, 'GET', null, headers).then((response) => expect(response.status).toBe(401));
      });
    });

    describe('Gateway call with correct X-Gravitee-Api-Key header', () => {
      test('Should return 200 OK', async () => {
        await fetchGateway(createdApi.context_path, 'GET', null, { 'X-Gravitee-Api-Key': createdApiKey.key }).then((response) =>
          expect(response.status).toBe(200),
        );
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
        await fetchGateway(`${createdApi.context_path}?${new URLSearchParams(queryParams)}`).then((response) =>
          expect(response.status).toBe(401),
        );
      });
    });

    describe('Gateway call with correct api-key query param', () => {
      test('Should return 200 OK', async () => {
        await fetchGateway(`${createdApi.context_path}?${new URLSearchParams({ 'api-key': createdApiKey.key })}`).then((response) =>
          expect(response.status).toBe(200),
        );
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

    // delete application
    if (createdApplication) {
      await applicationsResource.deleteApplication({
        envId,
        orgId,
        application: createdApplication.id,
      });
    }
  });
});
