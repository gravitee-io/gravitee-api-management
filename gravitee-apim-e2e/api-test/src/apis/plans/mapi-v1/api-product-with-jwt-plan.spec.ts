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

// Shared secret for JWT signing (matches plan security configuration)
const JWT_GIVEN_KEY = 'API_PRODUCT_JWT_GIVEN_KEY_' + faker.string.alphanumeric(8);

describe('API Product with JWT plan - gateway security enforcement', () => {
  let api: ApiV4;
  let contextPath: string;
  let productId: string;
  let planId: string;
  let applicationId: string;
  let subscriptionId: string;
  let CLIENT_ID: string;

  beforeAll(async () => {
    CLIENT_ID = 'my-test-client-id-' + faker.string.alphanumeric(4);
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
    // 1. Create a V4 PROXY API allowed in API Products, with HTTP listener and entrypoint
    //
    const path = `/product-jwt-${faker.string.alphanumeric(8).toLowerCase()}`;
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
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint2`,
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
        name: `e2e-product-jwt-${Date.now()}`,
        description: 'E2E product with JWT plan',
        version: '1.0.0',
        apiIds: [api.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;

    //
    // 3. Create and publish an API Product JWT plan
    //
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(
        MAPIV2PlansFaker.newPlanV4({
          name: `e2e-product-jwt-plan-${Date.now()}`,
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
              resolverParameter: JWT_GIVEN_KEY,
            },
          },
        }),
      ),
    });
    expect(planResponse.status).toEqual(201);
    const planBody = await planResponse.json();
    planId = planBody.id;

    const publishPlanResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans/${planId}/_publish`,
      {
        method: 'POST',
        headers: {
          Authorization: adminAuthHeader,
        },
      },
    );
    expect(publishPlanResponse.status).toEqual(200);

    //
    // 4. Create an application and subscribe to the Product JWT plan
    //
    const appResponse = await fetch(`${managementBaseUrl}/organizations/${orgId}/environments/${envId}/applications`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-jwt-app-${Date.now()}`,
        description: 'Application for product JWT plan',
        type: 'SIMPLE',
        settings: {
          app: {
            client_id: CLIENT_ID,
          },
        },
      }),
    });
    expect(appResponse.status).toEqual(201);
    const appBody = await appResponse.json();
    applicationId = appBody.id;

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
    subscriptionId = subscriptionBody.id;

    //
    // 5. Start and deploy API so gateway is in sync
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

  test('should allow gateway traffic with valid JWT (200)', async () => {
    const token = jwt.sign({ foo: 'bar', client_id: CLIENT_ID }, JWT_GIVEN_KEY, { algorithm: 'HS256' });

    await fetchGatewaySuccess({
      contextPath,
      timeBetweenRetries: 5000,
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  });

  test('should reject gateway traffic without token (401)', async () => {
    await fetchGatewayUnauthorized({
      contextPath,
      timeBetweenRetries: 5000,
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
        // API may already be stopped or deleted; continue to delete
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
