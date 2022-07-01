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
import { succeed } from '@lib/jest-utils';
import { ApiMetadataFormat, ApisFaker } from '@management-fakers/ApisFaker';

const apisResource = new APIsApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Update API from import with metadata', () => {
  describe('Update API with some metadata having key that already exists', () => {
    const expectedApiId = 'f1def2c0-07d6-329d-8976-0d6191ee0f4c';

    const fakeApi = ApisFaker.apiImport({
      id: '9c4b1371-ecf5-4a04-8d8c-99a62ae0216a',
      metadata: [
        {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Ops',
        },
      ],
    });

    let apiUpdate;

    test('should create an API with some metadata having a key with value "team"', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should get the API metadata', async () => {
      let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
      expect(foundMetadata).toHaveLength(2);
      expect(foundMetadata).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Ops',
            apiId: expectedApiId,
          }),
        ]),
      );
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update the API metadata having the key "team"', async () => {
      apiUpdate.metadata = [
        {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'DevOps',
        },
      ];

      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
    });

    test('should get the updated API metadata', async () => {
      let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
      expect(foundMetadata).toHaveLength(2);
      expect(foundMetadata).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'DevOps',
            apiId: expectedApiId,
          }),
        ]),
      );
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API with metadata having key that does not yet exist', () => {
    const expectedApiId = 'bf327615-1733-3d35-8614-c6713e87a812';

    const fakeApi = ApisFaker.apiImport({ id: '4fb4f3d7-e556-421c-b03f-5b2d3da3e774' });

    let apiUpdate;

    test('should create an API with no metadata', async () => {
      await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update API with some metadata having a key that does not yet exist', async () => {
      apiUpdate.metadata = [
        {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Info Sec',
        },
      ];

      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
    });

    test('should get the created API metadata', async () => {
      let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
      expect(foundMetadata).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Info Sec',
            apiId: expectedApiId,
          }),
        ]),
      );
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API with metadata having an undefined key', () => {
    const expectedApiId = '1a308f74-38d3-3bab-8202-eec1d8c3754c';

    const fakeApi = ApisFaker.apiImport({
      id: 'a67e7015-224c-4c32-abaa-231f58d4e542',
    });

    let apiUpdate;

    test('should create an API with no metadata', async () => {
      await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update the API, adding metadata with an undefined key', async () => {
      apiUpdate.metadata = [
        {
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Product',
        },
      ];

      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
    });

    test('should get the API metadata', async () => {
      let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
      expect(foundMetadata[0]).toEqual({
        name: 'team',
        key: 'team',
        format: ApiMetadataFormat.STRING,
        value: 'Product',
        apiId: expectedApiId,
      });
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });
});
