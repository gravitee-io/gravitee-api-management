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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { Application } from '@gravitee/portal-webclient-sdk/src/lib/models/Application';
import { PortalApplicationFaker } from '@gravitee/fixtures/portal/PortalApplicationFaker';
import { SubscriptionApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/SubscriptionApi';
import { forManagementAsApiUser, forPortalAsAppUser } from '@gravitee/utils/configuration';
import { ApplicationApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/ApplicationApi';
import {
  ApiEntityV4,
  LifecycleAction,
  PlanEntityV4,
  PlanSecurityTypeV4,
  PlanValidationTypeV4,
} from '@gravitee/management-webclient-sdk/src/lib/models';
import { PlansV4Faker } from '@gravitee/fixtures/management/PlansV4Faker';
import { ApisV4Faker } from '@gravitee/fixtures/management/ApisV4Faker';
import { V4APIPlansApi } from '@gravitee/management-webclient-sdk/src/lib/apis/V4APIPlansApi';
import { V4APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/V4APIsApi';
import { Subscription, SubscriptionStatusEnum } from '@gravitee/portal-webclient-sdk/src/lib';
import { APISubscriptionsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APISubscriptionsApi';
import { teardownV4ApisAndApplications } from '@gravitee/utils/management';
import { verifyWiremockRequest } from '@gravitee/utils/wiremock';
import faker from '@faker-js/faker';
import { sleep } from '@gravitee/utils/gateway';
import { describeIfJupiter } from '@lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new V4APIsApi(forManagementAsApiUser());
const apiPlansResource = new V4APIPlansApi(forManagementAsApiUser());
const apiSubscriptionResource = new APISubscriptionsApi(forManagementAsApiUser());
const portalApplicationResource = new ApplicationApi(forPortalAsAppUser());
const portalSubscriptionResource = new SubscriptionApi(forPortalAsAppUser());

// TODO: describeIfJupiter.skip('V4 subscription plan subscription and approval workflow', () => {
describe.skip('V4 subscription plan subscription and approval workflow', () => {
  let api: ApiEntityV4;
  let plan: PlanEntityV4;
  let application: Application;
  let subscription: Subscription;

  const setupApiAndPlan = async function (planValidation: PlanValidationTypeV4) {
    // create a V4 API with a webhook entrypoint and a mock endpoint
    api = await apisResource.createApi1({
      orgId,
      envId,
      newApiEntityV4: ApisV4Faker.newApi({
        listeners: [
          ApisV4Faker.newSubscriptionListener({
            entrypoints: [
              {
                type: 'webhook',
              },
            ],
          }),
        ],
        endpointGroups: [
          {
            name: 'default-group',
            type: 'mock',
            endpoints: [
              {
                name: 'default',
                type: 'mock',
                configuration: {
                  messageInterval: 0,
                },
              },
            ],
          },
        ],
      }),
    });

    // create a subscription plan
    plan = await apisResource.createApiPlan1({
      envId,
      orgId,
      api: api.id,
      newPlanEntityV4: PlansV4Faker.newPlan({
        validation: planValidation,
        security: { type: PlanSecurityTypeV4.SUBSCRIPTION },
      }),
    });

    // publish the plan
    await apiPlansResource.publishApiPlan1({
      envId,
      orgId,
      api: api.id,
      plan: plan.id,
    });

    // start the API
    await apisResource.doApiLifecycleAction1({
      envId,
      orgId,
      api: api.id,
      action: LifecycleAction.START,
    });

    // create an application from portal
    application = await portalApplicationResource.createApplication({
      applicationInput: PortalApplicationFaker.newApplicationInput(),
    });
  };

  describe('Subscribe to manually validated subscription plan', () => {
    let callbackUrl = `/${faker.random.word()}`;

    beforeAll(async () => {
      await setupApiAndPlan(PlanValidationTypeV4.MANUAL);
    });

    describe('Consumer subscribes to API from portal', () => {
      test('Consumer should subscribe', async () => {
        subscription = await portalSubscriptionResource.createSubscription({
          subscriptionInput: {
            application: application.id,
            plan: plan.id,
            configuration: {
              entrypointId: 'webhook',
              entrypointConfiguration: {
                callbackUrl: `${process.env.WIREMOCK_BASE_URL}${callbackUrl}`,
              },
            },
          },
        });
      });

      test('Subscription should be in PENDING status', async () => {
        subscription = await portalSubscriptionResource.getSubscriptionById({ subscriptionId: subscription.id });
        expect(subscription.status).toBe(SubscriptionStatusEnum.PENDING);
      });

      test('Should not send any message to entrypoint', async () => {
        await sleep(500);
        const { count: webhookRequestCount } = await verifyWiremockRequest(callbackUrl, 'POST').then((res) => res.json());
        expect(webhookRequestCount).toBe(0);
      });
    });

    describe('Publisher approves subscription', () => {
      test('Publisher should approve subscription', async () => {
        await apiSubscriptionResource.processApiSubscription({
          envId,
          orgId,
          subscription: subscription.id,
          api: api.id,
          processSubscriptionEntity: { accepted: true },
        });
      });

      test('Subscription should be in ACCEPTED status', async () => {
        subscription = await portalSubscriptionResource.getSubscriptionById({ subscriptionId: subscription.id });
        expect(subscription.status).toBe(SubscriptionStatusEnum.ACCEPTED);
      });

      test('Should send messages to entrypoint', async () => {
        await sleep(1000);
        const { count: webhookRequestCount } = await verifyWiremockRequest(callbackUrl, 'POST').then((res) => res.json());
        expect(webhookRequestCount).toBeGreaterThan(0);
      });
    });

    describe('Consumer updates subscription configuration', () => {
      test('Consumer should update subscription configuration', async () => {
        callbackUrl = `/${faker.random.word()}`;

        await portalSubscriptionResource.updateSubscription({
          subscriptionId: subscription.id,
          updateSubscriptionInput: {
            configuration: {
              entrypointId: 'webhook',
              entrypointConfiguration: {
                callbackUrl: `${process.env.WIREMOCK_BASE_URL}${callbackUrl}`,
              },
            },
          },
        });
      });

      test('Subscription should be in PENDING status', async () => {
        subscription = await portalSubscriptionResource.getSubscriptionById({ subscriptionId: subscription.id });
        expect(subscription.status).toBe(SubscriptionStatusEnum.PENDING);
      });

      test('Should not send any message to entrypoint', async () => {
        await sleep(500);
        const { count: webhookRequestCount } = await verifyWiremockRequest(callbackUrl, 'POST').then((res) => res.json());
        expect(webhookRequestCount).toBe(0);
      });
    });

    describe('Publisher rejects subscription', () => {
      test('Publisher should reject subscription', async () => {
        await apiSubscriptionResource.processApiSubscription({
          envId,
          orgId,
          subscription: subscription.id,
          api: api.id,
          processSubscriptionEntity: { accepted: false },
        });
      });

      test('Subscription should be in REJECTED status', async () => {
        subscription = await portalSubscriptionResource.getSubscriptionById({ subscriptionId: subscription.id });
        expect(subscription.status).toBe(SubscriptionStatusEnum.REJECTED);
      });

      test('Should not send any message to entrypoint', async () => {
        await sleep(500);
        const { count: webhookRequestCount } = await verifyWiremockRequest(callbackUrl, 'POST').then((res) => res.json());
        expect(webhookRequestCount).toBe(0);
      });
    });

    afterAll(async () => {
      await teardownV4ApisAndApplications(orgId, envId, [api.id], [application.id]);
    });
  });

  describe('Subscribe to automatically validated subscription plan', () => {
    let callbackUrl = `/${faker.random.word()}`;

    beforeAll(async () => {
      await setupApiAndPlan(PlanValidationTypeV4.AUTO);
    });

    describe('Consumer subscribes to API from portal', () => {
      test('Consumer should subscribe', async () => {
        subscription = await portalSubscriptionResource.createSubscription({
          subscriptionInput: {
            application: application.id,
            plan: plan.id,
            configuration: {
              entrypointId: 'webhook',
              entrypointConfiguration: {
                callbackUrl: `${process.env.WIREMOCK_BASE_URL}${callbackUrl}`,
              },
            },
          },
        });
      });

      test('Subscription should be in ACCEPTED status', async () => {
        subscription = await portalSubscriptionResource.getSubscriptionById({ subscriptionId: subscription.id });
        expect(subscription.status).toBe(SubscriptionStatusEnum.ACCEPTED);
      });
    });

    describe('Consumer updates subscription configuration', () => {
      test('Consumer should update subscription configuration', async () => {
        await portalSubscriptionResource.updateSubscription({
          subscriptionId: subscription.id,
          updateSubscriptionInput: {
            configuration: {
              entrypointId: 'webhook',
              entrypointConfiguration: {
                callbackUrl: `${process.env.WIREMOCK_BASE_URL}${callbackUrl}`,
              },
            },
          },
        });
      });

      test('Subscription should be in ACCEPTED status', async () => {
        subscription = await portalSubscriptionResource.getSubscriptionById({ subscriptionId: subscription.id });
        expect(subscription.status).toBe(SubscriptionStatusEnum.ACCEPTED);
      });
    });

    afterAll(async () => {
      await teardownV4ApisAndApplications(orgId, envId, [api.id], [application.id]);
    });
  });
});
