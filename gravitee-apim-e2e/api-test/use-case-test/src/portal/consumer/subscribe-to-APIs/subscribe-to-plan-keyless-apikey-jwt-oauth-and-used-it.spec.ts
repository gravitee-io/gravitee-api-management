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
import { Application } from '@portal-models/Application';
import { Subscription as PortalSubscription } from '@portal-models/Subscription';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { PortalApplicationFaker } from '@portal-fakers/PortalApplicationFaker';
import { GetSubscriptionByIdIncludeEnum, SubscriptionApi } from '@portal-apis/SubscriptionApi';
import { fetchGatewaySuccess } from '@lib/gateway';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser, forPortalAsAppUser } from '@client-conf/*';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { ApplicationApi } from '@portal-apis/ApplicationApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());
const portalApplicationResource = new ApplicationApi(forPortalAsAppUser());
const portalSubscriptionResource = new SubscriptionApi(forPortalAsAppUser());

describe('Subscribe to plan (Keyless, API Key, JWT, OAuth) and used it', () => {
  describe('Subscribe to an API Key plan', () => {
    let createdApi: ApiEntity;
    let createdPortalApplication: Application;
    let createdPortalSubscription: PortalSubscription;
    let createdPortalApiKey;

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

      // create an application from portal
      createdPortalApplication = await portalApplicationResource.createApplication({
        applicationInput: PortalApplicationFaker.newApplicationInput(),
      });

      // subscribe application to API key plan
      createdPortalSubscription = await portalSubscriptionResource.createSubscription({
        subscriptionInput: {
          application: createdPortalApplication.id,
          plan: createdApi.plans[0].id,
        },
      });

      // get portal subscription API key
      createdPortalApiKey = (
        await portalSubscriptionResource.getSubscriptionById({
          subscriptionId: createdPortalSubscription.id,
          include: [GetSubscriptionByIdIncludeEnum.Keys],
        })
      ).keys[0];
    });

    describe('Gateway call with API key in HTTP header', () => {
      describe('Gateway call with correct X-Gravitee-Api-Key header using portal subscription', () => {
        test('Should return 200 OK', async () => {
          await fetchGatewaySuccess({
            contextPath: createdApi.context_path,
            headers: { 'X-Gravitee-Api-Key': createdPortalApiKey.key },
          });
        });
      });
    });

    describe('Gateway call with API key in query parameter', () => {
      describe('Gateway call with correct api-key query param using portal subscription', () => {
        test('Should return 200 OK', async () => {
          await fetchGatewaySuccess({
            contextPath: `${createdApi.context_path}?${new URLSearchParams({ 'api-key': createdPortalApiKey.key })}`,
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

        // un-publish the API
        await apisResource.updateApi({
          envId,
          orgId,
          api: createdApi.id,
          updateApiEntity: {
            lifecycle_state: ApiLifecycleState.UNPUBLISHED,
            description: createdApi.description,
            name: createdApi.name,
            proxy: createdApi.proxy,
            version: createdApi.version,
            visibility: createdApi.visibility,
          },
        });

        // delete API
        await apisResource.deleteApi({
          envId,
          orgId,
          api: createdApi.id,
        });
      }

      if (createdPortalApplication) {
        await portalApplicationResource.deleteApplicationByApplicationId({
          applicationId: createdPortalApplication.id,
        });
      }
    });
  });
});
