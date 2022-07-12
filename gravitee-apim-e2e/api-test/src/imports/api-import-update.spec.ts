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

import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser } from '@client-conf/*';
import { fail, succeed } from '@lib/jest-utils';
import { Visibility } from '@management-models/Visibility';
import { ApisFaker } from '@management-fakers/ApisFaker';

const apisResource = new APIsApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Update API by importing it', () => {
  describe('Update API which ID in URL does not exist', () => {
    const api = ApisFaker.apiImport({ id: 'unknown-test-id' });

    test('should fail to update API, returning 404', async () => {
      await fail(
        apisResource.updateApiWithDefinition({
          envId,
          orgId,
          api: 'unknown-test-id',
          body: api,
        }),
        404,
        'Api [unknown-test-id] can not be found.',
      );
    });
  });

  describe('Update API with an ID in URL that exists, without ID in body', () => {
    const apiId = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
    const expectedApiId = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

    const fakeCreateApi = ApisFaker.apiImport({ id: apiId });

    // update API data. body doesn't contains API id
    const fakeUpdateApi = ApisFaker.apiImport({
      name: 'updatedName',
      version: '1.1',
      description: 'Updated API description',
      visibility: Visibility.PUBLIC,
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/updated/path',
          },
        ],
      }),
    });

    test('should create API generating a predictable id', async () => {
      await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi }));
    });

    test('should update the API with the generated ID, even if no ID in body', async () => {
      let updatedApi = await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: fakeUpdateApi }));
      expect(updatedApi.id).toBe(expectedApiId);
    });

    test('should get updated API with updated data', async () => {
      let foundApi = await apisResource.getApi({ envId, orgId, api: expectedApiId });
      expect(foundApi.id).toBe(expectedApiId);
      expect(foundApi.name).toBe('updatedName');
      expect(foundApi.version).toBe('1.1');
      expect(foundApi.description).toBe('Updated API description');
      expect(foundApi.visibility).toBe(Visibility.PUBLIC);
      expect(foundApi.proxy.virtual_hosts[0].path).toBe('/updated/path');
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API with an ID in URL that exists, with another API ID in body', () => {
    const apiId1 = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
    const expectedApiId1 = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

    const fakeCreateApi1 = ApisFaker.apiImport({ id: apiId1, name: 'originalName' });

    const apiId2 = '662712f4-8364-4e6b-825f-2008d59cc684';
    const expectedApiId2 = 'cb6cf46a-f396-3f80-9681-da23d9f2965c';

    const fakeCreateApi2 = ApisFaker.apiImport({ id: apiId2, name: 'originalName' });

    // that will update api2, with api1 id in body
    const fakeUpdateApi = ApisFaker.apiImport({ id: apiId1, name: 'updatedName' });

    test('should create API 1', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi1 }));
      expect(createdApi.id).toBe(expectedApiId1);
    });

    test('should create API 2', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi2 }));
      expect(createdApi.id).toBe(expectedApiId2);
    });

    test('should update API 2, event if api1 id in body', async () => {
      let updatedApi = await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId2, body: fakeUpdateApi }));
      expect(updatedApi.id).toBe(expectedApiId2);
    });

    test('should get API1 with unchanged data', async () => {
      let foundApi = await apisResource.getApi({ envId, orgId, api: expectedApiId1 });
      expect(foundApi.name).toBe('originalName');
    });

    test('should get API2 with updated data', async () => {
      let foundApi = await apisResource.getApi({ envId, orgId, api: expectedApiId2 });
      expect(foundApi.name).toBe('updatedName');
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId1 });
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId2 });
    });
  });

  describe('Update API with an updated context path matching another API context path', () => {
    const fakeCreateApi1 = ApisFaker.apiImport({
      id: '67d8020e-b0b3-47d8-9802-0eb0b357d84c',
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/importTest1',
          },
        ],
      }),
    });
    const expectedApiId1 = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

    const fakeCreateApi2 = ApisFaker.apiImport({
      id: '72dd3b21-b0cc-44a8-87d3-23e1f52b61fa',
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/importTest2',
          },
        ],
      }),
    });
    const expectedApiId2 = 'e6c25ef8-0946-3ea8-826b-4384aeda865c';

    // that will try to update api2, with the same context path as api1
    const fakeUpdateApi = ApisFaker.apiImport({
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/importTest1',
          },
        ],
      }),
    });

    test('should create a first API, generating a predictable ID', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi1 }));
      expect(createdApi.id).toBe(expectedApiId1);
    });

    test('should create a second API, generating another predictable ID', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi2 }));
      expect(createdApi.id).toBe(expectedApiId2);
    });

    test('should fail to update API 2 with same context path as API 1', async () => {
      await fail(
        apisResource.updateApiWithDefinition({ envId, orgId, api: expectedApiId2, body: fakeUpdateApi }),
        400,
        'The path [/importTest1/] is already covered by an other API.',
      );
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId1 });
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId2 });
    });
  });
});
