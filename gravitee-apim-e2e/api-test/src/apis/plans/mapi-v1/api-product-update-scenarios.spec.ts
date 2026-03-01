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
 * All four scenarios share a single product and a single subscription key.
 * The product membership evolves through each nested describe:
 *
 *   Baseline : [apiA, apiB]
 *   B1 (Add) : [apiA, apiB] → [apiA, apiB, apiC]  — apiC newly returns 200
 *   B2 (Remove): [apiA, apiB, apiC] → [apiA, apiC]  — apiB returns 401
 *   B3 (Replace): [apiA, apiC] → [apiD]             — apiA/apiC return 401; apiD returns 200
 *   B4 (Metadata): [apiD] → [apiD] (name/desc only) — apiD still returns 200
 */
describe('API Product - update scenarios (B1–B4)', () => {
  // Four independent V4 APIs used across the scenarios
  let apiA: ApiV4;
  let apiB: ApiV4;
  let apiC: ApiV4;
  let apiD: ApiV4;

  let productId: string;
  let apiKey: string;

  let adminAuthHeader: string;
  let managementV2BaseUrl: string;
  let managementBaseUrl: string;

  // ─── Helpers ─────────────────────────────────────────────────────────────

  const makeApi = async (slot: string): Promise<ApiV4> => {
    const path = `/product-update-${slot}-${faker.string.alphanumeric(6).toLowerCase()}`;
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
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=update-scenario`,
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

  const startAndDeployApi = async (api: ApiV4): Promise<void> => {
    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api.id }), 202);
  };

  const updateProduct = async (apiIds: string[], nameSuffix: string): Promise<void> => {
    const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
      method: 'PUT',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-update-${nameSuffix}-${Date.now()}`,
        description: `E2E product update scenario — ${nameSuffix}`,
        version: '1.0.0',
        apiIds,
      }),
    });
    expect(response.status).toEqual(200);
  };

  const deployProduct = async (): Promise<void> => {
    const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(response.status);
  };

  const expectDenied = async (contextPath: string): Promise<void> => {
    const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
      method: 'GET',
      headers: { 'X-Gravitee-Api-Key': apiKey },
    } as any);
    // 401/403 when the key is rejected; 404 when the route is fully gone after detach.
    expect([401, 403, 404]).toContain(response.status);
  };

  // ─── Baseline setup ───────────────────────────────────────────────────────

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
    // 1. Create all four APIs upfront so starts/deploys can be batched.
    //
    apiA = await makeApi('a');
    apiB = await makeApi('b');
    apiC = await makeApi('c');
    apiD = await makeApi('d');

    //
    // 2. Baseline product contains apiA and apiB only.
    //
    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-update-baseline-${Date.now()}`,
        description: 'E2E product update scenarios — baseline',
        version: '1.0.0',
        apiIds: [apiA.id, apiB.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    //
    // 3. One plan, one application, one subscription → one key (reused across all scenarios).
    //
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-update-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();

    const publishResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planBody.id}/_publish`,
      { method: 'POST', headers: { Authorization: adminAuthHeader } },
    );
    expect(publishResponse.status).toEqual(200);

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-update-app-${Date.now()}`,
        description: 'Application for product update scenarios',
        type: 'SIMPLE',
      }),
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

    //
    // 4. Start + deploy only the APIs that are already in the product.
    //    apiC and apiD are not yet in any product with a published plan, so
    //    the platform would reject startApi with "no published plan". They will
    //    be started individually when added to the product in B1 / B3.
    //
    await startAndDeployApi(apiA);
    await startAndDeployApi(apiB);

    //
    // 5. Initial product deployment — baseline: [apiA, apiB].
    //
    await deployProduct();
  });

  // ─── Baseline verification ────────────────────────────────────────────────

  test('baseline: key should return 200 on apiA', async () => {
    await fetchGatewaySuccess({
      contextPath: (apiA.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  test('baseline: key should return 200 on apiB', async () => {
    await fetchGatewaySuccess({
      contextPath: (apiB.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
    });
  });

  // ─── B1 — Add API to existing product ────────────────────────────────────
  // [apiA, apiB] → [apiA, apiB, apiC]

  describe('B1 - Add API to existing product', () => {
    beforeAll(async () => {
      await updateProduct([apiA.id, apiB.id, apiC.id], 'B1');
      // apiC is now in a product with a published plan — safe to start it.
      await startAndDeployApi(apiC);
      await deployProduct();
    });

    test('apiC (newly added) should return 200 with the existing key', async () => {
      await fetchGatewaySuccess({
        contextPath: (apiC.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });

    test('apiA (retained) should still return 200 after adding apiC', async () => {
      await fetchGatewaySuccess({
        contextPath: (apiA.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });

    test('apiB (retained) should still return 200 after adding apiC', async () => {
      await fetchGatewaySuccess({
        contextPath: (apiB.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });
  });

  // ─── B2 — Remove one API from product ────────────────────────────────────
  // [apiA, apiB, apiC] → [apiA, apiC]  (apiB removed)

  describe('B2 - Remove one API from product', () => {
    beforeAll(async () => {
      await updateProduct([apiA.id, apiC.id], 'B2');
      await deployProduct();
      // Allow the gateway time to apply the removal before asserting 401.
      await new Promise((resolve) => setTimeout(resolve, 6000));
    });

    test('apiB (removed) should return 401/403 after being detached from the product', async () => {
      await expectDenied((apiB.listeners[0] as HttpListener).paths[0].path);
    });

    test('apiA (retained) should still return 200 after apiB removal', async () => {
      await fetchGatewaySuccess({
        contextPath: (apiA.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });

    test('apiC (retained) should still return 200 after apiB removal', async () => {
      await fetchGatewaySuccess({
        contextPath: (apiC.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });
  });

  // ─── B3 — Replace full apiIds set ────────────────────────────────────────
  // [apiA, apiC] → [apiD]  (full replacement)

  describe('B3 - Replace full apiIds set', () => {
    beforeAll(async () => {
      await updateProduct([apiD.id], 'B3');
      // apiD is now in the product with a published plan — safe to start it.
      await startAndDeployApi(apiD);
      await deployProduct();
      // Allow the gateway time to apply the full replacement before asserting 401.
      await new Promise((resolve) => setTimeout(resolve, 6000));
    });

    test('apiA (detached) should return 401/403 after full set replacement', async () => {
      await expectDenied((apiA.listeners[0] as HttpListener).paths[0].path);
    });

    test('apiC (detached) should return 401/403 after full set replacement', async () => {
      await expectDenied((apiC.listeners[0] as HttpListener).paths[0].path);
    });

    test('apiD (newly attached) should return 200 after full set replacement', async () => {
      await fetchGatewaySuccess({
        contextPath: (apiD.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });
  });

  // ─── B4 — Metadata-only update ───────────────────────────────────────────
  // [apiD] → [apiD]  (apiIds unchanged; only name/description/version updated)

  describe('B4 - Metadata-only update leaves gateway behaviour unchanged', () => {
    beforeAll(async () => {
      // Update only metadata; apiIds remains [apiD].
      const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'PUT',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-product-update-B4-metadata-${Date.now()}`,
          description: 'Updated description — metadata only, apiIds unchanged',
          version: '2.0.0',
          apiIds: [apiD.id],
        }),
      });
      expect(response.status).toEqual(200);
      // Verify the name/description were persisted
      const updated = await response.json();
      expect(updated.description).toEqual('Updated description — metadata only, apiIds unchanged');
      expect(updated.version).toEqual('2.0.0');
    });

    test('apiD should still return 200 after a metadata-only update (no access regression)', async () => {
      await fetchGatewaySuccess({
        contextPath: (apiD.listeners[0] as HttpListener).paths[0].path,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });
  });

  // ─── Cleanup ──────────────────────────────────────────────────────────────

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }

    for (const api of [apiA, apiB, apiC, apiD]) {
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
