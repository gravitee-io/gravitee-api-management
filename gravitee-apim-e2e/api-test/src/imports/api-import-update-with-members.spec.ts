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
import { ApisFaker } from '@management-fakers/ApisFaker';
import { UsersApi } from '@management-apis/UsersApi';
import { UsersFaker } from '@management-fakers/UsersFaker';
import { RolesApi } from '@management-apis/RolesApi';
import { RoleFaker } from '@management-fakers/RoleFaker';
import { RoleScope } from '@management-models/RoleScope';

const apisResource = new APIsApi(forManagementAsAdminUser());
const usersResource = new UsersApi(forManagementAsAdminUser());
const rolesResource = new RolesApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Update API form import with members', () => {
  describe('Update API that already has members, without specifying any members in data', () => {
    const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';

    let member;
    let primaryOwner;
    let roleName = 'MY_TEST_ROLE';
    let roleId;

    let fakeApi;

    test('should create user (future member)', async () => {
      member = await succeed(
        usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
      );
    });

    test('should create user (future primary owner)', async () => {
      primaryOwner = await succeed(
        usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
      );
    });

    test('should create role', async () => {
      let role = await succeed(
        rolesResource.createRoleRaw({ scope: RoleScope.API, orgId, newRoleEntity: RoleFaker.newRoleEntity({ name: roleName }) }),
      );
      roleId = role.id;
    });

    test('should create an API, and associate a member with role', async () => {
      fakeApi = ApisFaker.apiImport({
        id: '533efd8a-22e1-4483-a8af-0c24a2abd590',
        members: [{ source: 'gravitee', sourceId: member.email, roles: [roleId] }],
        primaryOwner: { id: primaryOwner.id, type: 'USER', email: primaryOwner.email },
      });

      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should update the API, without any primaryOwner or members in data', async () => {
      fakeApi.members = [];
      fakeApi.primaryOwner = {};
      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: fakeApi }));
    });

    test('should get API members, which has kept both members that were present before update', async () => {
      let members = await succeed(apisResource.getApiMembersRaw({ envId, orgId, api: expectedApiId }));
      expect(members).toHaveLength(2);
      expect(members).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ id: primaryOwner.id, displayName: primaryOwner.displayName, role: 'PRIMARY_OWNER' }),
          expect.objectContaining({ id: member.id, displayName: member.displayName, role: 'MY_TEST_ROLE' }),
        ]),
      );
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      await rolesResource.deleteRoleRaw({ scope: RoleScope.API, orgId, role: roleName });
      if (member) {
        await usersResource.deleteUser({ envId, orgId, userId: member.id });
      }
      if (primaryOwner) {
        await usersResource.deleteUser({ envId, orgId, userId: primaryOwner.id });
      }
    });
  });

  describe('Update API that has 2 members, updating the role of 1 member', () => {
    const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';

    let member1;
    let member2;
    let primaryOwner;
    let role1Name = 'MY_TEST_ROLE';
    let role1Id;
    let role2Name = 'MY_OTHER_ROLE';
    let role2Id;
    let fakeApi;

    test('should create user (member 1)', async () => {
      member1 = await succeed(
        usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
      );
    });

    test('should create user (member 2)', async () => {
      member2 = await succeed(
        usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
      );
    });

    test('should create user (primary owner)', async () => {
      primaryOwner = await succeed(
        usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
      );
    });

    test('should create role1', async () => {
      let role = await succeed(
        rolesResource.createRoleRaw({ scope: RoleScope.API, orgId, newRoleEntity: RoleFaker.newRoleEntity({ name: role1Name }) }),
      );
      role1Id = role.id;
    });

    test('should create role2', async () => {
      let role = await succeed(
        rolesResource.createRoleRaw({ scope: RoleScope.API, orgId, newRoleEntity: RoleFaker.newRoleEntity({ name: role2Name }) }),
      );
      role2Id = role.id;
    });

    test('should create an API, with associated members', async () => {
      // member1 has role1, member2 has role2
      fakeApi = ApisFaker.apiImport({
        id: '533efd8a-22e1-4483-a8af-0c24a2abd590',
        members: [
          { source: 'gravitee', sourceId: member1.email, roles: [role1Id] },
          { source: 'gravitee', sourceId: member2.email, roles: [role2Id] },
        ],
        primaryOwner: { id: primaryOwner.id, type: 'USER', email: primaryOwner.email },
      });

      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should update the API, without any primaryOwner or members in data', async () => {
      // member1 has role2 (changed), member2 has role2 (not changed)
      fakeApi.members = [
        { source: 'gravitee', sourceId: member1.email, roles: [role2Id] },
        { source: 'gravitee', sourceId: member2.email, roles: [role2Id] },
      ];
      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: fakeApi }));
    });

    test('should export the API, resulting with member with updated roles', async () => {
      let exportedApi = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
      expect(exportedApi.members).toHaveLength(3);
      const member1Roles = exportedApi.members.filter((m) => m.sourceId == member1.email)[0].roles;
      expect(member1Roles).toHaveLength(2);
      expect(member1Roles).toContain(role1Id);
      expect(member1Roles).toContain(role2Id);
      const member2Roles = exportedApi.members.filter((m) => m.sourceId == member2.email)[0].roles;
      expect(member2Roles).toHaveLength(1);
      expect(member2Roles).toContain(role2Id);
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      await rolesResource.deleteRoleRaw({ scope: RoleScope.API, orgId, role: role2Name });
      await rolesResource.deleteRoleRaw({ scope: RoleScope.API, orgId, role: role1Name });
      if (member1) {
        await usersResource.deleteUser({ envId, orgId, userId: member1.id });
      }
      if (member2) {
        await usersResource.deleteUser({ envId, orgId, userId: member2.id });
      }
      if (primaryOwner) {
        await usersResource.deleteUser({ envId, orgId, userId: primaryOwner.id });
      }
    });
  });
});
