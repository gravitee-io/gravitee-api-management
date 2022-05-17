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
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';

import { forManagementAsAdminUser, forPortalAsAdminUser, forPortalAsSimpleUser } from '@client-conf/*';
import { APIsApi } from '@management-apis/APIsApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { fail } from '../../lib/jest-utils';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { Visibility } from '@management-models/Visibility';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { PlanEntity } from '@management-models/PlanEntity';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { SubscriptionApi as PortalSubscriptionApi } from '@portal-apis/SubscriptionApi';
import { APISubscriptionsApi } from '@management-apis/APISubscriptionsApi';
import { Subscription } from '@management-models/Subscription';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { ApiKeyEntity } from '@management-models/ApiKeyEntity';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanValidationType } from '@management-models/PlanValidationType';
import { SubscriptionStatus } from '@management-models/SubscriptionStatus';
import faker from '@faker-js/faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

// Portal resources
const portalSubscriptionApiAsAdmin = new PortalSubscriptionApi(forPortalAsAdminUser());
const portalSubscriptionApiAsSimpleUser = new PortalSubscriptionApi(forPortalAsSimpleUser());

// Management resources
const apisAsAdminUser = new APIsApi(forManagementAsAdminUser());
const apiPlansAsAdminUser = new APIPlansApi(forManagementAsAdminUser());
const applicationApiAsAdminUser = new ApplicationsApi(forManagementAsAdminUser());
const apiSubscriptionsAsAdminUser = new APISubscriptionsApi(forManagementAsAdminUser());
const appSubscriptionsAsAdminUser = new ApplicationSubscriptionsApi(forManagementAsAdminUser());

