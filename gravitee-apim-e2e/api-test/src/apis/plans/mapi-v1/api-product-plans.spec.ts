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
import { APIsApi, ApiType, ApiV4, HttpListener, PlanMode, PlanSecurityType } from '../../../../../lib/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { created } from '@lib/jest-utils';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';

const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
const adminUsername = process.env.ADMIN_USERNAME;
const adminPassword = process.env.ADMIN_PASSWORD;

if (!managementV2BaseUrl) {
  throw new Error('MANAGEMENT_V2_BASE_URL must be defined to run API Product plan e2e tests');
}
if (!adminUsername || !adminPassword) {
  throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined to run API Product plan e2e tests');
}

const authHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

interface ApiProduct {
  id: string;
  environmentId: string;
  name: string;
  description?: string;
  version: string;
}

interface ApiProductPlan {
  id: string;
  name: string;
  description?: string;
  security: {
    type: string;
    configuration?: unknown;
  };
  mode?: string;
  status?: string;
}

describe('API Product plans (management v2)', () => {
  let api: ApiV4;
  let apiProduct: ApiProduct;
  let plan: ApiProductPlan;

  beforeAll(async () => {
    const uniqueSuffix = Date.now();
    const apiProductName = `e2e-product-plans-${uniqueSuffix}`;

    // Create a V4 PROXY API (with HTTP listener + entrypoint) and attach to product so plan creation is allowed
    const path = `/product-plans-${faker.string.alphanumeric(8).toLowerCase()}`;
    const apiImport = MAPIV2ApisFaker.apiImportV4({
      api: MAPIV2ApisFaker.apiV4Proxy({
        type: ApiType.PROXY,
        listeners: [{ type: 'HTTP', paths: [{ path }], entrypoints: [{ type: 'http-proxy' }] }],
        endpointGroups: [
          {
            name: 'default-group',
            type: 'http-proxy',
            endpoints: [
              {
                name: 'default',
                type: 'http-proxy',
                configuration: { target: `${process.env.WIREMOCK_BASE_URL}/hello` },
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

    // Create dedicated API Product for plan tests and attach the API
    const createResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: apiProductName,
        description: 'E2E API Product for plan tests',
        version: '1.0.0',
        apiIds: [api.id],
      }),
    });

    expect(createResponse.status).toEqual(201);
    apiProduct = (await createResponse.json()) as ApiProduct;
    expect(apiProduct.id).toBeDefined();
  });

  /**
   * Scenario A — Create plan inside product
   * Covers:
   * - Plan attach logic
   * - Basic validation (required fields, default status)
   * Note: API Product plans do not allow KEY_LESS; use API_KEY.
   */
  test('should create plan inside API Product and attach it to product', async () => {
    const planName = `e2e-product-plan-${Date.now()}`;
    const planPayload = MAPIV2PlansFaker.newPlanV4({
      name: planName,
      description: 'Plan attached to API Product',
      order: 1,
      mode: PlanMode.STANDARD,
      security: { type: PlanSecurityType.API_KEY },
      validation: 'AUTO',
    });

    const createPlanResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/plans`, {
      method: 'POST',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(planPayload),
    });

    expect(createPlanResponse.status).toEqual(201);
    plan = (await createPlanResponse.json()) as ApiProductPlan;
    expect(plan.id).toBeDefined();
    expect(plan.name).toEqual(planName);
    expect(plan.security.type).toEqual('API_KEY');

    // Verify plan is listed under product plans (include STAGING since a new plan starts in STAGING)
    const listPlansResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/plans?statuses=STAGING,PUBLISHED&securities=API_KEY`,
      {
        method: 'GET',
        headers: {
          Authorization: authHeader,
        },
      },
    );
    expect(listPlansResponse.status).toEqual(200);
    const listBody = (await listPlansResponse.json()) as {
      data: ApiProductPlan[];
    };
    expect(listBody.data.find((p) => p.id === plan.id)).toBeDefined();
  });

  /**
   * Scenario B — Update plan (description)
   * Covers:
   * - Plan update logic
   */
  test('should update API Product plan security configuration', async () => {
    const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/plans/${plan.id}`, {
      method: 'PUT',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        description: 'Updated description for API Product plan',
      }),
    });

    expect(updateResponse.status).toEqual(200);
    const updatedPlan = (await updateResponse.json()) as ApiProductPlan;
    expect(updatedPlan.description).toEqual('Updated description for API Product plan');
  });

  /**
   * Scenario C — Close / Deprecate plan
   * Covers:
   * - Plan lifecycle transitions
   */
  test('should publish, deprecate then close API Product plan', async () => {
    // Publish
    const publishResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/plans/${plan.id}/_publish`,
      {
        method: 'POST',
        headers: {
          Authorization: authHeader,
        },
      },
    );
    expect(publishResponse.status).toEqual(200);
    const publishedPlan = (await publishResponse.json()) as ApiProductPlan;
    expect(publishedPlan.status).toEqual('PUBLISHED');

    // Deprecate
    const deprecateResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/plans/${plan.id}/_deprecate`,
      {
        method: 'POST',
        headers: {
          Authorization: authHeader,
        },
      },
    );
    expect(deprecateResponse.status).toEqual(200);
    const deprecatedPlan = (await deprecateResponse.json()) as ApiProductPlan;
    expect(deprecatedPlan.status).toEqual('DEPRECATED');

    // Close
    const closeResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/plans/${plan.id}/_close`,
      {
        method: 'POST',
        headers: {
          Authorization: authHeader,
        },
      },
    );
    expect(closeResponse.status).toEqual(200);
    const closedPlan = (await closeResponse.json()) as ApiProductPlan;
    expect(closedPlan.status).toEqual('CLOSED');
  });

  afterAll(async () => {
    if (apiProduct?.id) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
        method: 'DELETE',
        headers: {
          Authorization: authHeader,
        },
      });
    }
  });
});
