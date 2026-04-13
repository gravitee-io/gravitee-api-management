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
import { afterAll, afterEach, beforeAll, describe, expect, test } from '@jest/globals';
import { APIsApi, ApiV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsAdminUser } from '@gravitee/utils/configuration';
import * as openapiv3 from '@api-test-resources/openapi-withExtensions.json';
import { created, fail, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { faker } from '@faker-js/faker';

const envId = 'DEFAULT';

const v2ApisResource = new APIsApi(forManagementV2AsAdminUser());

describe('API - V4 - Update via Swagger/OpenAPI import', () => {
  const specification = JSON.stringify(openapiv3);

  describe('Update an existing API from an OpenAPI specification', () => {
    let createdApi: ApiV4;

    beforeAll(async () => {
      createdApi = await created(
        v2ApisResource.createApiFromSwaggerRaw({
          envId,
          importSwaggerDescriptor: { payload: specification },
        }),
      );
    });

    test('should update the API via PUT /_import/swagger', async () => {
      const updatedApi = await succeed(
        v2ApisResource.updateApiFromSwaggerRaw({
          envId,
          apiId: createdApi.id,
          importSwaggerDescriptor: { payload: specification },
        }),
      );

      expect(updatedApi).toBeTruthy();
      expect(updatedApi.id).toStrictEqual(createdApi.id);
      expect(updatedApi.name).toBe('Gravitee.io Swagger API');
      expect(updatedApi.apiVersion).toBe('1.2.3');
    });

    test('should get updated API with data from the specification', async () => {
      const fetchedApi = await succeed(
        v2ApisResource.getApiRaw({
          envId,
          apiId: createdApi.id,
        }),
      );
      expect(fetchedApi.id).toStrictEqual(createdApi.id);
      expect(fetchedApi.name).toBe('Gravitee.io Swagger API');
    });

    afterAll(async () => {
      if (createdApi) {
        await noContent(v2ApisResource.deleteApiRaw({ envId, apiId: createdApi.id }));
      }
    });
  });

  describe('Update a non-existent API from Swagger returns 404', () => {
    test('should fail with 404 when API does not exist', async () => {
      await fail(
        v2ApisResource.updateApiFromSwaggerRaw({
          envId,
          apiId: faker.string.uuid(),
          importSwaggerDescriptor: { payload: specification },
        }),
        404,
      );
    });
  });

  describe('Update API from Swagger preserves the API ID', () => {
    let importedApi: ApiV4;

    afterEach(async () => {
      if (importedApi) {
        await noContent(v2ApisResource.deleteApiRaw({ envId, apiId: importedApi.id }));
      }
    });

    test('should keep the same API ID after update', async () => {
      importedApi = await created(
        v2ApisResource.createApiFromSwaggerRaw({
          envId,
          importSwaggerDescriptor: { payload: specification },
        }),
      );

      const updatedApi = await succeed(
        v2ApisResource.updateApiFromSwaggerRaw({
          envId,
          apiId: importedApi.id,
          importSwaggerDescriptor: { payload: specification },
        }),
      );

      expect(updatedApi.id).toStrictEqual(importedApi.id);
    });

    test('should update the API flows from the specification', async () => {
      importedApi = await created(
        v2ApisResource.createApiFromSwaggerRaw({
          envId,
          importSwaggerDescriptor: { payload: specification },
        }),
      );

      const updatedApi = await succeed(
        v2ApisResource.updateApiFromSwaggerRaw({
          envId,
          apiId: importedApi.id,
          importSwaggerDescriptor: { payload: specification },
        }),
      );

      expect(updatedApi.flows).toBeDefined();
      expect(updatedApi.flows.length).toBeGreaterThan(0);
    });

    test('should preserve flow ids when re-importing the same OpenAPI', async () => {
      importedApi = await created(
        v2ApisResource.createApiFromSwaggerRaw({
          envId,
          importSwaggerDescriptor: { payload: specification },
        }),
      );

      const beforeUpdate = await succeed(
        v2ApisResource.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      const flowIdsBefore = ('flows' in beforeUpdate ? (beforeUpdate.flows ?? []) : []).map((flow) => flow.id);

      await succeed(
        v2ApisResource.updateApiFromSwaggerRaw({
          envId,
          apiId: importedApi.id,
          importSwaggerDescriptor: { payload: specification },
        }),
      );

      const afterUpdate = await succeed(
        v2ApisResource.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      const flowIdsAfter = ('flows' in afterUpdate ? (afterUpdate.flows ?? []) : []).map((flow) => flow.id);

      expect(flowIdsBefore.length).toBeGreaterThan(0);
      expect(flowIdsAfter.length).toEqual(flowIdsBefore.length);
      expect(new Set(flowIdsAfter)).toEqual(new Set(flowIdsBefore));
    });
  });
});
