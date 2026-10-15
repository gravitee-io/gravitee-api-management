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
import { afterAll, beforeAll, expect, test } from '@jest/globals';
import 'dotenv/config';
import { APIsApi, ApiType, ApiV4, PlanMode, PlanSecurityType } from '../../../../../lib/management-v2-webclient-sdk/src/lib';
import { adminAuthHeader, apiAuthHeader, envId, managementV2BaseUrl } from '@gravitee/utils/api-products';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { created, describeIfClientGatewaySupportingApiProduct } from '@lib/jest-utils';
import { faker } from '@faker-js/faker';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

interface ApiProduct {
  id: string;
}

interface ApiProductPlan {
  id: string;
  name: string;
  description?: string;
  status?: string;
  security: { type: string };
}

/**
 * An API owner holds plan rights on their own API only. The API plan endpoints must never reach an API Product plan,
 * whichever id is used in the path.
 */
describeIfClientGatewaySupportingApiProduct('API Product plans are out of reach of the API plan endpoints (management v2)', () => {
  const callerAuthHeader = apiAuthHeader || adminAuthHeader;

  let productApi: ApiV4;
  let callerApi: ApiV4;
  let apiProduct: ApiProduct;
  let productPlan: ApiProductPlan;

  const createApi = async (namePrefix: string, allowedInApiProducts: boolean): Promise<ApiV4> => {
    const path = `/${namePrefix}-${faker.string.alphanumeric(8).toLowerCase()}`;
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
        allowedInApiProducts,
      }),
    });
    return (await created(
      v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
        envId,
        exportApiV4: apiImport,
      }),
    )) as ApiV4;
  };

  const apiPlanUrl = (suffix = '') => `${managementV2BaseUrl}/environments/${envId}/apis/${callerApi.id}/plans/${productPlan.id}${suffix}`;
  const apiProductsUrl = `${managementV2BaseUrl}/environments/${envId}/api-products`;

  const asCaller = (method: string, url: string, body?: unknown) =>
    fetch(url, {
      method,
      headers: { Authorization: callerAuthHeader, 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });

  beforeAll(async () => {
    productApi = await createApi('plan-ownership-product', true);
    callerApi = await createApi('plan-ownership-caller', false);

    const createProductResponse = await fetch(apiProductsUrl, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `e2e-plan-ownership-${Date.now()}`,
        description: 'E2E API Product for plan ownership tests',
        version: '1.0.0',
        apiIds: [productApi.id],
      }),
    });
    expect(createProductResponse.status).toEqual(201);
    apiProduct = (await createProductResponse.json()) as ApiProduct;

    const planPayload = MAPIV2PlansFaker.newPlanV4({
      name: `e2e-plan-ownership-${Date.now()}`,
      description: 'Plan of the API Product, must not be reachable from an API',
      order: 1,
      mode: PlanMode.STANDARD,
      security: { type: PlanSecurityType.API_KEY },
      validation: 'AUTO',
    });
    const createPlanResponse = await fetch(`${apiProductsUrl}/${apiProduct.id}/plans`, {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify(planPayload),
    });
    expect(createPlanResponse.status).toEqual(201);
    productPlan = (await createPlanResponse.json()) as ApiProductPlan;
  });

  test('should not read an API Product plan through an API plan endpoint', async () => {
    const response = await asCaller('GET', apiPlanUrl());
    expect(response.status).toEqual(404);
  });

  test('should not update an API Product plan through an API plan endpoint', async () => {
    const response = await asCaller('PUT', apiPlanUrl(), {
      definitionVersion: 'V4',
      name: productPlan.name,
      description: 'Hijacked description',
      security: productPlan.security,
      mode: 'STANDARD',
      validation: 'AUTO',
    });
    expect(response.status).toEqual(404);
  });

  test('should not publish, deprecate or close an API Product plan through an API plan endpoint', async () => {
    for (const action of ['_publish', '_deprecate', '_close']) {
      const response = await asCaller('POST', apiPlanUrl(`/${action}`));
      expect(response.status).toEqual(404);
    }
  });

  test('should not delete an API Product plan through an API plan endpoint', async () => {
    const response = await asCaller('DELETE', apiPlanUrl());
    expect(response.status).toEqual(404);
  });

  test('should leave the API Product plan untouched', async () => {
    const response = await fetch(`${apiProductsUrl}/${apiProduct.id}/plans/${productPlan.id}`, {
      method: 'GET',
      headers: { Authorization: adminAuthHeader },
    });
    expect(response.status).toEqual(200);
    const plan = (await response.json()) as ApiProductPlan;
    expect(plan.status).toEqual(productPlan.status);
    expect(plan.description).toEqual(productPlan.description);
  });

  afterAll(async () => {
    if (apiProduct?.id) {
      await fetch(`${apiProductsUrl}/${apiProduct.id}`, { method: 'DELETE', headers: { Authorization: adminAuthHeader } });
    }
    for (const api of [productApi, callerApi]) {
      if (api?.id) {
        await v2ApisResourceAsApiPublisher.deleteApiRaw({ envId, apiId: api.id });
      }
    }
  });
});
