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
 * - A product is created with a single API (api1), one API_KEY plan, one
 *   subscription and one key.
 * - Phase 1: the key is validated against api1 (200).
 * - The product is then updated via PUT to include api2 and api3.
 *   No explicit POST /deployments call is made for the product after the update.
 * - api2 and api3 are started and deployed individually.
 * - Phase 2: the same key is validated against all three context paths and
 *   each must return 200.
 */
describe('API Product - incremental growth from 1 API to 3 APIs with the same subscription key', () => {
  let api1: ApiV4;
  let api2: ApiV4;
  let api3: ApiV4;
  let productId: string;
  let apiKey: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;
  let managementBaseUrl: string;

  // Helper: creates a V4 PROXY API allowed in API Products.
  const makeApi = async (slot: string): Promise<ApiV4> => {
    const path = `/product-growth-${slot}-${faker.string.alphanumeric(6).toLowerCase()}`;
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
                  target: '${process.env.WIREMOCK_BASE_URL}/hello?name=increment-growth-api',
                },
              },
            ],
          },
        ],
        allowedInApiProducts: true,
      }),
    });

    return (await created(
      v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
        envId,
        exportApiV4: apiImport,
      }),
    )) as ApiV4;
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
    // 1. Create all three APIs upfront so their IDs are available for the
    //    growth phase without additional API calls inside tests.
    //
    api1 = await makeApi('1');
    api2 = await makeApi('2');
    api3 = await makeApi('3');

    //
    // 2. Create a product containing only api1.
    //
    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-growth-${Date.now()}`,
        description: 'E2E product incremental growth test',
        version: '1.0.0',
        apiIds: [api1.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    //
    // 3. Create and publish one API_KEY plan on the product.
    //
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-growth-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    const planId = planBody.id;

    const publishPlanResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_publish`,
      {
        method: 'POST',
        headers: { Authorization: adminAuthHeader },
      },
    );
    expect(publishPlanResponse.status).toEqual(200);

    //
    // 4. Create one application and subscribe it to the plan.
    //
    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-growth-app-${Date.now()}`,
        description: 'Application for incremental growth test',
        type: 'SIMPLE',
      }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();

    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ applicationId: appBody.id, planId }),
    });
    expect(subscriptionResponse.status).toEqual(201);
    const subscriptionBody = await subscriptionResponse.json();

    //
    // 5. Fetch the single API key that will be reused across all three APIs.
    //
    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionBody.id}/api-keys`,
      {
        method: 'GET',
        headers: { Authorization: adminAuthHeader },
      },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;
    expect(apiKey).toBeDefined();

    //
    // 6. Start + deploy api1 and do the initial product deployment.
    //
    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api1.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api1.id }), 202);

    const deployProductResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployProductResponse.status);
  });

  // ─── Phase 1: single-API baseline ────────────────────────────────────────

  test('api1 should return 200 with the subscription key (baseline — product contains 1 API)', async () => {
    await fetchGatewaySuccess({
      contextPath: (api1.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  // ─── Phase 2: after growing the product to 3 APIs ────────────────────────

  describe('after growing the product to 3 APIs (no explicit product deployment)', () => {
    beforeAll(async () => {
      //
      // Update the product membership to include api2 and api3.
      // Intentionally no POST /deployments call follows — the scenario tests
      // that the key continues to work as API membership is expanded.
      //
      const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'PUT',
        headers: {
          Authorization: adminAuthHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: `e2e-product-growth-updated-${Date.now()}`,
          description: 'E2E product incremental growth test — 3 APIs',
          version: '1.0.0',
          apiIds: [api1.id, api2.id, api3.id],
        }),
      });
      expect(updateResponse.status).toEqual(200);

      // Start and individually deploy api2 and api3.
      await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api2.id }), 204);
      await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api2.id }), 202);

      await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api3.id }), 204);
      await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api3.id }), 202);
    });

    test('api1 should still return 200 with the same key after product growth', async () => {
      await fetchGatewaySuccess({
        contextPath: (api1.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });

    test('api2 should return 200 with the same key after being added to the product', async () => {
      await fetchGatewaySuccess({
        contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });

    test('api3 should return 200 with the same key after being added to the product', async () => {
      await fetchGatewaySuccess({
        contextPath: (api3.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });
  });

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }

    for (const api of [api1, api2, api3]) {
      if (api?.id) {
        try {
          await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
        } catch {
          // API may already be stopped; continue to delete
        }
        try {
          await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
        } catch {
          // Best-effort only; environment reset will clean up
        }
      }
    }
  });
});
