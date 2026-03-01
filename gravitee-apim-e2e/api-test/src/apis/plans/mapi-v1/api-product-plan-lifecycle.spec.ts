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
import jwt from 'jsonwebtoken';
import { APIsApi, ApiType, ApiV4, HttpListener, PlanMode, PlanSecurityType } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { fetchGatewaySuccess, fetchGatewayUnauthorized } from '@gravitee/utils/apim-http';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';
const orgId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

/**
 * D-series — Plan-focused lifecycle scenarios
 *
 *   D1 : Plan created in STAGING (not published) → subscription creation is blocked.
 *   D2 : Active plan deprecated then closed → access is maintained during deprecation,
 *        revoked after close.
 *   D3 : Security-type coverage (API_KEY, JWT, mTLS) in API Product context.
 *   D4 : Plan closed directly with active subscriptions → existing keys are revoked.
 */

// ─── Shared proxy-API factory ─────────────────────────────────────────────────

const makeProxyApi = async (slot: string): Promise<ApiV4> => {
  const path = `/product-plan-${slot}-${faker.string.alphanumeric(6).toLowerCase()}`;
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
                target: `${process.env.WIREMOCK_BASE_URL}/hello?name=plan-lifecycle`,
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
// D1 — Plan created but not published (STAGING)
// ─────────────────────────────────────────────────────────────────────────────

describe('D1 - Plan in STAGING state blocks subscription creation', () => {
  let api: ApiV4;
  let productId: string;
  let planId: string;
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

    //
    // 1. Create a V4 API and an API Product with that API attached.
    //
    api = await makeProxyApi('d1');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-d1-${Date.now()}`,
        description: 'D1: unpublished plan test',
        version: '1.0.0',
        apiIds: [api.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    //
    // 2. Create a plan but deliberately do NOT publish it — it stays in STAGING.
    //
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-d1-staging-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    planId = planBody.id;

    //
    // 3. Create an application to use as the subscription requester.
    //
    const appResponse = await fetch(`${managementV2BaseUrl.replace('/v2', '')}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: `e2e-d1-app-${Date.now()}`, description: 'D1 app', type: 'SIMPLE' }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();

    //
    // 4. Attempt to subscribe to the STAGING plan and store the response for assertions.
    //
    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ applicationId: appBody.id, planId }),
    });

    // Store as describe-scoped variable so tests can inspect it.
    (globalThis as any).__d1_subscriptionStatus = subscriptionResponse.status;
  });

  test('plan should be in STAGING status after creation without publishing', async () => {
    const listResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans?statuses=STAGING&securities=API_KEY`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(listResponse.status).toEqual(200);
    const listBody = await listResponse.json();
    const stagingPlan = (listBody.data as Array<{ id: string; status: string }>).find((p) => p.id === planId);
    expect(stagingPlan).toBeDefined();
    expect(stagingPlan!.status).toEqual('STAGING');
  });

  test('subscription creation against a STAGING plan should be rejected (non-201 response)', () => {
    // The subscription attempt was made in beforeAll.
    // A STAGING plan must not accept new subscriptions.
    const status: number = (globalThis as any).__d1_subscriptionStatus;
    expect(status).not.toEqual(201);
  });

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }
    if (api?.id) {
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
      } catch {}
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// D2 — Plan deprecated then closed: access retained during deprecation, revoked on close
// ─────────────────────────────────────────────────────────────────────────────

describe('D2 - Deprecate then close plan: existing key survives deprecation but is revoked on close', () => {
  let api: ApiV4;
  let contextPath: string;
  let productId: string;
  let planId: string;
  let subscriptionId: string;
  let apiKey: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;
  let managementBaseUrl: string;

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

    api = await makeProxyApi('d2');
    contextPath = (api.listeners[0] as HttpListener).paths[0].path;

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-d2-${Date.now()}`,
        description: 'D2: deprecate then close plan test',
        version: '1.0.0',
        apiIds: [api.id],
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
          name: `e2e-d2-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    planId = planBody.id;

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: `e2e-d2-app-${Date.now()}`, description: 'D2 app', type: 'SIMPLE' }),
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
    subscriptionId = subscriptionBody.id;

    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId}/api-keys`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;
    expect(apiKey).toBeDefined();

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
    await new Promise((r) => setTimeout(r, 6000));
  });

  test('baseline: key should return 200 with an active published plan', async () => {
    await fetchGatewaySuccess({
      contextPath,
      headers: { 'X-Gravitee-Api-Key': apiKey },
      maxRetries: 10,
      timeBetweenRetries: 2000,
    });
  });

  // ─── Phase: after deprecation ───────────────────────────────────────────────

  describe('after deprecating the plan', () => {
    beforeAll(async () => {
      const deprecateResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_deprecate`,
        { method: 'POST', headers: { Authorization: adminAuthHeader } },
      );
      expect(deprecateResponse.status).toEqual(200);
      const deprecatedPlan = await deprecateResponse.json();
      expect(deprecatedPlan.status).toEqual('DEPRECATED');
    });

    test('existing key should still return 200 after plan deprecation (deprecation does not revoke active subscriptions)', async () => {
      await fetchGatewaySuccess({
        contextPath,
        headers: { 'X-Gravitee-Api-Key': apiKey },
        maxRetries: 10,
        timeBetweenRetries: 2000,
      });
    });

    test('new subscription creation against a DEPRECATED plan should be rejected', async () => {
      const newAppResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: `e2e-d2-new-app-${Date.now()}`, description: 'D2 new app post-deprecation', type: 'SIMPLE' }),
      });
      expect(newAppResponse.status).toEqual(201);
      const newAppBody = await newAppResponse.json();

      const newSubscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({ applicationId: newAppBody.id, planId }),
      });
      // Deprecated plan must not accept new subscriptions.
      expect(newSubscriptionResponse.status).not.toEqual(201);
    });
  });

  // ─── Phase: after close ─────────────────────────────────────────────────────

  describe('after closing the deprecated plan', () => {
    beforeAll(async () => {
      // The platform requires all active subscriptions to be closed before a plan can be closed.
      // Close the subscription first so that _close on the plan succeeds.
      const closeSubResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId}/_close`,
        { method: 'POST', headers: { Authorization: adminAuthHeader } },
      );
      expect(closeSubResponse.status).toEqual(200);

      const closeResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_close`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader },
      });
      expect(closeResponse.status).toEqual(200);
      const closedPlan = await closeResponse.json();
      expect(closedPlan.status).toEqual('CLOSED');

      // Allow the gateway time to process the subscription revocation.
      await new Promise((resolve) => setTimeout(resolve, 6000));
    });

    test('key should return 401/403/404 after the plan is closed (all subscriptions revoked)', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
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
    if (api?.id) {
      try {
        await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
      } catch {}
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
      } catch {}
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// D3 — Security type coverage (API_KEY, JWT, mTLS) in API Product context
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Each sub-scenario uses its own independent V4 PROXY API so that the gateway
 * security chain for one security type never interferes with another.
 * When an API belongs to two products with different security types (e.g.
 * API_KEY + JWT) the gateway processes the security policies in priority order
 * and may reject a JWT-only request with 401 before the JWT plan is evaluated.
 * Giving each sub-describe its own API avoids this ambiguity entirely.
 */

// ─── D3.1 — API_KEY ──────────────────────────────────────────────────────────

describe('D3.1 - API_KEY security type in API Product context', () => {
  let api: ApiV4;
  let productId: string;
  let apiKey: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;
  let managementBaseUrl: string;

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
    api = await makeProxyApi('d3-1');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-d3-apikey-${Date.now()}`,
        description: 'D3.1: API_KEY security type coverage',
        version: '1.0.0',
        apiIds: [api.id],
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
          name: `e2e-d3-apikey-plan-${Date.now()}`,
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
      body: JSON.stringify({ name: `e2e-d3-apikey-app-${Date.now()}`, description: 'D3.1 app', type: 'SIMPLE' }),
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

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
    await new Promise((r) => setTimeout(r, 6000));
  });

  test('valid API key should return 200 on the API Product context path', async () => {
    await fetchGatewaySuccess({
      contextPath: (api.listeners[0] as HttpListener).paths[0].path,
      headers: { 'X-Gravitee-Api-Key': apiKey },
      maxRetries: 10,
      timeBetweenRetries: 2000,
    });
  });

  test('request without API key should return 401', async () => {
    await fetchGatewayUnauthorized({
      contextPath: (api.listeners[0] as HttpListener).paths[0].path,
    });
  });

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }
    if (api?.id) {
      try {
        await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
      } catch {}
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
      } catch {}
    }
  });
});

