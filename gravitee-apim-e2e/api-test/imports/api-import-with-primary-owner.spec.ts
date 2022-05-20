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
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsSimpleUser } from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { CurrentUserApi } from '@management-apis/CurrentUserApi';
import { UserDetails } from '@management-models/UserDetails';
import { created, succeed } from '../../lib/jest-utils';
import { GroupsApi } from '@management-apis/GroupsApi';
import { GroupsFaker } from '@management-fakers/GroupsFaker';
import { GroupEntity } from '@management-models/GroupEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsAdminUser = new APIsApi(forManagementAsAdminUser());
const groupsResourceAsAdminUser = new GroupsApi(forManagementAsAdminUser());
const currentUserResourceAsSimpleUser = new CurrentUserApi(forManagementAsSimpleUser());

describe('API - Imports with primary owner', () => {
  describe('Create API with primary owner of type "USER", already existing with same id', () => {
    const apiId = '92d900c3-7497-4739-bb98-a8f3615c2773';
    const expectedApiId = '32961b80-aec9-3e1e-b9d5-b1e7e596f407';
    let user: UserDetails;

    beforeAll(async () => {
      user = await currentUserResourceAsSimpleUser.getCurrentUser({ orgId });
    });

    test('should create an API with user "user" as a primary owner, omitting to use the display name', async () => {
      const fakeApi = ApisFaker.apiImport({
        id: apiId,
        primaryOwner: { id: user.id, type: 'USER', displayName: 'Not to be used', email: user.email },
      });

      const api = await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: fakeApi,
        }),
      );
      expect(api).toBeTruthy();
      expect(api.owner).toBeTruthy();
      expect(api.owner.displayName).toStrictEqual('user');
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: expectedApiId,
      });
    });
  });

  describe('Create API with primary owner of type "USER", not existing with same id', () => {
    const apiId = 'df41e94d-ddd5-47b5-a9d8-973fefc4118f';
    const expectedApiId = '55dd7d69-a7a4-31fb-b96a-8d4338c25d82';
    const userId = 'i-do-not-exist';

    test('should create an API, falling back to authenticated user as a primary owner', async () => {
      const fakeApi = ApisFaker.apiImport({
        id: apiId,
        primaryOwner: {
          id: userId,
          type: 'USER',
          displayName: 'Not to be used',
        },
      });

      const api = await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: fakeApi,
        }),
      );
      expect(api).toBeTruthy();
      expect(api.owner).toBeTruthy();
      expect(api.owner.displayName).toStrictEqual('admin');
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: expectedApiId,
      });
    });
  });

  describe('Create API with primary owner of type "GROUP", already existing with same id', () => {
    const groupName = 'R&D';
    const apiId = '5446abb0-0199-4303-8b1a-fff57b0a9eac';
    const expectedApiId = 'cc28bec3-598a-3728-ad19-b5c4b6c67fca';

    const fakeGroup = GroupsFaker.newGroup({ name: groupName });
    let createdGroup: GroupEntity;
    test('should create a group with name "R&D"', async () => {
      createdGroup = await created(
        groupsResourceAsAdminUser.createGroupRaw({
          envId,
          orgId,
          newGroupEntity: fakeGroup,
        }),
      );
      expect(createdGroup).toBeTruthy();
      expect(createdGroup.name).toStrictEqual('R&D');
      expect(createdGroup.id).toBeDefined();
    });

    test('should create an API with the "R&D" group as a primary owner, omitting to use the display name', async () => {
      // We have to set members to null because of #6808
      const fakeApi = ApisFaker.apiImport({
        id: apiId,
        members: null,
        primaryOwner: {
          id: createdGroup.id,
          type: 'GROUP',
          displayName: 'Not to be used',
        },
      });
      const api = await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: fakeApi,
        }),
      );
      expect(api).toBeTruthy();
      expect(api.owner).toBeTruthy();
      expect(api.owner.displayName).toStrictEqual('R&D');
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: expectedApiId,
      });

      await groupsResourceAsAdminUser.deleteGroup({ orgId, envId, group: createdGroup.id });
    });
  });

  describe('Create API with primary owner of type "GROUP", not existing with same id', () => {
    const apiId = 'b12f4414-76e1-425c-98e9-6f0c89c5b52d';
    const expectedApiId = 'd3a8a8d5-76f4-3ee4-bbfc-ffa031af89bc';
    const groupId = 'i-do-not-exist';

    test('should create an API, falling back to "admin" as an API owner', async () => {
      const fakeApi = ApisFaker.apiImport({
        id: apiId,
        primaryOwner: {
          id: groupId,
          type: 'GROUP',
        },
      });

      const api = await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: fakeApi,
        }),
      );
      expect(api).toBeTruthy();
      expect(api.owner).toBeTruthy();
      expect(api.owner.displayName).toStrictEqual('admin');
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: expectedApiId,
      });
    });
  });
});
