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
import { ApiApi } from '@portal-apis/ApiApi';
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
import { ApiEntity } from '@management-models/ApiEntity';
import { fail, succeed } from '@lib/jest-utils';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { SearchableUser } from '@management-models/SearchableUser';
import { UsersApi } from '@management-apis/UsersApi';
import { cloneDeep, find } from 'lodash';
import { APIMembershipsApi } from '@management-apis/APIMembershipsApi';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsApiUser = new APIsApi(forManagementAsApiUser());
const usersResourceAsApiUser = new UsersApi(forManagementAsApiUser());
const apiMembershipsResourceAsApiUser = new APIMembershipsApi(forManagementAsApiUser());

const apiResourceAsAnonymous = new ApiApi(forPortalAsAnonymous());
const apiResourceAsAdmin = new ApiApi(forPortalAsAdminUser());
const apiResourceAsSimpleUser = new ApiApi(forPortalAsSimpleUser());
const apiResourceAsApiUser = new ApiApi(forPortalAsApiUser());
const apiResourceAsAppUser = new ApiApi(forPortalAsAppUser());

let createdApi: ApiEntity;
let publishedApi: ApiEntity;
let userMember: SearchableUser;

describe('API - Visibility', () => {
  beforeAll(async () => {
    // create an API
    createdApi = await apisResourceAsApiUser.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi(),
    });

    // publish it
    publishedApi = await apisResourceAsApiUser.updateApi({
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({ ...createdApi, lifecycle_state: ApiLifecycleState.PUBLISHED }),
      orgId,
      envId,
    });

    // get user member
    const users: SearchableUser[] = await usersResourceAsApiUser.searchUsers({
      envId,
      orgId,
      q: process.env.SIMPLE_USERNAME,
    });
    userMember = find(users, (user) => user.displayName === process.env.SIMPLE_USERNAME);

    // add member user to API
    await apiMembershipsResourceAsApiUser.addOrUpdateApiMember({
      envId,
      orgId,
      api: createdApi.id,
      apiMembership: {
        id: userMember.id,
        reference: userMember.reference,
        role: 'USER',
      },
    });
  });

  describe('Private', () => {
    describe.each`
      user           | apiResource
      ${'ANONYMOUS'} | ${apiResourceAsAnonymous}
      ${'ADMIN'}     | ${apiResourceAsAdmin}
      ${'APP_USER'}  | ${apiResourceAsAppUser}
    `('As $user user', ({ apiResource }: { apiResource: ApiApi }) => {
      test('Get APIs should not contain created api', async () => {
        const apis = await succeed(apiResource.getApisRaw({}));
        expect(apis.data.find((api) => api.id === createdApi.id)).not.toBeDefined();
      });

      test('Get API by id should return not found', async () => {
        await fail(
          apiResource.getApiByApiIdRaw({
            apiId: createdApi.id,
          }),
          404,
        );
      });
    });

    describe.each`
      user             | apiResource
      ${'API_USER'}    | ${apiResourceAsApiUser}
      ${'SIMPLE_USER'} | ${apiResourceAsSimpleUser}
    `('As $user user', ({ apiResource }: { apiResource: ApiApi }) => {
      test('Get APIs should contain created api', async () => {
        const apis = await succeed(apiResource.getApisRaw({}));
        expect(apis.data.find((api) => api.id === createdApi.id)).toBeDefined();
      });

      test('Get API by id should return API', async () => {
        const foundApi = await succeed(
          apiResource.getApiByApiIdRaw({
            apiId: createdApi.id,
          }),
        );

        expect(foundApi.id).toBe(createdApi.id);
        expect(foundApi.running).toBe(false);
        expect(foundApi._public).toBe(false);
        expect(foundApi.draft).toBe(false);
      });
    });
  });

  describe('Public', () => {
    beforeAll(async () => {
      // make API public
      let updatedApi = cloneDeep(createdApi);
      delete updatedApi.id;
      delete updatedApi.state;
      delete updatedApi.created_at;
      delete updatedApi.updated_at;
      delete updatedApi.owner;
      delete updatedApi.context_path;
      updatedApi.lifecycle_state = ApiLifecycleState.PUBLISHED;
      updatedApi.visibility = 'PUBLIC';
      await apisResourceAsApiUser.updateApi({
        envId,
        orgId,
        api: createdApi.id,
        updateApiEntity: updatedApi,
      });
    });

    describe.each`
      user             | apiResource
      ${'API_USER'}    | ${apiResourceAsApiUser}
      ${'SIMPLE_USER'} | ${apiResourceAsSimpleUser}
      ${'ANONYMOUS'}   | ${apiResourceAsAnonymous}
      ${'ADMIN'}       | ${apiResourceAsAdmin}
      ${'APP_USER'}    | ${apiResourceAsAppUser}
    `('As $user user', ({ apiResource }: { apiResource: ApiApi }) => {
      test('Get APIs should contain created api', async () => {
        const apis = await succeed(apiResource.getApisRaw({}));
        expect(apis.data.find((api) => api.id === createdApi.id)).toBeDefined();
      });

      test('Get API by id should return API', async () => {
        const foundApi = await succeed(
          apiResource.getApiByApiIdRaw({
            apiId: createdApi.id,
          }),
        );

        expect(foundApi.id).toBe(createdApi.id);
        expect(foundApi.running).toBe(false);
        expect(foundApi._public).toBe(true);
        expect(foundApi.draft).toBe(false);
      });
    });
  });

  afterAll(async () => {
    // remove member user from API
    await apiMembershipsResourceAsApiUser.deleteApiMember({
      envId,
      orgId,
      api: createdApi.id,
      user: userMember.id,
    });

    await apisResourceAsApiUser.deleteApi({
      envId,
      orgId,
      api: createdApi.id,
    });
  });
});