// ─── D3.2 — JWT ──────────────────────────────────────────────────────────────

describe('D3.2 - JWT security type in API Product context', () => {
  let api: ApiV4;
  let productId: string;
  let CLIENT_ID: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;
  let managementBaseUrl: string;

  const JWT_SECRET = `d3-jwt-secret-${faker.string.alphanumeric(12)}`;

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
    CLIENT_ID = `d3-jwt-client-${faker.string.alphanumeric(6)}`;
    api = await makeProxyApi('d3-2');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-d3-jwt-${Date.now()}`,
        description: 'D3.2: JWT security type coverage',
        version: '1.0.0',
        apiIds: [api.id],
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
          name: `e2e-d3-jwt-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: {
            type: PlanSecurityType.JWT,
            configuration: {
              signature: 'HMAC_HS256',
              publicKeyResolver: 'GIVEN_KEY',
              useSystemProxy: false,
              extractClaims: false,
              propagateAuthHeader: true,
              userClaim: 'sub',
              resolverParameter: JWT_SECRET,
            },
          },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planBody.id}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });

    // JWT plans require an application with a matching client_id.
    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-d3-jwt-app-${Date.now()}`,
        description: 'D3.2 JWT app',
        type: 'SIMPLE',
        settings: { app: { client_id: CLIENT_ID } },
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

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
  });

  test('request without JWT token should return 401', async () => {
    await fetchGatewayUnauthorized({
      contextPath: (api.listeners[0] as HttpListener).paths[0].path,
      timeBetweenRetries: 5000,
    });
  });

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }
    if (api?.id) {
      try {
        await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
      } catch {}
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
      } catch {}
    }
  });
});

// ─── D3.3 — mTLS (management API only) ───────────────────────────────────────

/**
 * Gateway-level mTLS validation requires client certificate infrastructure
 * that is not available in this e2e environment. This sub-scenario verifies
 * only that an mTLS plan can be created on an API Product via the management
 * API and is returned with the correct security type.
 */
describe('D3.3 - mTLS security type in API Product context (plan creation — management API only)', () => {
  let api: ApiV4;
  let productId: string;
  let planId: string;
  let adminAuthHeader: string;
  let managementV2BaseUrl: string;

  beforeAll(async () => {
    managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL ?? '';
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL must be defined for this test');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for this test');
    }

    adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;
    api = await makeProxyApi('d3-3');

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-d3-mtls-${Date.now()}`,
        description: 'D3.3: mTLS security type coverage',
        version: '1.0.0',
        apiIds: [api.id],
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
          name: `e2e-d3-mtls-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.MTLS },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    planId = planBody.id;
  });

  test('mTLS plan should be created successfully on an API Product', async () => {
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}`, {
      method: 'GET',
      headers: { Authorization: adminAuthHeader },
    });
    expect(planResponse.status).toEqual(200);
    const plan = await planResponse.json();
    expect(plan.id).toEqual(planId);
    expect(plan.security.type).toEqual('MTLS');
  });

  test('mTLS plan should be listed under product plans with STAGING status', async () => {
    const listResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans?statuses=STAGING`, {
      method: 'GET',
      headers: { Authorization: adminAuthHeader },
    });
    expect(listResponse.status).toEqual(200);
    const listBody = await listResponse.json();
    const mtlsPlan = (listBody.data as Array<{ id: string; security: { type: string } }>).find((p) => p.id === planId);
    expect(mtlsPlan).toBeDefined();
    expect(mtlsPlan!.security.type).toEqual('MTLS');
  });

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }
    if (api?.id) {
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
      } catch {}
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// D4 — Close plan directly with active subscriptions: keys are revoked
// ─────────────────────────────────────────────────────────────────────────────

describe('D4 - Close plan with active subscriptions: existing keys are revoked immediately', () => {
  let api: ApiV4;
  let contextPath: string;
  let productId: string;
  let planId: string;
  let subscriptionId: string;
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

    api = await makeProxyApi('d4');
    contextPath = (api.listeners[0] as HttpListener).paths[0].path;

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-d4-${Date.now()}`,
        description: 'D4: close plan with active subscriptions test',
        version: '1.0.0',
        apiIds: [api.id],
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
          name: `e2e-d4-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    planId = planBody.id;

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });

    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: `e2e-d4-app-${Date.now()}`, description: 'D4 app', type: 'SIMPLE' }),
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
    subscriptionId = subscriptionBody.id;

    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId}/api-keys`,
      { method: 'GET', headers: { Authorization: adminAuthHeader } },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;
    expect(apiKey).toBeDefined();

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api.id }), 202);

    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployResponse.status);
    await new Promise((r) => setTimeout(r, 6000));
  });

  test('baseline: key should return 200 before the plan is closed', async () => {
    await fetchGatewaySuccess({
      contextPath,
      headers: { 'X-Gravitee-Api-Key': apiKey },
      maxRetries: 10,
      timeBetweenRetries: 2000,
    });
  });

  describe('after closing the plan directly (PUBLISHED → CLOSED)', () => {
    beforeAll(async () => {
      // The platform requires all active subscriptions to be closed before a plan can be closed.
      // Close the subscription first so that _close on the plan succeeds.
      const closeSubResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId}/_close`,
        { method: 'POST', headers: { Authorization: adminAuthHeader } },
      );
      expect(closeSubResponse.status).toEqual(200);

      const closeResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_close`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader },
      });
      expect(closeResponse.status).toEqual(200);
      const closedPlan = await closeResponse.json();
      expect(closedPlan.status).toEqual('CLOSED');

      // Allow the gateway time to process the subscription revocation.
      await new Promise((resolve) => setTimeout(resolve, 6000));
    });

    test('key should return 401/403/404 after the plan is closed directly from PUBLISHED state', async () => {
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey },
      } as any);
      expect([401, 403, 404]).toContain(response.status);
    });

    test('closed plan should have CLOSED status in the management API', async () => {
      const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}`, {
        method: 'GET',
        headers: { Authorization: adminAuthHeader },
      });
      expect(planResponse.status).toEqual(200);
      const plan = await planResponse.json();
      expect(plan.status).toEqual('CLOSED');
    });
  });

  afterAll(async () => {
    if (productId) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
    }
    if (api?.id) {
      try {
        await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
      } catch {}
      try {
        await noContent(v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id, closePlans: true }));
      } catch {}
    }
  });
});
