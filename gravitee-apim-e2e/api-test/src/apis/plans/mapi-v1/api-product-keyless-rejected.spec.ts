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

describe('API Product plan security - KEY_LESS rejected', () => {
  let productId: string;

  const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
  const adminUsername = process.env.ADMIN_USERNAME;
  const adminPassword = process.env.ADMIN_PASSWORD;

  if (!managementV2BaseUrl) {
    throw new Error('MANAGEMENT_V2_BASE_URL must be defined to run API Product KEY_LESS plan tests');
  }
  if (!adminUsername || !adminPassword) {
    throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined to run API Product KEY_LESS plan tests');
  }

  const adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;

  beforeAll(async () => {
    // Create a minimal API Product to attach the plan to
    const productResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: `e2e-product-keyless-rejection-${Date.now()}`,
        description: 'Product used to validate KEY_LESS plan rejection',
        version: '1.0.0',
      }),
    });

    expect(productResponse.status).toEqual(201);
    const productBody = await productResponse.json();
    productId = productBody.id;
    expect(productId).toBeDefined();
  });

  test('should reject API Product plan with KEY_LESS security', async () => {
    const planResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}/plans`, {
      method: 'POST',
      headers: {
        Authorization: adminAuthHeader,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: 'KEY_LESS plan should be rejected',
        description: 'This plan should not be allowed on API Products',
        order: 1,
        security: {
          type: 'KEY_LESS',
        },
        validation: 'AUTO',
      }),
    });

    expect(planResponse.status).toEqual(400);
    const errorBody = await planResponse.json();
    expect(errorBody.message).toBeDefined();
  });

  afterAll(async () => {
    if (!productId) {
      return;
    }

    await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${productId}`, {
      method: 'DELETE',
      headers: {
        Authorization: adminAuthHeader,
      },
    });
  });
});
