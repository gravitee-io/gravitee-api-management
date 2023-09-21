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
import { forManagementV2AsApiUser, forPortalAsAppUser } from '@gravitee/utils/configuration';
import { ApplicationApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/ApplicationApi';
import { PlanValidationType } from '@gravitee/management-webclient-sdk/src/lib/models';
import { MAPIV2PlansFaker } from '../../../../../../lib/fixtures/management/MAPIV2PlansFaker';
import { MAPIV2ApisFaker } from '../../../../../../lib/fixtures/management/MAPIV2ApisFaker';
import { Subscription, SubscriptionStatusEnum } from '@gravitee/portal-webclient-sdk/src/lib';
import { APISubscriptionsApi } from '@gravitee/management-v2-webclient-sdk/src/lib/apis/APISubscriptionsApi';
import { teardownV4ApisAndApplications } from '@gravitee/utils/management';
import { verifyWiremockRequest } from '@gravitee/utils/wiremock';
import faker from '@faker-js/faker';
import { sleep } from '@gravitee/utils/apim-http';
import { describeIfV4EmulationEngine } from '@lib/jest-utils';
import { Api, APIPlansApi, APIsApi, Plan, PlanMode, PlanValidation } from '@gravitee/management-v2-webclient-sdk/src/lib';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementV2AsApiUser());
const apiPlansResource = new APIPlansApi(forManagementV2AsApiUser());
const apiSubscriptionResource = new APISubscriptionsApi(forManagementV2AsApiUser());
const portalApplicationResource = new ApplicationApi(forPortalAsAppUser());
const portalSubscriptionResource = new SubscriptionApi(forPortalAsAppUser());

describeIfV4EmulationEngine('V4 subscription plan subscription and approval workflow', () => {
  let api: Api;
  let plan: Plan;
  let application: Application;
  let subscription: Subscription;

  const setupApiAndPlan = async function (planValidation: PlanValidation) {
    // create a V4 API with a webhook entrypoint and a mock endpoint
    api = await apisResource.createApi({
      envId,
      createApiV4: MAPIV2ApisFaker.newApi({
        listeners: [
          MAPIV2ApisFaker.newSubscriptionListener({
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
    plan = await apiPlansResource.createApiPlan({
      envId,
      apiId: api.id,
      createPlan: MAPIV2PlansFaker.newPlanV4({
        validation: planValidation,
        mode: PlanMode.PUSH,
        security: null,
      }),
    });

    // publish the plan
    await apiPlansResource.publishApiPlan({
      envId,
      apiId: api.id,
      planId: plan.id,
    });

    // start the API
    await apisResource.startApi({
      envId,
      apiId: api.id,
    });

    // create an application from portal
    application = await portalApplicationResource.createApplication({
      applicationInput: PortalApplicationFaker.newApplicationInput(),
    });
  };

  describe('Subscribe to manually validated subscription plan', () => {
    let callbackUrl = `/${faker.random.word()}`;

    beforeAll(async () => {
      await setupApiAndPlan(PlanValidationType.MANUAL);
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
        await apiSubscriptionResource.acceptApiSubscription({
          envId,
          apiId: api.id,
          subscriptionId: subscription.id,
          acceptSubscription: {},
        });
      });

      test('Subscription should be in ACCEPTED status', async () => {
        subscription = await portalSubscriptionResource.getSubscriptionById({ subscriptionId: subscription.id });
        expect(subscription.status).toBe(SubscriptionStatusEnum.ACCEPTED);
      });

      test('Should send messages to entrypoint', async () => {
        await sleep(5000);
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
        await apiSubscriptionResource.rejectApiSubscription({
          envId,
          apiId: api.id,
          subscriptionId: subscription.id,
          rejectSubscription: {},
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
      await setupApiAndPlan(PlanValidationType.AUTO);
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
