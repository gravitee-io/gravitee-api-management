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
import { forManagementAsAdminUser, forManagementV2AsAdminUser, forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { created, noContent, succeed } from '@lib/jest-utils';
import { RoleEntity, RoleScope, UserEntity } from '@gravitee/management-webclient-sdk/src/lib/models';
import { APIsApi as v1APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { UsersApi } from '@gravitee/management-webclient-sdk/src/lib/apis/UsersApi';
import { UsersFaker } from '@gravitee/fixtures/management/UsersFaker';
import { ConfigurationApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ConfigurationApi';
import { RoleFaker } from '@gravitee/fixtures/management/RoleFaker';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2MembersFaker } from '@gravitee/fixtures/management/MAPIV2MembersFaker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const v1ApisResourceAsAdmin = new v1APIsApi(forManagementAsAdminUser());
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2ApisResourceAsAdmin = new APIsApi(forManagementV2AsAdminUser());
const v1UsersResourceAsAdmin = new UsersApi(forManagementAsAdminUser());
const v1ConfigurationResourceAsAdmin = new ConfigurationApi(forManagementAsAdminUser());

describe('API - V4 - Native Kafka - Import - Gravitee Definition - With members', () => {
  describe('Create v4 API from import with members', () => {
    const roleName = 'IMPORT_TEST_ROLE';
    let importedApi: ApiV4;
    let member: UserEntity;
    let memberWithUnknownRole: UserEntity;
    let memberWithNoRole: UserEntity;
    let customRole: RoleEntity;

    test('should create member, primary owner and custom role', async () => {
      member = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

      memberWithUnknownRole = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

      memberWithNoRole = await succeed(
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

    test('should import v4 API with member', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            api: MAPIV2ApisFaker.apiV4NativeKafka(),
            members: [
              MAPIV2MembersFaker.member({
                id: member.id,
                displayName: member.displayName,
                roles: [
                  {
                    name: customRole.name,
                    scope: customRole.scope,
                  },
                ],
              }),
              MAPIV2MembersFaker.member({
                id: memberWithUnknownRole.id,
                displayName: member.displayName,
                roles: [
                  {
                    name: 'NOT_FOUND',
                    scope: customRole.scope,
                  },
                ],
              }),
              MAPIV2MembersFaker.member({
                id: memberWithNoRole.id,
                displayName: member.displayName,
                roles: [],
              }),
            ],
          }),
        }),
      );
      expect(importedApi).toBeTruthy();
    });

    test('should get API members, with a primary owner, and an additional member with custom role', async () => {
      // But should be able to access it as an admin
      const members = await succeed(
        v1ApisResourceAsAdmin.getApiMembersRaw({
          orgId,
          envId,
          api: importedApi.id,
        }),
      );
      expect(members).toBeTruthy();
      expect(members).toHaveLength(4);
      const memberResult = members.find((m) => m.displayName === member.displayName);
      expect(memberResult.id).toBeTruthy();
      expect(memberResult.role).toEqual(customRole.name);
      const memberWithUnknownRoleResult = members.find((m) => m.displayName === memberWithUnknownRole.displayName);
      expect(memberWithUnknownRoleResult.id).toBeTruthy();
      expect(memberWithUnknownRoleResult.role).toEqual('USER');
      const memberWithNoRoleResult = members.find((m) => m.displayName === memberWithNoRole.displayName);
      expect(memberWithNoRoleResult.id).toBeTruthy();
      expect(memberWithNoRoleResult.role).toEqual('USER');
      const poMemberResult = members.find((m) => m.displayName === process.env.API_USERNAME);
      expect(poMemberResult.id).toBeTruthy();
      expect(poMemberResult.role).toEqual('PRIMARY_OWNER');
    });

    afterAll(async () => {
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
          userId: member.id,
        }),
      );
      await noContent(
        v2ApisResourceAsAdmin.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });
  });
});
