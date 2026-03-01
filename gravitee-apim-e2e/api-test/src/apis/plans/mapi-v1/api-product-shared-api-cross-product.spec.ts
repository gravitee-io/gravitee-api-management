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
import 'dotenv/config';
import fetch from 'node-fetch';
import { APIsApi, ApiType, ApiV4, HttpListener, PlanMode, PlanSecurityType } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { fetchGatewaySuccess } from '@gravitee/utils/apim-http';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';
const orgId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

/**
 * Scenario:
 * - One V4 PROXY API is attached to both Product-A and Product-B.
 * - Each product has its own API_KEY plan, application, subscription and key.
 * - Phase 1 (before detach): both keyA and keyB return 200 on the shared API's
 *   context path.
 * - The API is removed from Product-A via PUT (apiIds set to []) and Product-A
 *   is redeployed so the gateway picks up the change.
 * - Phase 2 (after detach): keyA returns 401/403; keyB remains 200.
 */
describe('API Product - same API shared across two products: detaching from one does not affect the other', () => {
  let sharedApi: ApiV4;
  let contextPath: string;

  // Product-A state
  let productAId: string;
  let keyA: string;

  // Product-B state
  let productBId: string;
  let keyB: string;

  let adminAuthHeader: string;
  let managementV2BaseUrl: string;
  let managementBaseUrl: string;

  // ─── Helper: create + publish a plan on a product, subscribe one app, return the API key ──

  const createPlanSubscriptionAndKey = async (productId: string, label: string): Promise<string> => {
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-shared-plan-${label}-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    const planId = planBody.id;

    const publishResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });
    expect(publishResponse.status).toEqual(200);

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-shared-app-${label}-${Date.now()}`,
        description: `Application for shared API test — ${label}`,
        type: 'SIMPLE',
      }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();

    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ applicationId: appBody.id, planId }),
    });
    expect(subscriptionResponse.status).toEqual(201);
    const subscriptionBody = await subscriptionResponse.json();

    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionBody.id}/api-keys`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    const key: string = apiKeysBody.data[0].key;
    expect(key).toBeDefined();
    return key;
  };

  // ─── Helper: deploy a product ─────────────────────────────────────────────

  const deployProduct = async (productId: string): Promise<void> => {
    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
  };

  beforeAll(async () => {
    managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL ?? '';
    managementBaseUrl = process.env.MANAGEMENT_BASE_URL ?? '';
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl || !managementBaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL and MANAGEMENT_BASE_URL must be defined for this test');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for this test');
    }

    adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    //
    // 1. Create one shared V4 PROXY API allowed in API Products.
    //
    const path = `/product-shared-${faker.string.alphanumeric(8).toLowerCase()}`;
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
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=shared-api`,
                },
              },
            ],
          },
        ],
        allowedInApiProducts: true,
      }),
    });

    sharedApi = (await created(
      v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
        envId,
        exportApiV4: apiImport,
      }),
    )) as ApiV4;
    contextPath = (sharedApi.listeners[0] as HttpListener).paths[0].path;

    //
    // 2. Create Product-A and Product-B, both referencing the same API.
    //
    const createProduct = async (label: string): Promise<string> => {
      const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-shared-product-${label}-${Date.now()}`,
          description: `E2E shared API test — ${label}`,
          version: '1.0.0',
          apiIds: [sharedApi.id],
        }),
      });
      expect(response.status).toEqual(201);
      const body = await response.json();
      return body.id;
    };

    productAId = await createProduct('A');
    productBId = await createProduct('B');

    //
    // 3. Each product gets its own plan, application, subscription, and key.
    //
    keyA = await createPlanSubscriptionAndKey(productAId, 'A');
    keyB = await createPlanSubscriptionAndKey(productBId, 'B');

    //
    // 4. Start + deploy the shared API, then deploy both products.
    //
    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: sharedApi.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: sharedApi.id }), 202);

    await deployProduct(productAId);
    await deployProduct(productBId);
  });

  // ─── Phase 1: both keys must work before any detach ──────────────────────

  test('keyA (Product-A subscription) should return 200 on the shared API (before detach)', async () => {
    await fetchGatewaySuccess({
      contextPath,
      headers: { 'X-Gravitee-Api-Key': keyA },
    });
  });

  test('keyB (Product-B subscription) should return 200 on the shared API (before detach)', async () => {
    await fetchGatewaySuccess({
      contextPath,
      headers: { 'X-Gravitee-Api-Key': keyB },
    });
  });

  // ─── Phase 2: after removing the shared API from Product-A ───────────────

  describe('after removing the shared API from Product-A and redeploying', () => {
    beforeAll(async () => {
      //
      // Remove the shared API from Product-A by updating its membership to an
      // empty list, then redeploy Product-A so the gateway applies the change.
      //
      const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productAId}`, {
        method: 'PUT',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-shared-product-A-detached-${Date.now()}`,
          description: 'E2E shared API test — A (API detached)',
          version: '1.0.0',
          apiIds: [],
        }),
      });
      expect(updateResponse.status).toEqual(200);

      await deployProduct(productAId);

      // Allow the gateway time to apply the redeployment before asserting.
      await new Promise((resolve) => setTimeout(resolve, 6000));
    });

    test('keyA should return 401/403 after the shared API is removed from Product-A', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': keyA },
      } as any);

      expect([401, 403]).toContain(response.status);
    });

    test('keyB should still return 200 after the shared API is removed from Product-A only', async () => {
      await fetchGatewaySuccess({
        contextPath,
        headers: { 'X-Gravitee-Api-Key': keyB },
      });
    });
  });

  afterAll(async () => {
    for (const productId of [productAId, productBId]) {
      if (productId) {
        await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
          method: 'DELETE',
          headers: { Authorization: adminAuthHeader },
        });
      }
    }

    if (sharedApi?.id) {
      try {
        await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: sharedApi.id }), 204);
      } catch {
        // May already be stopped; continue to delete
      }
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: sharedApi.id, closePlans: true }));
      } catch {
        // Best-effort only; environment reset will clean up
      }
    }
  });
});