describe('Portal: Business Error - subscriptions', () => {
  let createdApi: ApiEntity;
  let plan1, plan2: PlanEntity;
  let createdApplication: ApplicationEntity;
  let subscription1, subscription2: Subscription;
  let subscription1Keys: ApiKeyEntity[];

  beforeAll(async () => {
    // create an api
    createdApi = await apisAsAdminUser.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi(),
    });

    // publish an api
    await apisAsAdminUser.updateApi({
      api: createdApi.id,
      updateApiEntity: ApisFaker.updateApiFromApiEntity(createdApi, {
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        visibility: Visibility.PUBLIC,
      }),
      orgId,
      envId,
    });

    // create the api plan
    plan1 = await apiPlansAsAdminUser.createApiPlan({
      envId,
      orgId,
      api: createdApi.id,
      newPlanEntity: PlansFaker.newPlan({ security: PlanSecurityType.APIKEY, validation: PlanValidationType.AUTO }),
    });

    // publish the api plan
    await apiPlansAsAdminUser.publishApiPlanRaw({ envId, orgId, plan: plan1.id, api: createdApi.id });

    // create application for subscription
    createdApplication = await applicationApiAsAdminUser.createApplication({
      orgId,
      envId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });

    // create first subscription
    subscription1 = await apiSubscriptionsAsAdminUser.createSubscriptionToApi({
      orgId,
      envId,
      api: createdApi.id,
      plan: plan1.id,
      application: createdApplication.id,
    });

    // get the subscription 1 api keys
    subscription1Keys = await appSubscriptionsAsAdminUser.getApiKeysForApplicationSubscription({
      orgId,
      envId,
      application: createdApplication.id,
      subscription: subscription1.id,
    });
    expect(subscription1Keys.length).toBeGreaterThanOrEqual(1);
  });

  describe('400', () => {
    test('should check input value', async () => {
      await fail(portalSubscriptionApiAsAdmin.createSubscriptionRaw({ subscriptionInput: undefined }), 400, {
        code: 'createSubscription.arg0',
        message: 'Input must not be null.',
      });
    });

    describe('keyId parameter does not correspond to the subscription', () => {
      beforeAll(async () => {
        // create a second api plan
        plan2 = await apiPlansAsAdminUser.createApiPlan({
          envId,
          orgId,
          api: createdApi.id,
          newPlanEntity: PlansFaker.newPlan({ security: PlanSecurityType.APIKEY, validation: PlanValidationType.AUTO }),
        });

        // publish the api plan
        await apiPlansAsAdminUser.publishApiPlanRaw({ envId, orgId, plan: plan2.id, api: createdApi.id });

        subscription2 = await apiSubscriptionsAsAdminUser.createSubscriptionToApi({
          orgId,
          envId,
          api: createdApi.id,
          plan: plan2.id,
          application: createdApplication.id,
        });
      });

      test('should not have Etag header when we revoke the subscription', async () => {
        await fail(
          portalSubscriptionApiAsAdmin.revokeKeySubscriptionRaw({
            subscriptionId: subscription2.id,
            apiKey: subscription1Keys[0].key,
          }),
          400,
        );
      });
    });
  });

  describe('403', () => {
    test('should not get subscriptions using application id', async () => {
      await fail(
        portalSubscriptionApiAsSimpleUser.getSubscriptionsRaw({
          applicationId: createdApplication.id,
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
        },
      );
    });

    test('should not get subscriptions using application id and api id', async () => {
      await fail(
        portalSubscriptionApiAsSimpleUser.getSubscriptionsRaw({
          applicationId: createdApplication.id,
          apiId: createdApi.id,
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
        },
      );
    });

    test('should not be able to create a new subscription', async () => {
      await fail(
        portalSubscriptionApiAsSimpleUser.createSubscriptionRaw({
          subscriptionInput: {
            application: createdApplication.id,
            plan: plan1.id,
            request: faker.lorem.sentence(10),
          },
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
        },
      );
    });

    test('should not be able to get the created subscription', async () => {
      await fail(
        portalSubscriptionApiAsSimpleUser.getSubscriptionByIdRaw({
          subscriptionId: subscription1.id,
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
        },
      );
    });

    test('should not be able to close the created subscription', async () => {
      await fail(
        portalSubscriptionApiAsSimpleUser.closeSubscription({
          subscriptionId: subscription1.id,
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
        },
      );
    });

    test('should not be able to renew the subscription keys', async () => {
      await fail(
        portalSubscriptionApiAsSimpleUser.renewKeySubscription({
          subscriptionId: subscription1.id,
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
        },
      );
    });

    test('should not be able to revoke the subscription keys', async () => {
      await fail(
        portalSubscriptionApiAsSimpleUser.revokeKeySubscriptionRaw({
          apiKey: subscription1Keys[0].key,
          subscriptionId: subscription1.id,
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
        },
      );
    });
  });

  describe('404', () => {
    test('should not find the subscription', async () => {
      await fail(
        portalSubscriptionApiAsAdmin.getSubscriptionByIdRaw({
          subscriptionId: 'fake-subscription-id',
        }),
        404,
      );
    });

    test('should not find the subscription when the user try to close it', async () => {
      await fail(
        portalSubscriptionApiAsAdmin.closeSubscriptionRaw({
          subscriptionId: 'fake-subscription-id',
        }),
        404,
      );
    });

    test('should not find the subscription when the user try to renew keys', async () => {
      await fail(
        portalSubscriptionApiAsAdmin.renewKeySubscriptionRaw({
          subscriptionId: 'fake-subscription-id',
        }),
        404,
      );
    });

    test('should not find the subscription when the user try to revoke keys', async () => {
      await fail(
        portalSubscriptionApiAsAdmin.revokeKeySubscriptionRaw({
          apiKey: subscription1Keys[0].key,
          subscriptionId: 'fake-subscription-id',
        }),
        404,
      );
    });
  });

  afterAll(async () => {
    const deletedSubscriptions = [subscription1, subscription2].map(
      async (subscription) =>
        await apiSubscriptionsAsAdminUser.changeApiSubscriptionStatus({
          envId,
          orgId,
          api: createdApi.id,
          subscription: subscription.id,
          status: SubscriptionStatus.CLOSED,
        }),
    );
    await Promise.all(deletedSubscriptions);

    await applicationApiAsAdminUser.deleteApplication({
      orgId,
      envId,
      application: createdApplication.id,
    });

    const deletedPlans = [plan1, plan2].map(
      async (plan) =>
        await apiPlansAsAdminUser.closeApiPlan({
          orgId,
          envId,
          api: createdApi.id,
          plan: plan.id,
        }),
    );
    await Promise.all(deletedPlans);

    await apisAsAdminUser.deleteApi({ orgId, envId, api: createdApi.id });
  });
});
