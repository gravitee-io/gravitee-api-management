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
import { describeIfClientGatewaySupportingApiProduct } from '@lib/jest-utils';
import 'dotenv/config';
import fetch from 'node-fetch';
import { ApiType, ApiV4, HttpListener, PlanMode, PlanSecurityType } from '../../../../lib/management-v2-webclient-sdk/src/lib';
import { adminAuthHeader, envId, managementV2BaseUrl, orgId } from '@gravitee/utils/api-products';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { faker } from '@faker-js/faker';

const managementBaseUrl = process.env.MANAGEMENT_BASE_URL;

if (!managementBaseUrl) {
  throw new Error('MANAGEMENT_BASE_URL must be defined to run API Product subscription e2e tests');
}

/**
 * All Management V2 API Product requests use this (admin).
 * Create requires ENVIRONMENT_API_PRODUCT[CREATE]; ensure admin has that role.
 */
const v2ApiProductsResourceAsAdmin = {
  baseUrl: managementV2BaseUrl,
  authHeader: adminAuthHeader,
  envId,
};

interface ApiProduct {
  id: string;
  environmentId: string;
  name: string;
  version: string;
}

interface ApiProductPlan {
  id: string;
  apiId: string;
  name: string;
  status: string;
  validation?: string;
}

interface Application {
  id: string;
  name: string;
}

interface Subscription {
  id: string;
  status: string;
}

