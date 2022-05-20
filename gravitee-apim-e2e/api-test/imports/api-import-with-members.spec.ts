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
import { forManagementAsAdminUser } from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { succeed } from '../../lib/jest-utils';
import { UsersApi } from '@management-apis/UsersApi';
import { UserEntity } from '@management-models/UserEntity';
import { RoleScope } from '@management-models/RoleScope';
import { RoleFaker } from '@management-fakers/RoleFaker';
import { UsersFaker } from '@management-fakers/UsersFaker';
import { RoleEntity } from '@management-models/RoleEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsAdminUser = new APIsApi(forManagementAsAdminUser());
const usersResourceAsAdminUser = new UsersApi(forManagementAsAdminUser());
const configurationsResourceAsAdminUser = new ConfigurationApi(forManagementAsAdminUser());

describe('API - Imports with members', () => {
  const apiId = '721badf2-3563-4a84-b2e8-f69752fe416c';
  const expectedApiId = '01daa02c-b4ec-370e-9d2b-8732f4bf06b7';

  let member: UserEntity;
  let primaryOwner: UserEntity;
  let roleName = 'MY_TEST_ROLE';
  let customRole: RoleEntity;

  beforeAll(async () => {
    // create user future member
    member = await usersResourceAsAdminUser.createUser({
      orgId,
      envId,
      newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
    });

    // create user future primary owner
    primaryOwner = await usersResourceAsAdminUser.createUser({
      orgId,
      envId,
      newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
    });

    // create role
    customRole = await configurationsResourceAsAdminUser.createRole({
      orgId,
      scope: RoleScope.API,
      newRoleEntity: RoleFaker.newRoleEntity({ name: roleName }),
    });
  });

  describe('Create API with member (member role is specified by id)', () => {
    test('should create an API, and associate a member with role, by role id', async () => {
      const fakeApi = ApisFaker.apiImport({
        id: apiId,
        members: [{ source: 'gravitee', sourceId: member.email, roles: [customRole.id] }],
        primaryOwner: { id: primaryOwner.id, type: 'USER', email: primaryOwner.email },
      });
      await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: fakeApi,
        }),
      );
    });

    test('should get API members, with a primary owner, and an additional member with associated role', async () => {
      const members = await succeed(apisResourceAsAdminUser.getApiMembersRaw({ orgId, envId, api: expectedApiId }));
      expect(members).toBeTruthy();
      expect(members).toHaveLength(2);
      expect(members).toEqual(
        expect.arrayContaining([
          { id: primaryOwner.id, displayName: primaryOwner.displayName, role: 'PRIMARY_OWNER' },
          { id: member.id, displayName: member.displayName, role: 'MY_TEST_ROLE' },
        ]),
      );
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        orgId,
        envId,
        api: expectedApiId,
      });
    });
  });

  describe('Create API with member (member role is specified by name)', () => {
    test('should create an API, and associate a member with role, by role name', async () => {
      const fakeApi = ApisFaker.apiImport({
        id: apiId,
        members: [{ source: 'gravitee', sourceId: member.email, role: customRole.name }],
        primaryOwner: { id: primaryOwner.id, type: 'USER', email: primaryOwner.email },
      });
      await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: fakeApi,
        }),
      );
    });

    test('should get API members, with a primary owner, and an additional member with associated role', async () => {
      const members = await succeed(apisResourceAsAdminUser.getApiMembersRaw({ orgId, envId, api: expectedApiId }));
      expect(members).toBeTruthy();
      expect(members).toHaveLength(2);
      expect(members).toEqual(
        expect.arrayContaining([
          { id: primaryOwner.id, displayName: primaryOwner.displayName, role: 'PRIMARY_OWNER' },
          { id: member.id, displayName: member.displayName, role: 'MY_TEST_ROLE' },
        ]),
      );
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        orgId,
        envId,
        api: expectedApiId,
      });
    });
  });

  afterAll(async () => {
    await configurationsResourceAsAdminUser.deleteRoleRaw({ role: roleName, orgId, scope: RoleScope.API });

    await usersResourceAsAdminUser.deleteUser({ orgId, envId, userId: member.id });

    await usersResourceAsAdminUser.deleteUser({ orgId, envId, userId: primaryOwner.id });
  });
});
