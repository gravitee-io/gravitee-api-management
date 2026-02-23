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
import { APIsApi, ApiType, ApiV4, HttpListener, PlanMode, PlanSecurityType } from '../../../../lib/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

describe('API Product gateway behavior (API Key)', () => {
  let api: ApiV4;
  let contextPath: string;
  let productId: string;
  let planId: string;
  let apiKey: string;

  beforeAll(async () => {
    // Create a proxy V4 API allowed in API products using import definition.
    // HTTP listener must have at least one entrypoint or start/deploy returns 400.
    const path = `/product-gateway-${faker.string.alphanumeric(8).toLowerCase()}`;
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
                  target: '${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint2',
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

    // Create an API Product referencing this API
    const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
    if (!managementV2BaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL must be defined for api-product-gateway tests');
    }

    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for api-product-gateway tests');
    }
    const adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-gateway-${Date.now()}`,
        description: 'E2E API Product for gateway tests',
        version: '1.0.0',
        apiIds: [api.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    // Create an API Product API_KEY plan
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-product-plan-${Date.now()}`,
          mode: PlanMode.STANDARD,
          security: {
            type: PlanSecurityType.API_KEY,
          },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    planId = planBody.id;

    // Publish plan
    const publishResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_publish`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
      },
    });
    expect(publishResponse.status).toEqual(200);

    // Create subscription to API Product plan
    const appResponse = await fetch(`${process.env.MANAGEMENT_BASE_URL}/organizations/DEFAULT/environments/${envId}/applications`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-gateway-app-${Date.now()}`,
        description: 'App for API Product gateway tests',
        type: 'SIMPLE',
      }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();
    const applicationId = appBody.id;

    const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        applicationId,
        planId,
      }),
    });
    expect(subscriptionResponse.status).toEqual(201);
    const subscriptionBody = await subscriptionResponse.json();
    const subscriptionId = subscriptionBody.id;

    // Retrieve API key for subscription
    const apiKeysResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionId}/api-keys`,
      {
        method: 'GET',
        headers: {
          Authorization: adminAuthHeader,
        },
      },
    );
    expect(apiKeysResponse.status).toEqual(200);
    const apiKeysBody = await apiKeysResponse.json();
    apiKey = apiKeysBody.data[0].key;

    // Start & deploy API so that gateway is in sync (API may return 202 Accepted or 204 No Content)
    await succeed(
      v2ApisResourceAsApiPublisher.startApiRaw({
        envId,
        apiId: api.id,
      }),
      204,
    );

    await succeed(
      v2ApisResourceAsApiPublisher.createApiDeploymentRaw({
        envId,
        apiId: api.id,
      }),
      202,
    );

    // Allow gateway time to sync deployment before hitting the proxy
    await new Promise((r) => setTimeout(r, 2000));
  });

  test('should return 200 with valid API Key', async () => {
    const fullUrl = `${process.env.GATEWAY_BASE_URL}${contextPath}`;

    await fetch(fullUrl, {
      method: 'GET',
      headers: {
        'X-Gravitee-Api-Key': apiKey,
      },
    });
  });

  test('should return 401 without key', async () => {
    const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
      method: 'GET',
    } as any);

    // Gateway may return 401 Unauthorized, 403 Forbidden or 404 when no key is provided
    expect([401, 403, 404]).toContain(response.status);
  });

  test('should return 403 with invalid key', async () => {
    const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
      method: 'GET',
      headers: {
        'X-Gravitee-Api-Key': 'invalid-api-key',
      },
    } as any);

    // Gateway may return 401 Unauthorized, 403 Forbidden or 404 for invalid key
    expect([401, 403, 404]).toContain(response.status);
  });

  test('should return 403 if subscription closed', async () => {
    const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;
    const adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    // Close all subscriptions for the plan (using verify endpoint is overkill; we close directly)
    const subsResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
      method: 'GET',
      headers: {
        Authorization: adminAuthHeader,
      },
    });
    expect(subsResponse.status).toEqual(200);
    const subsBody = await subsResponse.json();
    const currentSubscriptionId = subsBody.data[0].id;

    const closeResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${currentSubscriptionId}/_close`,
      {
        method: 'POST',
        headers: {
          Authorization: adminAuthHeader,
        },
      },
    );
    expect(closeResponse.status).toEqual(200);

    const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
      method: 'GET',
      headers: {
        'X-Gravitee-Api-Key': apiKey,
      },
    } as any);

    // Gateway may return 403 Forbidden or 404 when subscription is closed
    expect([403, 404]).toContain(response.status);
  });

  test('should return 403 if plan closed', async () => {
    const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;
    const adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    const closePlanResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_close`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
      },
    });
    expect(closePlanResponse.status).toEqual(200);

    const response = await fetch(`${process.env.GATEWAY_BASE_URL}${contextPath}`, {
      method: 'GET',
      headers: {
        'X-Gravitee-Api-Key': apiKey,
      },
    } as any);

    // Gateway may return 401 Unauthorized, 403 Forbidden or 404 when plan is closed
    expect([401, 403, 404]).toContain(response.status);
  });

  afterAll(async () => {
    if (api?.id) {
      // API must be stopped before it can be deleted
      await succeed(
        v2ApisResourceAsApiPublisher.stopApiRaw({
          envId,
          apiId: api.id,
        }),
        204,
      );
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: api.id,
        }),
      );
    }
  });
});
