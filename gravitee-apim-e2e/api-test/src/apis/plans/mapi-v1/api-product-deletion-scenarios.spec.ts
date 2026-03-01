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
 * Each scenario is fully independent with its own product, APIs, plan and key.
 *
 *   C1      : Delete the entire product → all attached API paths become inaccessible.
 *   C2.1    : Stop + delete an underlying API → its path returns 404; sibling API still 200.
 *   C2.2    : Detach API from product membership (API stays deployed) + explicit redeploy
 *             → detached path returns 401/404; sibling API still 200.
 *   C3      : Detach API from product mapping with no explicit product deploy call
 *             → tests auto-propagation: detached path must become inaccessible promptly.
 */

// ─── Shared proxy-API factory (module-level) ─────────────────────────────────

const makeProxyApi = async (slot: string): Promise<ApiV4> => {
  const path = `/product-del-${slot}-${faker.string.alphanumeric(6).toLowerCase()}`;
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
                target: '${process.env.WIREMOCK_BASE_URL}/hello?name=deletion-scenario',
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

// ─────────────────────────────────────────────────────────────────────────────
// C1 — Delete API Product with active plans/subscriptions
// ─────────────────────────────────────────────────────────────────────────────

describe('C1 - Delete API Product with active plans/subscriptions: all product keys become invalid', () => {
  let api1: ApiV4;
  let api2: ApiV4;
  let productId: string;
  let apiKey: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;

  beforeAll(async () => {
    managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL ?? '';
    const managementBaseUrl = process.env.MANAGEMENT_BASE_URL ?? '';
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl || !managementBaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL and MANAGEMENT_BASE_URL must be defined for this test');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for this test');
    }

    adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    api1 = await makeProxyApi('c1a');
    api2 = await makeProxyApi('c1b');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-c1-${Date.now()}`,
        description: 'C1: delete product test',
        version: '1.0.0',
        apiIds: [api1.id, api2.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-c1-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planBody.id}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: `e2e-c1-app-${Date.now()}`, description: 'C1 app', type: 'SIMPLE' }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();

    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ applicationId: appBody.id, planId: planBody.id }),
    });
    expect(subscriptionResponse.status).toEqual(201);
    const subscriptionBody = await subscriptionResponse.json();

    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionBody.id}/api-keys`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;
    expect(apiKey).toBeDefined();

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api1.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api1.id }), 202);
    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api2.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api2.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
  });

  test('baseline: api1 should return 200 with product key before product deletion', async () => {
    await fetchGatewaySuccess({
      contextPath: (api1.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  test('baseline: api2 should return 200 with product key before product deletion', async () => {
    await fetchGatewaySuccess({
      contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  describe('after deleting the API Product', () => {
    beforeAll(async () => {
      const deleteResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
      expect([200, 204]).toContain(deleteResponse.status);
      // Mark as deleted so afterAll skips its own cleanup attempt.
      productId = '';

      // Allow the gateway time to process the product deletion event.
      await new Promise((resolve) => setTimeout(resolve, 6000));
    });

    test('api1 should return 401/403/404 after the product is deleted', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${(api1.listeners[0] as HttpListener).paths[0].path}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey },
      } as any);
      expect([401, 403, 404]).toContain(response.status);
    });

    test('api2 should return 401/403/404 after the product is deleted', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${(api2.listeners[0] as HttpListener).paths[0].path}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey },
      } as any);
      expect([401, 403, 404]).toContain(response.status);
    });
  });

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }
    for (const api of [api1, api2]) {
      if (api?.id) {
        try {
          await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
        } catch {}
        try {
          await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
        } catch {}
      }
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// C2.1 — Remove underlying API (stop + delete): path returns 404
// ─────────────────────────────────────────────────────────────────────────────

describe('C2.1 - Stop and delete an underlying API: its path returns 404; sibling API remains 200', () => {
  let api1: ApiV4;
  let api2: ApiV4;
  let productId: string;
  let apiKey: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;

  beforeAll(async () => {
    managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL ?? '';
    const managementBaseUrl = process.env.MANAGEMENT_BASE_URL ?? '';
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl || !managementBaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL and MANAGEMENT_BASE_URL must be defined for this test');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for this test');
    }

    adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    api1 = await makeProxyApi('c21a');
    api2 = await makeProxyApi('c21b');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-c21-${Date.now()}`,
        description: 'C2.1: remove underlying API test',
        version: '1.0.0',
        apiIds: [api1.id, api2.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-c21-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planBody.id}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: `e2e-c21-app-${Date.now()}`, description: 'C2.1 app', type: 'SIMPLE' }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();

    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ applicationId: appBody.id, planId: planBody.id }),
    });
    expect(subscriptionResponse.status).toEqual(201);
    const subscriptionBody = await subscriptionResponse.json();

    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionBody.id}/api-keys`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;
    expect(apiKey).toBeDefined();

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api1.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api1.id }), 202);
    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api2.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api2.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
  });

  test('baseline: api1 should return 200 before being removed from the gateway', async () => {
    await fetchGatewaySuccess({
      contextPath: (api1.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  test('baseline: api2 should return 200 before api1 removal', async () => {
    await fetchGatewaySuccess({
      contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  describe('after stopping and deleting the underlying api1', () => {
    beforeAll(async () => {
      // Stop api1 — triggers a gateway undeploy event.
      await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api1.id }), 204);
      // Delete the API permanently.
      await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api1.id, closePlans: true }));
      // Mark as deleted so afterAll skips a second delete attempt.
      api1 = { ...api1, id: '' };

      // Allow the gateway time to process the undeploy event.
      await new Promise((resolve) => setTimeout(resolve, 3000));
    });

    test('api1 path should return 404 after the underlying API is removed from the gateway', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${(api1.listeners[0] as HttpListener).paths[0].path}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey },
      } as any);
      expect(response.status).toEqual(404);
    });

    test('api2 should still return 200 — removing api1 has no effect on the sibling API', async () => {
      await fetchGatewaySuccess({
        contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
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
    // api1 is already deleted during the test; only api2 needs cleanup.
    if (api2?.id) {
      try {
        await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api2.id }), 204);
      } catch {}
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api2.id, closePlans: true }));
      } catch {}
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// C2.2 — Detach API from product mapping (API runtime stays deployed)
// ─────────────────────────────────────────────────────────────────────────────

