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
import { afterAll, describe, expect, test } from '@jest/globals';
import { APIsApi, ApiV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, fail, noContent, succeed } from '@lib/jest-utils';

const envId = 'DEFAULT';

const v2ApisResource = new APIsApi(forManagementV2AsApiUser());

describe('API - V4 - Update via definition import', () => {
  describe('Update an existing PROXY API with a new definition', () => {
    let createdApi: ApiV4;
    const originalDefinition = MAPIV2ApisFaker.apiImportV4();

    test('should create a v4 PROXY API to update later', async () => {
      createdApi = await created(
        v2ApisResource.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: originalDefinition,
        }),
      );
      expect(createdApi).toBeTruthy();
      expect(createdApi.id).toBeTruthy();
    });

    test('should update the PROXY API via PUT /_import/definition', async () => {
      const updatedDefinition = MAPIV2ApisFaker.apiImportV4({
        api: MAPIV2ApisFaker.apiV4Proxy({
          name: 'Updated API Name',
          apiVersion: '2.0.0',
          description: 'Updated via definition import',
          listeners: createdApi.listeners,
        }),
      });

      const updatedApi = await succeed(
        v2ApisResource.updateApiWithDefinitionRaw({
          envId,
          apiId: createdApi.id,
          exportApiV4: updatedDefinition,
        }),
      );

      expect(updatedApi).toBeTruthy();
      expect(updatedApi.id).toStrictEqual(createdApi.id);
      expect(updatedApi.name).toBe('Updated API Name');
      expect(updatedApi.apiVersion).toBe('2.0.0');
      expect(updatedApi.description).toBe('Updated via definition import');
    });

    test('should get updated PROXY API with new data', async () => {
      const fetchedApi = await succeed(
        v2ApisResource.getApiRaw({
          envId,
          apiId: createdApi.id,
        }),
      );
      expect(fetchedApi.id).toStrictEqual(createdApi.id);
      expect(fetchedApi.name).toBe('Updated API Name');
      expect(fetchedApi.apiVersion).toBe('2.0.0');
    });

    afterAll(async () => {
      if (createdApi) {
        await noContent(v2ApisResource.deleteApiRaw({ envId, apiId: createdApi.id }));
      }
    });
  });

  describe('Update an existing MESSAGE API with a new definition', () => {
    let createdApi: ApiV4;

    test('should create a v4 MESSAGE API to update later', async () => {
      createdApi = await created(
        v2ApisResource.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            api: MAPIV2ApisFaker.apiV4Message(),
          }),
        }),
      );
      expect(createdApi).toBeTruthy();
      expect(createdApi.id).toBeTruthy();
      expect(createdApi.type).toBe('MESSAGE');
    });

    test('should update the MESSAGE API via PUT /_import/definition', async () => {
      const updatedDefinition = MAPIV2ApisFaker.apiImportV4({
        api: MAPIV2ApisFaker.apiV4Message({
          name: 'Updated Message API',
          apiVersion: '3.0.0',
          description: 'Updated message API via definition import',
          listeners: createdApi.listeners,
        }),
      });

      const updatedApi = await succeed(
        v2ApisResource.updateApiWithDefinitionRaw({
          envId,
          apiId: createdApi.id,
          exportApiV4: updatedDefinition,
        }),
      );

      expect(updatedApi).toBeTruthy();
      expect(updatedApi.id).toStrictEqual(createdApi.id);
      expect(updatedApi.name).toBe('Updated Message API');
      expect(updatedApi.apiVersion).toBe('3.0.0');
      expect(updatedApi.description).toBe('Updated message API via definition import');
    });

    test('should get updated MESSAGE API with new data', async () => {
      const fetchedApi = await succeed(
        v2ApisResource.getApiRaw({
          envId,
          apiId: createdApi.id,
        }),
      );
      expect(fetchedApi.id).toStrictEqual(createdApi.id);
      expect(fetchedApi.name).toBe('Updated Message API');
      expect(fetchedApi.apiVersion).toBe('3.0.0');
    });

    afterAll(async () => {
      if (createdApi) {
        await noContent(v2ApisResource.deleteApiRaw({ envId, apiId: createdApi.id }));
      }
    });
  });

  describe('Update API with a conflicting context path returns 400', () => {
    let api1: ApiV4;
    let api2: ApiV4;

    test('should create first API', async () => {
      api1 = await created(
        v2ApisResource.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4(),
        }),
      );
    });

    test('should create second API', async () => {
      api2 = await created(
        v2ApisResource.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4(),
        }),
      );
    });

    test('should fail to update second API with the same context path as first API', async () => {
      await fail(
        v2ApisResource.updateApiWithDefinitionRaw({
          envId,
          apiId: api2.id,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            api: MAPIV2ApisFaker.apiV4Proxy({
              listeners: api1.listeners,
            }),
          }),
        }),
        400,
      );
    });

    afterAll(async () => {
      if (api1) await noContent(v2ApisResource.deleteApiRaw({ envId, apiId: api1.id }));
      if (api2) await noContent(v2ApisResource.deleteApiRaw({ envId, apiId: api2.id }));
    });
  });

  describe('Update API preserves the API ID', () => {
    let createdApi: ApiV4;

    test('should create an API', async () => {
      createdApi = await created(
        v2ApisResource.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4(),
        }),
      );
      expect(createdApi.id).toBeTruthy();
    });

    test('should preserve API ID after update even when definition contains a different id', async () => {
      const updatedDefinition = MAPIV2ApisFaker.apiImportV4({
        api: MAPIV2ApisFaker.apiV4Proxy({
          id: 'some-other-id',
          name: 'API With Different ID In Definition',
          listeners: createdApi.listeners,
        }),
      });

      const updatedApi = await succeed(
        v2ApisResource.updateApiWithDefinitionRaw({
          envId,
          apiId: createdApi.id,
          exportApiV4: updatedDefinition,
        }),
      );

      expect(updatedApi.id).toStrictEqual(createdApi.id);
      expect(updatedApi.name).toBe('API With Different ID In Definition');
    });

    afterAll(async () => {
      if (createdApi) {
        await noContent(v2ApisResource.deleteApiRaw({ envId, apiId: createdApi.id }));
      }
    });
  });
});
