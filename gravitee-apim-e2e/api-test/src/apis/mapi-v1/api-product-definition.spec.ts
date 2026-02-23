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
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';

const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
const adminUsername = process.env.ADMIN_USERNAME;
const adminPassword = process.env.ADMIN_PASSWORD;

if (!managementV2BaseUrl) {
  throw new Error('MANAGEMENT_V2_BASE_URL must be defined to run API Product e2e tests');
}
if (!adminUsername || !adminPassword) {
  throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined to run API Product e2e tests');
}

const authHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

interface ApiProduct {
  id: string;
  environmentId: string;
  name: string;
  description?: string;
  version: string;
}

describe('API Product definition (management v2)', () => {
  let apiProduct: ApiProduct;
  const apiProductName = `e2e-product-${faker.string.uuid()}`;

  beforeAll(async () => {
    // First verify the name is available
    const verifyResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/_verify`, {
      method: 'POST',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: apiProductName,
      }),
    });

    expect(verifyResponse.status).toEqual(200);
    const verifyBody = (await verifyResponse.json()) as { ok: boolean; reason?: string };
    expect(verifyBody.ok).toBe(true);

    // Then create the API Product
    const createResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: apiProductName,
        description: 'E2E API Product for definition tests',
        version: '1.0.0',
      }),
    });

    expect(createResponse.status).toEqual(201);
    apiProduct = (await createResponse.json()) as ApiProduct;
    expect(apiProduct.id).toBeDefined();
    expect(apiProduct.name).toEqual(apiProductName);
    expect(apiProduct.version).toEqual('1.0.0');
    expect(apiProduct.environmentId).toEqual(envId);
  });

  test('should get API Product by id', async () => {
    const getResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'GET',
      headers: {
        Authorization: authHeader,
      },
    });

    expect(getResponse.status).toEqual(200);
    const body = (await getResponse.json()) as ApiProduct;
    expect(body.id).toEqual(apiProduct.id);
    expect(body.name).toEqual(apiProductName);
  });

  test('should list API Products and contain created product', async () => {
    const listResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'GET',
      headers: {
        Authorization: authHeader,
      },
    });

    expect(listResponse.status).toEqual(200);
    const listBody = (await listResponse.json()) as {
      data: ApiProduct[];
      pagination: { totalCount: number };
    };

    expect(Array.isArray(listBody.data)).toBe(true);
    expect(listBody.data.find((p) => p.id === apiProduct.id)).toBeDefined();
  });

  test('should update API Product', async () => {
    const updatedDescription = 'Updated E2E API Product description';

    const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'PUT',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: apiProductName,
        description: updatedDescription,
        version: '1.0.1',
      }),
    });

    expect(updateResponse.status).toEqual(200);
    const body = (await updateResponse.json()) as ApiProduct;
    expect(body.description).toEqual(updatedDescription);
    expect(body.version).toEqual('1.0.1');
  });

  test('should deploy API Product', async () => {
    const deployResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/deployments`, {
      method: 'POST',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({}),
    });

    expect(deployResponse.status).toEqual(202);
  });

  test('should detect duplicate API Product name', async () => {
    const verifyResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/_verify`, {
      method: 'POST',
      headers: {
        Authorization: authHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: apiProductName,
      }),
    });

    expect(verifyResponse.status).toEqual(200);
    const verifyBody = (await verifyResponse.json()) as { ok: boolean; reason?: string };
    expect(verifyBody.ok).toBe(false);
    expect(verifyBody.reason).toBeDefined();
  });

  afterAll(async () => {
    if (!apiProduct?.id) {
      return;
    }

    const deleteResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'DELETE',
      headers: {
        Authorization: authHeader,
      },
    });

    // Deletion may fail if feature is not enabled or already deleted; this should not make the suite fail
    expect([200, 204, 400, 404]).toContain(deleteResponse.status);
  });
});