describe('C2.2 - Detach API from product scope while it remains deployed: detached path returns 401', () => {
  let api1: ApiV4;
  let api2: ApiV4;
  let productId: string;
  let apiKey: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;

  beforeAll(async () => {
    managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL ?? '';
    const managementBaseUrl = process.env.MANAGEMENT_BASE_URL ?? '';
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl || !managementBaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL and MANAGEMENT_BASE_URL must be defined for this test');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for this test');
    }

    adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    api1 = await makeProxyApi('c22a');
    api2 = await makeProxyApi('c22b');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-c22-${Date.now()}`,
        description: 'C2.2: detach API while deployed test',
        version: '1.0.0',
        apiIds: [api1.id, api2.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-c22-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planBody.id}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: `e2e-c22-app-${Date.now()}`, description: 'C2.2 app', type: 'SIMPLE' }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();

    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ applicationId: appBody.id, planId: planBody.id }),
    });
    expect(subscriptionResponse.status).toEqual(201);
    const subscriptionBody = await subscriptionResponse.json();

    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionBody.id}/api-keys`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;
    expect(apiKey).toBeDefined();

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api1.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api1.id }), 202);
    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api2.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api2.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
  });

  test('baseline: api1 should return 200 while still in the product scope', async () => {
    await fetchGatewaySuccess({
      contextPath: (api1.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  test('baseline: api2 should return 200 before any detach', async () => {
    await fetchGatewaySuccess({
      contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  describe('after detaching api1 from the product (api1 runtime remains deployed)', () => {
    beforeAll(async () => {
      // Remove api1 from the product via PUT; api1 is still running on the gateway.
      const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'PUT',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-product-c22-detached-${Date.now()}`,
          description: 'C2.2: api1 detached from product scope',
          version: '1.0.0',
          apiIds: [api2.id],
        }),
      });
      expect(updateResponse.status).toEqual(200);

      // Explicit product redeploy so the gateway applies the membership change.
      const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      });
      expect([200, 202]).toContain(deployResponse.status);

      // Allow the gateway time to process the redeployment before asserting.
      await new Promise((resolve) => setTimeout(resolve, 6000));
    });

    test('api1 should return 401/403/404 in the product scope after being detached', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${(api1.listeners[0] as HttpListener).paths[0].path}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey },
      } as any);
      // 401/403 when the product key is rejected; 404 when the route is gone.
      expect([401, 403, 404]).toContain(response.status);
    });

    test('api2 should still return 200 after api1 is detached from the product', async () => {
      await fetchGatewaySuccess({
        contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
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
    for (const api of [api1, api2]) {
      if (api?.id) {
        try {
          await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
        } catch {}
        try {
          await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
        } catch {}
      }
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// C3 — Detach mapping immediate stop (no explicit product deploy call)
// ─────────────────────────────────────────────────────────────────────────────

describe('C3 - Detach API from product mapping without explicit redeploy: access stops promptly', () => {
  let api1: ApiV4;
  let api2: ApiV4;
  let productId: string;
  let apiKey: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;

  beforeAll(async () => {
    managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL ?? '';
    const managementBaseUrl = process.env.MANAGEMENT_BASE_URL ?? '';
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl || !managementBaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL and MANAGEMENT_BASE_URL must be defined for this test');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for this test');
    }

    adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    api1 = await makeProxyApi('c3a');
    api2 = await makeProxyApi('c3b');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-c3-${Date.now()}`,
        description: 'C3: immediate detach test',
        version: '1.0.0',
        apiIds: [api1.id, api2.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-c3-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planBody.id}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: `e2e-c3-app-${Date.now()}`, description: 'C3 app', type: 'SIMPLE' }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();

    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ applicationId: appBody.id, planId: planBody.id }),
    });
    expect(subscriptionResponse.status).toEqual(201);
    const subscriptionBody = await subscriptionResponse.json();

    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionBody.id}/api-keys`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;
    expect(apiKey).toBeDefined();

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api1.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api1.id }), 202);
    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api2.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api2.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
  });

  test('baseline: api1 should return 200 before detach', async () => {
    await fetchGatewaySuccess({
      contextPath: (api1.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  test('baseline: api2 should return 200 before detach', async () => {
    await fetchGatewaySuccess({
      contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  describe('after detaching api1 from product mapping — no explicit product deploy call', () => {
    beforeAll(async () => {
      // Detach api1 via PUT; intentionally NO POST /deployments call after this.
      // The test validates that the mapping change propagates to the gateway
      // automatically without needing an explicit deployment trigger.
      const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'PUT',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-product-c3-detached-${Date.now()}`,
          description: 'C3: api1 detached — no explicit deploy',
          version: '1.0.0',
          apiIds: [api2.id],
        }),
      });
      expect(updateResponse.status).toEqual(200);

      // Short wait to allow the mapping change event to propagate to the gateway.
      await new Promise((resolve) => setTimeout(resolve, 3000));
    });

    test('api1 should be inaccessible via the product key after detach (no explicit deploy)', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${(api1.listeners[0] as HttpListener).paths[0].path}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey },
      } as any);
      expect([401, 403, 404]).toContain(response.status);
    });

    test('api2 should remain accessible after api1 is detached without explicit deploy', async () => {
      await fetchGatewaySuccess({
        contextPath: (api2.listeners[0] as HttpListener).paths[0].path,
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
    for (const api of [api1, api2]) {
      if (api?.id) {
        try {
          await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
        } catch {}
        try {
          await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
        } catch {}
      }
    }
  });
});
