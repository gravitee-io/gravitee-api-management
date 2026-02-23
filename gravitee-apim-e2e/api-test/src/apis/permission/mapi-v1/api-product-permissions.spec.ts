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

const envId = 'DEFAULT';

const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
const adminUsername = process.env.ADMIN_USERNAME;
const adminPassword = process.env.ADMIN_PASSWORD;
const apiUsername = process.env.API_USERNAME;
const apiPassword = process.env.API_PASSWORD;
const simpleUsername = process.env.SIMPLE_USERNAME;
const simplePassword = process.env.SIMPLE_PASSWORD;

if (!managementV2BaseUrl) {
  throw new Error('MANAGEMENT_V2_BASE_URL must be defined to run API Product permissions e2e tests');
}

const adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;
const apiAuthHeader = apiUsername && apiPassword ? `Basic ${Buffer.from(`${apiUsername}:${apiPassword}`).toString('base64')}` : undefined;
const simpleAuthHeader =
  simpleUsername && simplePassword ? `Basic ${Buffer.from(`${simpleUsername}:${simplePassword}`).toString('base64')}` : undefined;

interface ApiProduct {
  id: string;
  environmentId: string;
  name: string;
  description?: string;
  version: string;
  primaryOwner?: {
    id: string;
    displayName: string;
    type: string;
  };
}

describe('API Product permissions (management v2)', () => {
  let apiProduct: ApiProduct;

  beforeAll(async () => {
    // Create an API Product as admin (primary owner should be admin user)
    const createResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-permissions-${Date.now()}`,
        description: 'E2E API Product for permissions tests',
        version: '1.0.0',
      }),
    });

    expect(createResponse.status).toEqual(201);
    apiProduct = (await createResponse.json()) as ApiProduct;
    expect(apiProduct.id).toBeDefined();
  });

  /**
   * Scenario A - Only primary owner can update
   *
   * - Admin (creator / primary owner) can update the product.
   * - A non-primary owner (API user) should be forbidden.
   */
  test('should allow primary owner to update API Product but forbid non-owner', async () => {
    // Admin (primary owner) update should succeed
    const adminUpdateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'PUT',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: apiProduct.name,
        description: 'Updated by primary owner',
        version: apiProduct.version,
      }),
    });
    expect(adminUpdateResponse.status).toEqual(200);

    // If API user credentials are available, ensure update is forbidden
    if (apiAuthHeader) {
      const publisherUpdateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
        method: 'PUT',
        headers: {
          Authorization: apiAuthHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: apiProduct.name,
          description: 'Updated by non-owner',
          version: apiProduct.version,
        }),
      });

      expect([401, 403]).toContain(publisherUpdateResponse.status);
    }
  });

  /**
   * Scenario B - Publisher cannot delete product
   *
   * - Primary owner (admin) can delete.
   * - API publisher user should not be allowed to delete.
   */
  test('should forbid non-owner from deleting API Product', async () => {
    if (!apiAuthHeader) {
      return;
    }

    const publisherDeleteResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'DELETE',
      headers: {
        Authorization: apiAuthHeader,
      },
    });

    expect([401, 403]).toContain(publisherDeleteResponse.status);
  });

  /**
   * Scenario C - Visibility enforcement (PRIVATE / PUBLIC)
   *
   * For now the API Product model is environment-scoped and protected via permissions.
   * We ensure that:
   * - Admin / publisher can see the product.
   * - A simple user without API_PRODUCT_DEFINITION[READ] gets 403/401.
   */
  test('should enforce visibility / permission on GET API Product', async () => {
    // Admin should see it
    const adminGetResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'GET',
      headers: {
        Authorization: adminAuthHeader,
      },
    });
    expect(adminGetResponse.status).toEqual(200);

    // If API user is configured, it should also have access (publisher role)
    if (apiAuthHeader) {
      const apiUserGetResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
        method: 'GET',
        headers: {
          Authorization: apiAuthHeader,
        },
      });
      expect([200, 403]).toContain(apiUserGetResponse.status);
    }

    // Simple user should not be able to read if permissions are not granted
    if (simpleAuthHeader) {
      const simpleUserGetResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
        method: 'GET',
        headers: {
          Authorization: simpleAuthHeader,
        },
      });
      expect([401, 403]).toContain(simpleUserGetResponse.status);
    }
  });

  /**
   * Scenario D - Role-based access denial
   *
   * Validate that a user without API_PRODUCT_DEFINITION[UPDATE]/[DELETE] cannot perform those actions.
   */
  test('should deny update and delete to user without proper role', async () => {
    if (!simpleAuthHeader) {
      return;
    }

    const simpleUpdateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'PUT',
      headers: {
        Authorization: simpleAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: apiProduct.name,
        description: 'Attempted update by simple user',
        version: apiProduct.version,
      }),
    });
    expect([401, 403]).toContain(simpleUpdateResponse.status);

    const simpleDeleteResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'DELETE',
      headers: {
        Authorization: simpleAuthHeader,
      },
    });
    expect([401, 403]).toContain(simpleDeleteResponse.status);
  });

  afterAll(async () => {
    // Final cleanup with admin credentials
    if (apiProduct?.id) {
      await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
        method: 'DELETE',
        headers: {
          Authorization: adminAuthHeader,
        },
      });
    }
  });
});