describeIfClientGatewaySupportingApiProduct('API Product subscriptions (management v2)', () => {
  let api: ApiV4;
  let apiProduct: ApiProduct;
  let autoPlan: ApiProductPlan;
  let manualPlan: ApiProductPlan;
  let application: Application;
  let subscriptionAuto: Subscription;
  let subscriptionManual: Subscription;

  beforeAll(async () => {
    const uniqueSuffix = Date.now();

    //
    // 0. Create a V4 PROXY API allowed in API Products (HTTP listener must have entrypoints for start/deploy)
    //
    const path = `/product-subscriptions-${faker.string.alphanumeric(8).toLowerCase()}`;
    const apiImport = MAPIV2ApisFaker.apiImportV4({
      api: MAPIV2ApisFaker.apiV4Proxy({
        type: ApiType.PROXY,
        listeners: [
          {
            type: 'HTTP',
            paths: [{ path }],
            entrypoints: [{ type: 'http-proxy' }],
          },
        ],
        endpointGroups: [
          {
            name: 'default-group',
            type: 'http-proxy',
            endpoints: [
              {
                name: 'default',
                type: 'http-proxy',
                configuration: {
                  target: `${process.env.WIREMOCK_BASE_URL}/hello`,
                },
              },
            ],
          },
        ],
        allowedInApiProducts: true,
      }),
    });

    // Create API via Management V2 with same admin credentials as API Product (avoids 403 on product create)
    const importResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/apis/_import/definition`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(apiImport),
    });
    expect(importResponse.status).toEqual(201);
    api = (await importResponse.json()) as ApiV4;
    expect(api.id).toBeDefined();

    // Create application (management v1)
    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-app-api-product-${uniqueSuffix}`,
        description: 'E2E application for API Product subscription tests',
        type: 'SIMPLE',
      }),
    });
    expect(appResponse.status).toEqual(201);
    application = (await appResponse.json()) as Application;
    expect(application.id).toBeDefined();

    // Create API Product (management v2) and attach the V4 API — requires ENVIRONMENT_API_PRODUCT[CREATE]
    const productResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: `e2e-product-subscriptions-${uniqueSuffix}`,
          description: 'E2E API Product for subscription tests',
          version: '1.0.0',
          apiIds: [api.id],
        }),
      },
    );
    expect(productResponse.status).toEqual(201);
    apiProduct = (await productResponse.json()) as ApiProduct;
    expect(apiProduct.id).toBeDefined();

    // Create AUTO validation plan (API Product plans do not allow KEY_LESS; use API_KEY and full plan payload)
    const autoPlanPayload = MAPIV2PlansFaker.newPlanV4({
      name: `e2e-product-plan-auto-${uniqueSuffix}`,
      description: 'AUTO validation plan for product subscription tests',
      order: 1,
      mode: PlanMode.STANDARD,
      security: { type: PlanSecurityType.API_KEY },
      validation: 'AUTO',
    });
    const autoPlanResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/plans`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(autoPlanPayload),
      },
    );
    expect(autoPlanResponse.status).toEqual(201);
    autoPlan = (await autoPlanResponse.json()) as ApiProductPlan;
    expect(autoPlan.id).toBeDefined();

    // Create MANUAL validation plan (API Product plans do not allow KEY_LESS; use API_KEY and full plan payload)
    const manualPlanPayload = MAPIV2PlansFaker.newPlanV4({
      name: `e2e-product-plan-manual-${uniqueSuffix}`,
      description: 'MANUAL validation plan for product subscription tests',
      order: 2,
      mode: PlanMode.STANDARD,
      security: { type: PlanSecurityType.API_KEY },
      validation: 'MANUAL',
    });
    const manualPlanResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/plans`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(manualPlanPayload),
      },
    );
    expect(manualPlanResponse.status).toEqual(201);
    manualPlan = (await manualPlanResponse.json()) as ApiProductPlan;
    expect(manualPlan.id).toBeDefined();

    // Publish both plans so they can be subscribed
    const publishAutoResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/plans/${autoPlan.id}/_publish`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
        },
      },
    );
    expect(publishAutoResponse.status).toEqual(200);
    autoPlan = (await publishAutoResponse.json()) as ApiProductPlan;
    expect(autoPlan.status).toEqual('PUBLISHED');

    const publishManualResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/plans/${manualPlan.id}/_publish`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
        },
      },
    );
    expect(publishManualResponse.status).toEqual(200);
    manualPlan = (await publishManualResponse.json()) as ApiProductPlan;
    expect(manualPlan.status).toEqual('PUBLISHED');
  });

  /**
   * Scenario A — Create subscription (AUTO)
   * AUTO validation should directly result in an ACCEPTED / ACTIVE subscription.
   */
  test('should create subscription with AUTO validation plan', async () => {
    const response = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/subscriptions`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationId: application.id,
          planId: autoPlan.id,
        }),
      },
    );

    expect(response.status).toEqual(201);
    subscriptionAuto = (await response.json()) as Subscription;
    expect(subscriptionAuto.id).toBeDefined();
    expect(subscriptionAuto.status).not.toEqual('PENDING');
  });

  /**
   * Scenario B — Manual approval flow
   */
  test('should create subscription with MANUAL validation plan', async () => {
    const createResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/subscriptions`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationId: application.id,
          planId: manualPlan.id,
        }),
      },
    );

    expect(createResponse.status).toEqual(201);
    subscriptionManual = (await createResponse.json()) as Subscription;
    expect(subscriptionManual.id).toBeDefined();
    expect(subscriptionManual.status).toEqual('ACCEPTED');
  });

  /**
   * Scenario C — Reject subscription
   * TODO - implement rejection and verify subscription status changes to REJECTED and keys are not generated
   */

  /**
   * Scenario D — Re-subscribe after rejection / closure
   * Ensures that after a subscription is closed, a new one can be created.
   */
  test('should allow re-subscribe after subscription is closed', async () => {
    // Use a dedicated application for this scenario to ensure we control the full lifecycle
    const resubAppResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-app-api-product-resub-${Date.now()}`,
        description: 'E2E application for API Product re-subscribe test',
        type: 'SIMPLE',
      }),
    });
    expect(resubAppResponse.status).toEqual(201);
    const resubApp = (await resubAppResponse.json()) as Application;

    // Create & accept subscription
    const createResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/subscriptions`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationId: resubApp.id,
          planId: manualPlan.id,
        }),
      },
    );
    expect(createResponse.status).toEqual(201);
    const created = (await createResponse.json()) as Subscription;

    // Close subscription
    const closeResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/subscriptions/${created.id}/_close`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
        },
      },
    );
    expect(closeResponse.status).toEqual(200);
    const closed = (await closeResponse.json()) as Subscription;
    expect(closed.status).toEqual('CLOSED');

    // Re-subscribe with same application & plan
    const resubscribeResponse = await fetch(
      `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}/subscriptions`,
      {
        method: 'POST',
        headers: {
          Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationId: resubApp.id,
          planId: manualPlan.id,
        }),
      },
    );
    expect(resubscribeResponse.status).toEqual(201);
    const resubscribed = (await resubscribeResponse.json()) as Subscription;
    expect(resubscribed.id).not.toEqual(created.id);

    // Clean up scenario-specific application
    await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications/${resubApp.id}`, {
      method: 'DELETE',
      headers: {
        Authorization: adminAuthHeader,
      },
    });
  });

  afterAll(async () => {
    if (application?.id) {
      await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications/${application.id}`, {
        method: 'DELETE',
        headers: {
          Authorization: adminAuthHeader,
        },
      });
    }

    if (apiProduct?.id) {
      await fetch(
        `${v2ApiProductsResourceAsAdmin.baseUrl}/environments/${v2ApiProductsResourceAsAdmin.envId}/api-products/${apiProduct.id}`,
        {
          method: 'DELETE',
          headers: {
            Authorization: v2ApiProductsResourceAsAdmin.authHeader,
          },
        },
      );
    }
  });
});
