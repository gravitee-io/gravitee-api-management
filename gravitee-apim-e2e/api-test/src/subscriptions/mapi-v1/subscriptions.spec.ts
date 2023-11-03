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
import { APIsApi } from '../../../../lib/management-webclient-sdk/src/lib/apis/APIsApi';
import {
  forManagementAsAdminUser,
  forManagementAsApiUser,
  forManagementAsAppUser,
  forPortalAsAdminUser,
  forPortalAsAppUser,
} from '@gravitee/utils/configuration';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { ApiEntity } from '../../../../lib/management-webclient-sdk/src/lib/models/ApiEntity';
import { ApiLifecycleState } from '../../../../lib/management-webclient-sdk/src/lib/models/ApiLifecycleState';
import { APIPlansApi } from '../../../../lib/management-webclient-sdk/src/lib/apis/APIPlansApi';
import { ApplicationApi as PortalApplicationApi } from '../../../../lib/portal-webclient-sdk/src/lib/apis/ApplicationApi';
import { ApplicationsApi } from '../../../../lib/management-webclient-sdk/src/lib/apis/ApplicationsApi';
import { ApplicationsFaker } from '@gravitee/fixtures/management/ApplicationsFaker';
import fetchApi from 'node-fetch';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PlanValidationType } from '../../../../lib/management-webclient-sdk/src/lib/models/PlanValidationType';
import { PlanEntity } from '../../../../lib/management-webclient-sdk/src/lib/models/PlanEntity';
import { Api } from '../../../../lib/portal-webclient-sdk/src/lib/models/Api';
import { ApplicationEntity } from '../../../../lib/management-webclient-sdk/src/lib/models/ApplicationEntity';
import { PlanSecurityType } from '../../../../lib/management-webclient-sdk/src/lib/models/PlanSecurityType';
import { ApiApi } from '../../../../lib/portal-webclient-sdk/src/lib/apis/ApiApi';
import { Visibility } from '../../../../lib/management-webclient-sdk/src/lib/models/Visibility';
import { succeed } from '@lib/jest-utils';
import { GetSubscriptionByIdIncludeEnum, SubscriptionApi } from '../../../../lib/portal-webclient-sdk/src/lib/apis/SubscriptionApi';
import faker from '@faker-js/faker';
import { Subscription } from '../../../../lib/portal-webclient-sdk/src/lib/models/Subscription';
import { APISubscriptionsApi } from '../../../../lib/management-webclient-sdk/src/lib/apis/APISubscriptionsApi';
import { UpdateApiEntityFromJSON } from '../../../../lib/management-webclient-sdk/src/lib/models/UpdateApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

// Portal resources
const apisAsAppUser = new ApiApi(forPortalAsAppUser());
const portalApplicationsAsAppUser = new PortalApplicationApi(forPortalAsAppUser());
const portalApplicationsAsAdmin = new PortalApplicationApi(forPortalAsAdminUser());
const subscriptionAsAppUser = new SubscriptionApi(forPortalAsAppUser());

// Management resources
const apisAsUser = new APIsApi(forManagementAsApiUser());
const apiPlansAsApiUser = new APIPlansApi(forManagementAsApiUser());
const applicationManagementApiAsAppUser = new ApplicationsApi(forManagementAsAppUser());
const apiSubscriptionsAsAdminUser = new APISubscriptionsApi(forManagementAsAdminUser());

let createdApi: ApiEntity;
let createdPlan: PlanEntity;
let createdApplication: ApplicationEntity;
let api: Api;
let createdSubscription: Subscription;

