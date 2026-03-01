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
import { APIsApi, ApiType, ApiV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

/**
 * Scenario:
 * - Two V4 PROXY APIs are created and marked as allowed in API Products.
 * - An API Product is created with duplicate API IDs in the request body
 *   (e.g. [api1, api1, api2]).
 * - The product's stored API membership is read back and asserted to contain
 *   only unique IDs — no duplicates.
 * - The product is then updated (redeployed) with duplicate IDs again
 *   (e.g. [api1, api2, api1, api2]).
 * - The stored membership is read back again and asserted to still contain
 *   only unique IDs, confirming no duplicate routing side-effects.
 */
describe('API Product - duplicate API IDs in request are deduplicated', () => {
  let api1: ApiV4;
  let api2: ApiV4;
  let productId: string;
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

    //
    // Create two independent V4 PROXY APIs, each allowed in API Products.
    // They do not need to be started or deployed — this test covers management
    // API deduplication behaviour only, not gateway traffic.
    //
    const makeApi = async (slot: 'a' | 'b'): Promise<ApiV4> => {
      const path = `/product-dedup-${slot}-${faker.string.alphanumeric(6).toLowerCase()}`;
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
                    target: '${process.env.WIREMOCK_BASE_URL}/hello',
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

    api1 = await makeApi('a');
    api2 = await makeApi('b');
  });

  /**
   * Verify that the product creation endpoint deduplicates API IDs when
   * the same ID appears more than once in the request body.
   */
  test('should create product and store only unique API IDs when duplicates are submitted', async () => {
    // api1.id appears twice — expect exactly one occurrence in stored membership
    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-dedup-${Date.now()}`,
        description: 'E2E product duplicate API IDs test',
        version: '1.0.0',
        apiIds: [api1.id, api1.id, api2.id],
      }),
    });
    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;
    expect(productId).toBeDefined();

    // Read the product back to inspect stored API membership
    const getResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
      method: 'GET',
      headers: { Authorization: adminAuthHeader },
    });
    expect(getResponse.status).toEqual(200);
    const getBody = await getResponse.json();
    const storedApiIds: string[] = getBody.apiIds ?? [];

    // Both APIs must be present
    expect(storedApiIds).toContain(api1.id);
    expect(storedApiIds).toContain(api2.id);

    // No duplicates: the unique set must be the same size as the full list
    const uniqueIds = [...new Set(storedApiIds)];
    expect(uniqueIds.length).toEqual(storedApiIds.length);
  });

  /**
   * Verify that updating (redeploying) a product with duplicate API IDs also
   * results in a deduplicated membership — confirming no duplicate routing
   * side-effects are introduced on subsequent deployments.
   */
  test('should update product and store only unique API IDs when duplicates are submitted', async () => {
    // Both api1.id and api2.id appear twice each
    const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
      method: 'PUT',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-dedup-updated-${Date.now()}`,
        description: 'E2E product duplicate API IDs test — after update',
        version: '1.0.0',
        apiIds: [api1.id, api2.id, api1.id, api2.id],
      }),
    });
    expect(updateResponse.status).toEqual(200);

    // Read the product back to inspect stored API membership after update
    const getResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
      method: 'GET',
      headers: { Authorization: adminAuthHeader },
    });
    expect(getResponse.status).toEqual(200);
    const getBody = await getResponse.json();
    const storedApiIds: string[] = getBody.apiIds ?? [];

    expect(storedApiIds).toContain(api1.id);
    expect(storedApiIds).toContain(api2.id);

    const uniqueIds = [...new Set(storedApiIds)];
    expect(uniqueIds.length).toEqual(storedApiIds.length);
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
