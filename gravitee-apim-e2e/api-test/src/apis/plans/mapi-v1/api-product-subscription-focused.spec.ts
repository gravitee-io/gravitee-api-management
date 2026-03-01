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
import { forManagementV2AsApiUser, forPortalAsAdminUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { fetchGatewaySuccess } from '@gravitee/utils/apim-http';
import { SubscriptionApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/SubscriptionApi';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';
const orgId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const portalSubscriptionApi = new SubscriptionApi(forPortalAsAdminUser());

/**
 * Subscription-focused e2e tests for API Products:
 *
 * E1 - Subscription created but not accepted: PENDING subscription key returns 401.
 * E2 - Subscription revoked/cancelled: After revoke/close, same key returns 401.
 * E3 - Two apps on same product plan (isolation): Revoking one key leaves the other working.
 */
describe('API Product subscription-focused e2e', () => {
  let api: ApiV4;
  let contextPath: string;
  let productId: string;
  let planId: string;
  let managementV2BaseUrl: string;
  let managementBaseUrl: string;
  let adminAuthHeader: string;

  beforeAll(async () => {
    managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL!;
    managementBaseUrl = process.env.MANAGEMENT_BASE_URL!;
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl || !managementBaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL and MANAGEMENT_BASE_URL must be defined');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined');
    }

    adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    const path = `/product-subscription-focused-${faker.string.alphanumeric(8).toLowerCase()}`;
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
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=subscription-focused`,
                },
              },
            ],
          },
        ],
        allowedInApiProducts: true,
      }),
    });

    api = (await created(
      v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
        envId,
        exportApiV4: apiImport,
      }),
    )) as ApiV4;
    contextPath = (api.listeners[0] as HttpListener).paths[0].path;

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-product-subscription-focused-${Date.now()}`,
        description: 'E2E API Product for subscription-focused tests',
        version: '1.0.0',
        apiIds: [api.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    productId = (await productResponse.json()).id;

    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-product-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: { type: PlanSecurityType.API_KEY },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    planId = (await planResponse.json()).id;

    const publishResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_publish`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader },
    });
    expect(publishResponse.status).toEqual(200);

    await succeed(v2ApisResourceAsApiPublisher.startApiRaw({ envId, apiId: api.id }), 204);
    await succeed(v2ApisResourceAsApiPublisher.createApiDeploymentRaw({ envId, apiId: api.id }), 202);

    const deployProductResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/deployments`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });
    expect([200, 202]).toContain(deployProductResponse.status);

    await new Promise((r) => setTimeout(r, 2000));
  });

  /**
   * E1 - Subscription created but not accepted
   * Create subscription in pending/manual state; attempt access with pending subscription key.
   * Expectation: Access denied (401).
   */
  describe('E1 - Subscription created but not accepted', () => {
    let manualPlanId: string;
    let pendingSubscriptionId: string;

    beforeAll(async () => {
      const manualPlanResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify(
          MAPIV2PlansFaker.newPlanV4({
            name: `e2e-product-plan-manual-${Date.now()}`,
            mode: PlanMode.STANDARD,
            security: { type: PlanSecurityType.API_KEY },
            validation: 'MANUAL',
          }),
        ),
      });
      expect(manualPlanResponse.status).toEqual(201);
      manualPlanId = (await manualPlanResponse.json()).id;

      const publishManualResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${manualPlanId}/_publish`,
        { method: 'POST', headers: { Authorization: adminAuthHeader } },
      );
      expect(publishManualResponse.status).toEqual(200);

      const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-pending-app-${Date.now()}`,
          description: 'App for E1 pending subscription test',
          type: 'SIMPLE',
        }),
      });
      expect(appResponse.status).toEqual(201);
      const appId = (await appResponse.json()).id;

      const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({ applicationId: appId, planId: manualPlanId }),
      });
      expect(subscriptionResponse.status).toEqual(201);
      const subBody = await subscriptionResponse.json();
      pendingSubscriptionId = subBody.id;
      expect(subBody.status).toEqual('PENDING');

      const apiKeysResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${pendingSubscriptionId}/api-keys`,
        { method: 'GET', headers: { Authorization: adminAuthHeader } },
      );
      expect(apiKeysResponse.status).toEqual(200);
      const keysData = (await apiKeysResponse.json()).data;
      // PENDING subscriptions do not have API keys until accepted
      expect(keysData?.length ?? 0).toBe(0);
    });

    test('E1: access without valid key (pending subscription has none) should return 401', async () => {
      // No API key exists for PENDING subscription; any access attempt is denied
      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': 'invalid-pending-subscription-key' },
      } as any);

      expect(response.status).toEqual(401);
    });
  });

  /**
   * E2 - Subscription revoked/cancelled
   * Start from working accepted subscription; revoke/cancel subscription; retry access with same key.
   * Expectation: Access denied (401) after revoke/cancel.
   */
  describe('E2 - Subscription revoked/cancelled', () => {
    let subscriptionId: string;
    let apiKey: string;

    beforeAll(async () => {
      const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-revoke-app-${Date.now()}`,
          description: 'App for E2 revoke test',
          type: 'SIMPLE',
        }),
      });
      expect(appResponse.status).toEqual(201);
      const appId = (await appResponse.json()).id;

      const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({ applicationId: appId, planId }),
      });
      expect(subscriptionResponse.status).toEqual(201);
      subscriptionId = (await subscriptionResponse.json()).id;

      const apiKeysResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId}/api-keys`,
        { method: 'GET', headers: { Authorization: adminAuthHeader } },
      );
      expect(apiKeysResponse.status).toEqual(200);
      apiKey = (await apiKeysResponse.json()).data[0].key;
    });

    test('E2: access with accepted subscription key should return 200', async () => {
      await fetchGatewaySuccess({
        contextPath,
        headers: { 'X-Gravitee-Api-Key': apiKey },
      });
    });

    test('E2: after closing subscription, access with same key should return 401', async () => {
      const closeResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId}/_close`,
        { method: 'POST', headers: { Authorization: adminAuthHeader } },
      );
      expect(closeResponse.status).toEqual(200);

      // Allow gateway time to sync subscription closure before asserting
      await new Promise((r) => setTimeout(r, 6000));

      const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey },
      } as any);

      expect([401, 403, 404]).toContain(response.status);
    });
  });

  /**
   * E3 - Two apps on same product plan (isolation)
   * Subscribe two applications to same plan; validate both keys work; revoke one key; re-check both.
   * Expectation: Revoked key returns 401; other app key continues to work (200).
   */
  describe('E3 - Two apps on same product plan (isolation)', () => {
    let subscriptionId1: string;
    let subscriptionId2: string;
    let apiKey1: string;
    let apiKey2: string;

    beforeAll(async () => {
      const app1Response = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-isolation-app1-${Date.now()}`,
          description: 'App 1 for E3 isolation test',
          type: 'SIMPLE',
        }),
      });
      expect(app1Response.status).toEqual(201);
      const app1Id = (await app1Response.json()).id;

      const app2Response = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: `e2e-isolation-app2-${Date.now()}`,
          description: 'App 2 for E3 isolation test',
          type: 'SIMPLE',
        }),
      });
      expect(app2Response.status).toEqual(201);
      const app2Id = (await app2Response.json()).id;

      const sub1Response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({ applicationId: app1Id, planId }),
      });
      expect(sub1Response.status).toEqual(201);
      subscriptionId1 = (await sub1Response.json()).id;

      const sub2Response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
        method: 'POST',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({ applicationId: app2Id, planId }),
      });
      expect(sub2Response.status).toEqual(201);
      subscriptionId2 = (await sub2Response.json()).id;

      const keys1Response = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId1}/api-keys`,
        { method: 'GET', headers: { Authorization: adminAuthHeader } },
      );
      expect(keys1Response.status).toEqual(200);
      apiKey1 = (await keys1Response.json()).data[0].key;

      const keys2Response = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId2}/api-keys`,
        { method: 'GET', headers: { Authorization: adminAuthHeader } },
      );
      expect(keys2Response.status).toEqual(200);
      apiKey2 = (await keys2Response.json()).data[0].key;
    });

    test('E3: both keys should return 200 initially', async () => {
      await fetchGatewaySuccess({
        contextPath,
        headers: { 'X-Gravitee-Api-Key': apiKey1 },
      });
      await fetchGatewaySuccess({
        contextPath,
        headers: { 'X-Gravitee-Api-Key': apiKey2 },
      });
    });

    test('E3: after revoking app1 key, app1 key returns 401 and app2 key still returns 200', async () => {
      const revokeResponse = await portalSubscriptionApi.revokeKeySubscriptionRaw({
        subscriptionId: subscriptionId1,
        apiKey: apiKey1,
      });
      expect(revokeResponse.raw.status).toEqual(204);

      await new Promise((resolve) => setTimeout(resolve, 6000));

      const response1 = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
        method: 'GET',
        headers: { 'X-Gravitee-Api-Key': apiKey1 },
      } as any);
      expect([401, 403]).toContain(response1.status);

      await fetchGatewaySuccess({
        contextPath,
        headers: { 'X-Gravitee-Api-Key': apiKey2 },
      });
    });
  });

  afterAll(async () => {
    if (api?.id) {
      try {
        await succeed(v2ApisResourceAsApiPublisher.stopApiRaw({ envId, apiId: api.id }), 204);
      } catch {
        /* best-effort */
      }
      try {
        await noContent(
          v2ApisResourceAsApiPublisher.deleteApiRaw({
            envId,
            apiId: api.id,
            closePlans: true,
          }),
        );
      } catch {
        /* best-effort */
      }
    }
  });
});
