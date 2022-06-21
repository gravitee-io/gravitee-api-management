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
import { forManagementAsAdminUser, forManagementAsApiUser, forPortalAsAppUser } from '@client-conf/*';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { ApplicationApi } from '@portal-apis/ApplicationApi';
import { SettingsApi } from '@management-apis/SettingsApi';
import { ApiKeyModeEnum } from '@portal-models/ApiKeyModeEnum';
import { PortalSettingsEntity } from '@management-models/PortalSettingsEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const settingsResource = new SettingsApi(forManagementAsAdminUser());
const apisResource = new APIsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());
const portalApplicationResource = new ApplicationApi(forPortalAsAppUser());
const portalSubscriptionResource = new SubscriptionApi(forPortalAsAppUser());

describe('Subscribe using shared API-Key and use it', () => {
  let portalSettingsEntity: PortalSettingsEntity;
  let createdApi1: ApiEntity;
  let createdApi2: ApiEntity;
  let createdApplication: Application;
  let createdSubscription1: PortalSubscription;
  let createdSubscription2: PortalSubscription;
  let createdSharedApiKey;

  beforeAll(async () => {
    // enable shared API key mode at the environment level
    portalSettingsEntity = await settingsResource.getPortalSettings({ orgId, envId });
    portalSettingsEntity.plan.security.sharedApiKey.enabled = true;
    await settingsResource.savePortalSettings({ orgId, envId, portalSettingsEntity });

    // create APIs with a published API key plan
    createdApi1 = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ security: PlanSecurityType.APIKEY, status: PlanStatus.PUBLISHED })],
      }),
    });

    createdApi2 = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ security: PlanSecurityType.APIKEY, status: PlanStatus.PUBLISHED })],
      }),
    });

    // publish the APIs
    for (const api of [createdApi1, createdApi2]) {
      await apisResource.updateApi({
        envId,
        orgId,
        api: api.id,
        updateApiEntity: {
          lifecycle_state: ApiLifecycleState.PUBLISHED,
          description: api.description,
          name: api.name,
          proxy: api.proxy,
          version: api.version,
          visibility: api.visibility,
        },
      });
    }

    // start APIs
    for (const api of [createdApi1, createdApi2]) {
      await apisResource.doApiLifecycleAction({
        envId,
        orgId,
        api: api.id,
        action: LifecycleAction.START,
      });
    }

    // create an application from portal
    createdApplication = await portalApplicationResource.createApplication({
      applicationInput: PortalApplicationFaker.newApplicationInput(),
    });

    // subscribe application to first API key plan
    createdSubscription1 = await portalSubscriptionResource.createSubscription({
      subscriptionInput: {
        application: createdApplication.id,
        plan: createdApi1.plans[0].id,
      },
    });

    // enable shared API key mode at the application level
    await portalApplicationResource.updateApplicationByApplicationId({
      applicationId: createdApplication.id,
      application: { ...createdApplication, api_key_mode: ApiKeyModeEnum.SHARED },
    });

    // subscribe application to the other API key plan
    createdSubscription2 = await portalSubscriptionResource.createSubscription({
      subscriptionInput: {
        application: createdApplication.id,
        plan: createdApi2.plans[0].id,
      },
    });

    // get portal subscription API key
    createdSharedApiKey = (
      await portalSubscriptionResource.getSubscriptionById({
        subscriptionId: createdSubscription1.id,
        include: [GetSubscriptionByIdIncludeEnum.Keys],
      })
    ).keys[0];
  });

  describe('Gateway call with shared API key in HTTP header', () => {
    describe('Gateway call with correct X-Gravitee-Api-Key header using shared API key', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: createdApi1.context_path,
          headers: { 'X-Gravitee-Api-Key': createdSharedApiKey.key },
        });
      });

      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: createdApi2.context_path,
          headers: { 'X-Gravitee-Api-Key': createdSharedApiKey.key },
        });
      });
    });
  });

  describe('Gateway call with API key in query parameter', () => {
    describe('Gateway call with correct api-key query param using portal subscription', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: `${createdApi1.context_path}?${new URLSearchParams({ 'api-key': createdSharedApiKey.key })}`,
        });
      });

      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: `${createdApi2.context_path}?${new URLSearchParams({ 'api-key': createdSharedApiKey.key })}`,
        });
      });
    });
  });

  afterAll(async () => {
    // disable shared API key mode at the environment level
    portalSettingsEntity.plan.security.sharedApiKey.enabled = false;
    await settingsResource.savePortalSettings({ orgId, envId, portalSettingsEntity });

    for (const api of [createdApi1, createdApi2]) {
      if (api) {
        // stop API
        await apisResource.doApiLifecycleAction({
          envId,
          orgId,
          api: api.id,
          action: LifecycleAction.STOP,
        });

        // close plan
        await apiPlansResource.closeApiPlan({
          envId,
          orgId,
          plan: api.plans[0].id,
          api: api.id,
        });

        // un-publish the API
        await apisResource.updateApi({
          envId,
          orgId,
          api: api.id,
          updateApiEntity: {
            lifecycle_state: ApiLifecycleState.UNPUBLISHED,
            description: api.description,
            name: api.name,
            proxy: api.proxy,
            version: api.version,
            visibility: api.visibility,
          },
        });

        // delete API
        await apisResource.deleteApi({
          envId,
          orgId,
          api: api.id,
        });
      }
    }

    if (createdApplication) {
      await portalApplicationResource.deleteApplicationByApplicationId({
        applicationId: createdApplication.id,
      });
    }
  });
});
