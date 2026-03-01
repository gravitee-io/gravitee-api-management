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
 * - One V4 PROXY API attached to one API Product.
 * - Two independent API_KEY plans are created and published on the same product.
 * - A dedicated application subscribes to each plan independently.
 * - Each subscription produces its own API key.
 * - Both keys must return HTTP 200 when calling the gateway endpoint.
 */
describe('API Product with multiple API_KEY plans - each subscription key grants access (200)', () => {
  let api: ApiV4;
  let contextPath: string;
  let productId: string;

  // Per-plan state: indexed by plan slot (0 = plan A, 1 = plan B)
  const planIds: string[] = [];
  const applicationIds: string[] = [];
  const subscriptionIds: string[] = [];
  const apiKeys: string[] = [];

  const PLAN_COUNT = 2;

  beforeAll(async () => {
    const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
    const managementBaseUrl = process.env.MANAGEMENT_BASE_URL;
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!managementV2BaseUrl || !managementBaseUrl) {
      throw new Error('MANAGEMENT_V2_BASE_URL and MANAGEMENT_BASE_URL must be defined for this test');
    }
    if (!adminUsername || !adminPassword) {
      throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined for this test');
    }

    const adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

    //
    // 1. Create a V4 PROXY API allowed in API Products
    //
    const path = `/product-multi-plan-${faker.string.alphanumeric(8).toLowerCase()}`;
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
                  target: '${process.env.WIREMOCK_BASE_URL}/hello?name=multi-plan',
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

    //
    // 2. Create an API Product and attach the V4 API
    //
    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-multi-plan-${Date.now()}`,
        description: 'E2E product with multiple API_KEY plans',
        version: '1.0.0',
        apiIds: [api.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    //
    // 3. Create PLAN_COUNT plans, each published, with one application + subscription each
    //
    for (let i = 0; i < PLAN_COUNT; i++) {
      // 3a. Create and publish the plan
      const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
        method: 'POST',
        headers: {
          Authorization: adminAuthHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(
          MAPIV2PlansFaker.newPlanV4({
            name: `e2e-multi-plan-${i + 1}-${Date.now()}`,
            mode: PlanMode.STANDARD,
            security: { type: PlanSecurityType.API_KEY },
          }),
        ),
      });
      expect(planResponse.status).toEqual(201);
      const planBody = await planResponse.json();
      planIds.push(planBody.id);

      const publishPlanResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planBody.id}/_publish`,
        {
          method: 'POST',
          headers: {
            Authorization: adminAuthHeader,
          },
        },
      );
      expect(publishPlanResponse.status).toEqual(200);

      // 3b. Create an application for this plan
      const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
        method: 'POST',
        headers: {
          Authorization: adminAuthHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: `e2e-multi-plan-app-${i + 1}-${Date.now()}`,
          description: `Application for plan ${i + 1}`,
          type: 'SIMPLE',
        }),
      });
      expect(appResponse.status).toEqual(201);
      const appBody = await appResponse.json();
      applicationIds.push(appBody.id);

      // 3c. Subscribe the application to this plan
      const subscriptionResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions`, {
        method: 'POST',
        headers: {
          Authorization: adminAuthHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationId: appBody.id,
          planId: planBody.id,
        }),
      });
      expect(subscriptionResponse.status).toEqual(201);
      const subscriptionBody = await subscriptionResponse.json();
      subscriptionIds.push(subscriptionBody.id);

      // 3d. Fetch the API key for this subscription
      const apiKeysResponse = await fetch(
        `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/subscriptions/${subscriptionBody.id}/api-keys`,
        {
          method: 'GET',
          headers: {
            Authorization: adminAuthHeader,
          },
        },
      );
      expect(apiKeysResponse.status).toEqual(200);
      const apiKeysBody = await apiKeysResponse.json();
      const key = apiKeysBody.data[0].key;
      expect(key).toBeDefined();
      apiKeys.push(key);
    }

    //
    // 4. Start, deploy the API, then deploy the product so the gateway is in sync
    //
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

  //
  // Tests — each plan's key must independently grant gateway access (200)
  //
  test('should allow gateway traffic using API key from plan 1 subscription (200)', async () => {
    await fetchGatewaySuccess({
      contextPath,
      headers: {
        'X-Gravitee-Api-Key': apiKeys[0],
      },
    });
  });

  test('should allow gateway traffic using API key from plan 2 subscription (200)', async () => {
    await fetchGatewaySuccess({
      contextPath,
      headers: {
        'X-Gravitee-Api-Key': apiKeys[1],
      },
    });
  });

  afterAll(async () => {
    if (api?.id) {
      try {
        await succeed(
          v2ApisResourceAsApiPublisher.stopApiRaw({
            envId,
            apiId: api.id,
          }),
          204,
        );
      } catch {
        // API may already be stopped; continue to delete
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
        // Best-effort only; environment reset will clean up
      }
    }
  });
});
