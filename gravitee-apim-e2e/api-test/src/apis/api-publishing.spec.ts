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
import { APIsApi } from '@management-apis/APIsApi';
import {
  forManagementAsApiUser,
  forPortalAsAdminUser,
  forPortalAsAnonymous,
  forPortalAsApiUser,
  forPortalAsAppUser,
  forPortalAsSimpleUser,
} from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiApi } from '@portal-apis/ApiApi';
import { ApiEntity } from '@management-models/ApiEntity';
import { fail, succeed } from '@lib/jest-utils';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { ApiResponse } from '../../../lib/management-webclient-sdk/src/lib/runtime';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceUser = new APIsApi(forManagementAsApiUser());
const apiResourceAnonymous = new ApiApi(forPortalAsAnonymous());
const apiResourceAdmin = new ApiApi(forPortalAsAdminUser());
const apiResourceApiUser = new ApiApi(forPortalAsApiUser());
const apiResourceAppUser = new ApiApi(forPortalAsAppUser());
const apiResourceSimpleUser = new ApiApi(forPortalAsSimpleUser());

let userApi: ApiEntity;
let publishedApi: ApiEntity;

describe('API - Publishing', () => {
  describe('Not published', () => {
    beforeAll(async () => {
      userApi = await apisResourceUser.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi(),
      });
    });

    describe.each`
      user             | apiResource
      ${'ANONYMOUS'}   | ${apiResourceAnonymous}
      ${'ADMIN'}       | ${apiResourceAdmin}
      ${'API_USER'}    | ${apiResourceApiUser}
      ${'APP_USER'}    | ${apiResourceAppUser}
      ${'SIMPLE_USER'} | ${apiResourceSimpleUser}
    `('As $user user', ({ apiResource }: { apiResource: ApiApi }) => {
      test('Get APIs should not contain created api', async () => {
        const apisResponse = await succeed(apiResource.getApisRaw({}));
        expect(apisResponse.data.find((api) => api.id === userApi.id)).not.toBeDefined();
      });

      test('Get API by id should return not found', async () => {
        await fail(
          apiResource.getApiByApiId({
            apiId: userApi.id,
          }),
          404,
        );
      });
    });

    afterAll(async () => {
      await apisResourceUser.deleteApi({ orgId, envId, api: userApi.id });
    });
  });

  describe('Published', () => {
    beforeAll(async () => {
      userApi = await apisResourceUser.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi(),
      });
      publishedApi = await apisResourceUser.updateApi({
        api: userApi.id,
        updateApiEntity: UpdateApiEntityFromJSON({ ...userApi, lifecycle_state: ApiLifecycleState.PUBLISHED }),
        orgId,
        envId,
      });
    });

    describe.each`
      user             | apiResource
      ${'ANONYMOUS'}   | ${apiResourceAnonymous}
      ${'ADMIN'}       | ${apiResourceAdmin}
      ${'APP_USER'}    | ${apiResourceAppUser}
      ${'SIMPLE_USER'} | ${apiResourceSimpleUser}
    `('As $user user', ({ apiResource }: { apiResource: ApiApi }) => {
      test('Get APIs should not contain created api', async () => {
        const apisResponse = await succeed(apiResource.getApisRaw({}));
        expect(apisResponse.data.find((api) => api.id === userApi.id)).not.toBeDefined();
      });

      test('Get API by id should return not found', async () => {
        await fail(
          apiResource.getApiByApiId({
            apiId: userApi.id,
          }),
          404,
        );
      });
    });

    describe('As API_USER user', () => {
      test('Get APIs should contain created api', async () => {
        const apisResponse = await succeed(apiResourceApiUser.getApisRaw({}));
        expect(apisResponse.data.find((api) => api.id === userApi.id)).toBeDefined();
      });

      test('Get API by id should return the api', async () => {
        const apiResponse = await succeed(
          apiResourceApiUser.getApiByApiIdRaw({
            apiId: userApi.id,
          }),
        );

        expect(apiResponse).toEqual(
          expect.objectContaining({
            id: userApi.id,
            running: false,
            _public: false,
            draft: false,
          }),
        );
      });
    });

    afterAll(async () => {
      await apisResourceUser.deleteApi({ orgId, envId, api: userApi.id });
    });
  });
});
