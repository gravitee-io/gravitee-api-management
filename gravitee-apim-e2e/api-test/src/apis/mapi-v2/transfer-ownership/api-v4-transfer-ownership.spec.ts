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
import { test, describe, expect, afterAll } from '@jest/globals';
import {
  APIMembersApi,
  ApiResponse,
  APIsApi,
  Api,
  ApiV4,
  HttpListener,
  MembersResponse,
} from '@gravitee/management-v2-webclient-sdk/src/lib';
import {
  API_USER,
  forManagementAsAdminUser,
  forManagementV2,
  forManagementV2AsAdminUser,
  forManagementV2AsApiUser,
} from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, fail, forbidden, noContent, succeed } from '@lib/jest-utils';
import { UsersFaker } from '@gravitee/fixtures/management/UsersFaker';
import { UsersApi } from '@gravitee/management-webclient-sdk/src/lib/apis/UsersApi';
import { RoleScope } from '@gravitee/management-webclient-sdk/src/lib/models';
import { RoleFaker } from '@gravitee/fixtures/management/RoleFaker';
import { ConfigurationApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ConfigurationApi';
import { UserRegistrationApi } from '@gravitee/management-webclient-sdk/src/lib/apis/UserRegistrationApi';
import { GroupsFaker } from '@gravitee/fixtures/management/GroupsFaker';
import { GroupsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupsApi';
import { GroupMembershipsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupMembershipsApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const v1ConfigurationResourceAsAdmin = new ConfigurationApi(forManagementAsAdminUser());
const v1UsersResourceAsAdmin = new UsersApi(forManagementAsAdminUser());
const v1GroupsResourceAsAdmin = new GroupsApi(forManagementAsAdminUser());
const v1GroupMembershipResourceAsAdmin = new GroupMembershipsApi(forManagementAsAdminUser());
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2ApisResourceAsAdmin = new APIsApi(forManagementV2AsAdminUser());
const v2ApiMembersResourceAsApiPublisher = new APIMembersApi(forManagementV2AsApiUser());
const v2ApiMembersResourceAsAdmin = new APIMembersApi(forManagementV2AsAdminUser());

describe('API - V4 - Transfer Ownership', () => {
  describe('Transfer ownership to other user', () => {
    const roleName = 'TRANSFER_OWNERSHIP_ROLE';
    let importedApi;
    let user;
    let customRole;

    test('should create member and custom role', async () => {
      user = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

      customRole = await succeed(
        v1ConfigurationResourceAsAdmin.createRoleRaw({
          orgId,
          scope: RoleScope.API,
          newRoleEntity: RoleFaker.newRoleEntity({ name: roleName, scope: RoleScope.API }),
        }),
      );
    });

    test('should import v4 API', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4(),
        }),
      );
    });

    test('should get created v4 API with generated ID', async () => {
      const apiV4: Api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(apiV4).toBeTruthy();
      expect(apiV4.id).toStrictEqual(importedApi.id);
    });

    test('should transfer ownership to other user', async () => {
      await noContent(
        v2ApisResourceAsApiPublisher.transferOwnershipRaw({
          envId,
          apiId: importedApi.id,
          apiTransferOwnership: {
            userId: user.id,
            userType: 'USER',
            userReference: user.referenceId,
            poRole: customRole.name,
          },
        }),
      );
    });

    test('should not be able to see member as original api publisher', async () => {
      await forbidden(
        v2ApiMembersResourceAsApiPublisher.listApiMembersRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });

    test('should verify other user is primary owner', async () => {
      let membersResponse: MembersResponse = await succeed(
        // Ideally, we should list members as the 'Other user'
        v2ApiMembersResourceAsAdmin.listApiMembersRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(membersResponse).toBeTruthy();
      const apiPublisher = membersResponse.data.find((member) => member.displayName === API_USER.username);
      const otherUser = membersResponse.data.find((member) => member.id === user.id);

      expect(apiPublisher.roles).toHaveLength(1);
      expect(apiPublisher.roles[0].name).toStrictEqual(customRole.name);

      expect(otherUser.roles).toHaveLength(1);
      expect(otherUser.roles[0].name).toStrictEqual('PRIMARY_OWNER');
    });

    afterAll(async () => {
      await noContent(
        v2ApisResourceAsAdmin.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      await noContent(
        v1ConfigurationResourceAsAdmin.deleteRoleRaw({
          orgId,
          role: roleName,
          scope: RoleScope.API,
        }),
      );
      await noContent(
        v1UsersResourceAsAdmin.deleteUserRaw({
          orgId,
          envId,
          userId: user.id,
        }),
      );
    });
  });
  describe('Transfer ownership to an api member', () => {
    const roleName = 'TRANSFER_OWNERSHIP_ROLE';
    let importedApi;
    let user;
    let customRole;

    test('should create member and custom role', async () => {
      user = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

      customRole = await succeed(
        v1ConfigurationResourceAsAdmin.createRoleRaw({
          orgId,
          scope: RoleScope.API,
          newRoleEntity: RoleFaker.newRoleEntity({ name: roleName, scope: RoleScope.API }),
        }),
      );
    });

    test('should import v4 API', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4(),
        }),
      );
    });

    test('should add user as api member', async () => {
      v2ApiMembersResourceAsApiPublisher.addApiMember({
        envId,
        apiId: importedApi.id,
        addMember: {
          userId: user.id,
          externalReference: user.referenceId,
          roleName: 'USER',
        },
      });
    });

    test('should get created v4 API with generated ID', async () => {
      const apiV4: Api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(apiV4).toBeTruthy();
      expect(apiV4.id).toStrictEqual(importedApi.id);
    });

    test('should transfer ownership to other user', async () => {
      await noContent(
        v2ApisResourceAsApiPublisher.transferOwnershipRaw({
          envId,
          apiId: importedApi.id,
          apiTransferOwnership: {
            userId: user.id,
            userType: 'USER',
            userReference: user.referenceId,
            poRole: customRole.name,
          },
        }),
      );
    });

    test('should not be able to see member as original api publisher', async () => {
      await forbidden(
        v2ApiMembersResourceAsApiPublisher.listApiMembersRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });

    test('should verify other user is primary owner', async () => {
      let membersResponse: MembersResponse = await succeed(
        // Ideally, we should list members as the 'Other user'
        v2ApiMembersResourceAsAdmin.listApiMembersRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(membersResponse).toBeTruthy();
      const apiPublisher = membersResponse.data.find((member) => member.displayName === API_USER.username);
      const otherUser = membersResponse.data.find((member) => member.id === user.id);

      expect(apiPublisher.roles).toHaveLength(1);
      expect(apiPublisher.roles[0].name).toStrictEqual(customRole.name);

      expect(otherUser.roles).toHaveLength(1);
      expect(otherUser.roles[0].name).toStrictEqual('PRIMARY_OWNER');
    });

    afterAll(async () => {
      await noContent(
        v2ApisResourceAsAdmin.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      await noContent(
        v1ConfigurationResourceAsAdmin.deleteRoleRaw({
          orgId,
          role: roleName,
          scope: RoleScope.API,
        }),
      );
      await noContent(
        v1UsersResourceAsAdmin.deleteUserRaw({
          orgId,
          envId,
          userId: user.id,
        }),
      );
    });
  });
  describe('Transfer ownership to a group', () => {
    const roleName = 'TRANSFER_OWNERSHIP_ROLE';
    let importedApi;
    let user;
    let customRole;
    let group;

    test('should create member and custom role', async () => {
      user = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

      customRole = await succeed(
        v1ConfigurationResourceAsAdmin.createRoleRaw({
          orgId,
          scope: RoleScope.API,
          newRoleEntity: RoleFaker.newRoleEntity({ name: roleName, scope: RoleScope.API }),
        }),
      );
    });

    test('should create group', async () => {
      group = await created(
        v1GroupsResourceAsAdmin.createGroupRaw({
          orgId,
          envId,
          newGroupEntity: GroupsFaker.newGroup(),
        }),
      );

      await succeed(
        v1GroupMembershipResourceAsAdmin.addOrUpdateGroupMemberRaw({
          envId,
          orgId,
          group: group.id,
          groupMembership: [
            {
              id: user.id,
              reference: user.reference,
              roles: [
                {
                  name: 'PRIMARY_OWNER',
                  scope: 'API',
                },
              ],
            },
          ],
        }),
      );
    });

    test('should import v4 API', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({}),
        }),
      );
    });

    test('should get created v4 API with generated ID', async () => {
      const apiV4: Api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(apiV4).toBeTruthy();
      expect(apiV4.id).toStrictEqual(importedApi.id);
    });

    test('should transfer ownership to group', async () => {
      await noContent(
        v2ApisResourceAsApiPublisher.transferOwnershipRaw({
          envId,
          apiId: importedApi.id,
          apiTransferOwnership: {
            userId: group.id,
            userType: 'GROUP',
            userReference: null,
            poRole: customRole.name,
          },
        }),
      );
    });

    test('should not be able to see member as original api publisher', async () => {
      await forbidden(
        v2ApiMembersResourceAsApiPublisher.listApiMembersRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });

    test('should verify api publisher is not primary owner anymore', async () => {
      let membersResponse = await succeed(
        // Ideally, we should list members as the 'Other user'
        v2ApiMembersResourceAsAdmin.listApiMembersRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(membersResponse).toBeTruthy();
      expect(membersResponse.data).toHaveLength(1);
      const apiPublisher = membersResponse.data.find((member) => member.displayName === API_USER.username);

      expect(apiPublisher.roles).toHaveLength(1);
      expect(apiPublisher.roles[0].name).toStrictEqual(customRole.name);
    });

    test('should verify group is primary owner', async () => {
      let groupResponse = await succeed(
        v1GroupsResourceAsAdmin.getGroupRaw({
          orgId,
          envId,
          group: group.id,
        }),
      );
      expect(groupResponse).toBeTruthy();
      expect(groupResponse.apiPrimaryOwner).toEqual(user.id);
      expect(groupResponse.primary_owner).toBeTruthy();
    });

    afterAll(async () => {
      await noContent(
        v2ApisResourceAsAdmin.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      await noContent(
        v1ConfigurationResourceAsAdmin.deleteRoleRaw({
          orgId,
          role: roleName,
          scope: RoleScope.API,
        }),
      );
      await noContent(
        v1GroupsResourceAsAdmin.deleteGroupRaw({
          orgId,
          envId,
          group: group.id,
        }),
      );
      await noContent(
        v1UsersResourceAsAdmin.deleteUserRaw({
          orgId,
          envId,
          userId: user.id,
        }),
      );
    });
  });
});
