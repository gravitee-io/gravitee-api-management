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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { fail, succeed } from '../../lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());

describe('API - Imports', () => {
  describe('Create API from import', () => {
    describe('Create API without ID', () => {
      let createdApi;

      beforeAll(async () => {
        createdApi = await apisResourceAsPublisher.importApiDefinition({
          envId,
          orgId,
          body: ApisFaker.apiImport(),
        });
      });

      test('should get created API with generated ID', async () => {
        const api = await succeed(
          apisResourceAsPublisher.getApiRaw({
            envId,
            orgId,
            api: createdApi.id,
          }),
        );
        expect(api).toBeTruthy();
        expect(api.id).toStrictEqual(createdApi.id);
      });

      afterAll(async () => {
        await apisResourceAsPublisher.deleteApi({
          envId,
          orgId,
          api: createdApi.id,
        });
      });
    });

    describe('Create empty API with an already existing context path', () => {
      const apiId1 = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
      const generatedApiId = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

      const fakeApi1 = ApisFaker.apiImport({ id: apiId1 });
      fakeApi1.proxy.virtual_hosts[0].path = '/testimport/';

      // api2 has different ID, but same context path as api 1
      const apiId2 = '67d8020e-b0b3-47d8-9802-0eb0b357d84d';
      const fakeApi2 = ApisFaker.apiImport({ id: apiId2 });
      fakeApi2.proxy.virtual_hosts[0].path = '/testimport/';

      test('should create API with the specified ID', async () => {
        const api = await succeed(
          apisResourceAsPublisher.importApiDefinitionRaw({
            envId,
            orgId,
            body: fakeApi1,
          }),
        );

        expect(api).toBeTruthy();
        expect(api.id).toStrictEqual(generatedApiId);
      });

      test('should fail to create API with the same context path', async () => {
        await fail(
          apisResourceAsPublisher.importApiDefinitionRaw({
            envId,
            orgId,
            body: fakeApi2,
          }),
          400,
          'The path [/testimport/] is already covered by an other API.',
        );
      });

      afterAll(async () => {
        await apisResourceAsPublisher.deleteApi({
          envId,
          orgId,
          api: generatedApiId,
        });
      });
    });
  });
});