describe.each([PlanValidationType.AUTO, PlanValidationType.MANUAL])('Subscriptions tests with %s plan validation', (planValidationType) => {
  beforeAll(async () => {
    // create an api
    createdApi = await apisAsUser.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi(),
    });

    // publish an api
    await apisAsUser.updateApi({
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({
        ...createdApi,
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        visibility: Visibility.PUBLIC,
      }),
      orgId,
      envId,
    });

    // create the api plan
    createdPlan = await apiPlansAsApiUser.createApiPlan({
      envId,
      orgId,
      api: createdApi.id,
      newPlanEntity: PlansFaker.newPlan({ security: PlanSecurityType.API_KEY, validation: planValidationType }),
    });

    // publish the api plan
    await apiPlansAsApiUser.publishApiPlanRaw({ envId, orgId, plan: createdPlan.id, api: createdApi.id });

    // create application for subscription
    createdApplication = await applicationManagementApiAsAppUser.createApplication({
      orgId,
      envId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });
  });

  describe('Subscription flow', () => {
    test('should get portal apis and find the created one', async () => {
      const createdApis = await succeed(
        apisAsAppUser.getApisRaw({
          size: -1,
        }),
      );
      api = createdApis.data.find((api) => api.id === createdApi.id);
      expect(api).toBeDefined();
      expect(api._links).toBeTruthy();
      expect(api._links.self).toBeTruthy();
      expect(api._links.picture).toBeTruthy();
      expect(api._links.plans).toBeTruthy();
    });

    test('should get the api from api._links', async () => {
      const selfLinkApi = await fetchApi(api._links.self, { method: 'GET' });

      expect(selfLinkApi).toBeTruthy();
      expect(selfLinkApi.status).toEqual(200);

      const body = await selfLinkApi.json();
      expect(body).toBeTruthy();
      expect(body.name).toStrictEqual(createdApi.name);
    });

    test('should get the api picture from api._links', async () => {
      const apiPicture = await fetchApi(api._links.picture, { method: 'GET' });

      expect(apiPicture).toBeTruthy();
      expect(apiPicture.status).toEqual(200);
      expect(apiPicture.headers.get('ETag')).toBeNull();
    });

    test('should get the api plans from api._links', async () => {
      const apiPlans = await fetchApi(api._links.plans, { method: 'GET' }).then((response) => response.json());

      expect(apiPlans.data).toHaveLength(1);
      expect(apiPlans.metadata.data.total).toStrictEqual(apiPlans.data.length);
      expect(apiPlans.data).toHaveLength(1);
      expect(apiPlans.data[0].id).toStrictEqual(createdPlan.id);
    });

    test('should get applications', async () => {
      const portalApplications = await succeed(
        portalApplicationsAsAppUser.getApplicationsRaw({
          size: -1,
        }),
      );

      expect(portalApplications).toBeTruthy();
      expect(portalApplications.data.find((app) => app.id === createdApplication.id)).toBeTruthy();
    });

    test('should create a subscription', async () => {
      createdSubscription = await succeed(
        subscriptionAsAppUser.createSubscriptionRaw({
          subscriptionInput: {
            application: createdApplication.id,
            plan: createdPlan.id,
            request: faker.lorem.sentence(10),
          },
        }),
      );
    });

    if (planValidationType === PlanValidationType.MANUAL) {
      test('Api publisher should accept subscription', async () => {
        const starting_at = new Date();
        const ending_at = new Date();
        ending_at.setDate(ending_at.getDate() + 1);

        await succeed(
          apiSubscriptionsAsAdminUser.processApiSubscriptionRaw({
            envId,
            orgId,
            api: createdApi.id,
            subscription: createdSubscription.id,
            processSubscriptionEntity: {
              accepted: true,
              starting_at,
              ending_at,
            },
          }),
        );
      });
    }

    test('should get the subscription with the keys', async () => {
      const subscription = await succeed(
        subscriptionAsAppUser.getSubscriptionByIdRaw({
          subscriptionId: createdSubscription.id,
          include: [GetSubscriptionByIdIncludeEnum.Keys],
        }),
      );

      expect(subscription).toBeTruthy();
      expect(subscription.keys).toBeTruthy();
    });

    test('should get the subscription without the keys', async () => {
      const subscription = await succeed(
        subscriptionAsAppUser.getSubscriptionByIdRaw({
          subscriptionId: createdSubscription.id,
        }),
      );

      expect(subscription).toBeTruthy();
      expect(subscription.keys).toBeUndefined();
    });
  });

  test('should get application subscribers', async () => {
    const applicationSubscribers = await succeed(
      portalApplicationsAsAppUser.getSubscriberApisByApplicationIdRaw({
        applicationId: createdApplication.id,
      }),
    );

    expect(applicationSubscribers).toBeTruthy();
    expect(applicationSubscribers.data).toHaveLength(1);
    expect(applicationSubscribers.data[0]._links).toBeTruthy();
    expect(applicationSubscribers.data[0]._links.picture).toBeTruthy();
  });

  afterAll(async () => {
    await subscriptionAsAppUser.closeSubscription({
      subscriptionId: createdSubscription.id,
    });

    await applicationManagementApiAsAppUser.deleteApplicationRaw({
      orgId,
      envId,
      application: createdApplication.id,
    });

    await apiPlansAsApiUser.closeApiPlanRaw({
      orgId,
      envId,
      api: createdApi.id,
      plan: createdPlan.id,
    });

    await apisAsUser.deleteApiRaw({ orgId, envId, api: createdApi.id });
  });
});
