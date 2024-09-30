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

import { API_USER, forManagementAsAdminUser, forManagementAsApiUser } from '@gravitee/utils/configuration';
import { RolesApi } from '../../../lib/management-webclient-sdk/src/lib/apis/RolesApi';
import { UserTokensApi } from '../../../lib/management-webclient-sdk/src/lib/apis/UserTokensApi';
import { UsersApi } from '../../../lib/management-webclient-sdk/src/lib/apis/UsersApi';
import type { NewRoleEntity, TokenEntity, UserEntity } from '../../../lib/management-webclient-sdk/src/lib/models';
import { beforeAll, describe, expect } from '@jest/globals';
import { created, noContent, succeed } from '@lib/jest-utils';
import faker from '@faker-js/faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const managementRolesResourceAsAdminUser = new RolesApi(forManagementAsAdminUser());
const managementUserResourceAsAdminUser = new UsersApi(forManagementAsAdminUser());
const managementUserTokensResourceAsAdminUser = new UserTokensApi(forManagementAsAdminUser());
const managementUserTokensResourceAsApiUser = new UserTokensApi(forManagementAsApiUser());

describe('User tokens crud tests', () => {
  let apiUser: UserEntity;

  beforeAll(async () => {
    const users = await managementUserResourceAsAdminUser.getAllUsers({ orgId, envId });
    apiUser = users.data.find((user) => user.displayName === API_USER.username);
  });

  describe('Create', () => {
    it('should create a token', async () => {
      const createdTokenRole = await succeed(
        managementRolesResourceAsAdminUser.createRoleRaw({
          newRoleEntity: getNewRoleEntity({ USER_TOKEN: ['C'] }),
          scope: 'ORGANIZATION',
          orgId,
        }),
      );

      const createdToken = await created(
        managementUserTokensResourceAsApiUser.createTokenRaw({
          orgId,
          envId,
          newTokenEntity: { name: `an awesome token ${faker.datatype.uuid()}` },
          userId: apiUser.id,
        }),
      );
      expect(createdToken).toBeTruthy();

      await noContent(managementRolesResourceAsAdminUser.deleteRoleRaw({ role: createdTokenRole.name, orgId, scope: 'ORGANIZATION' }));
      await noContent(managementUserTokensResourceAsAdminUser.revokeToken7Raw({ token: createdToken.id, orgId, userId: apiUser.id }));
    });
  });

  describe('READ', () => {
    it('should create and read token', async () => {
      const createdTokenRole = await succeed(
        managementRolesResourceAsAdminUser.createRoleRaw({
          newRoleEntity: getNewRoleEntity({ USER_TOKEN: ['C', 'R'] }),
          scope: 'ORGANIZATION',
          orgId,
        }),
      );

      const createdToken: TokenEntity = await created(
        managementUserTokensResourceAsApiUser.createTokenRaw({
          orgId,
          envId,
          newTokenEntity: { name: `an awesome token ${faker.datatype.uuid()}` },
          userId: apiUser.id,
        }),
      );

      expect(createdToken).toBeTruthy();

      const userTokens = await succeed(
        managementUserTokensResourceAsApiUser.getUserTokensRaw({
          orgId,
          envId,
          userId: apiUser.id,
        }),
      );

      expect(userTokens).toBeTruthy();
      expect(userTokens.find((t) => t.id === createdToken.id)).toBeTruthy();

      await noContent(managementRolesResourceAsAdminUser.deleteRoleRaw({ role: createdTokenRole.name, orgId, scope: 'ORGANIZATION' }));
      await noContent(managementUserTokensResourceAsAdminUser.revokeToken7Raw({ token: createdToken.id, orgId, userId: apiUser.id }));
    });
  });

  describe('DELETE', () => {
    it('should create and delete token', async () => {
      const createdTokenRole = await succeed(
        managementRolesResourceAsAdminUser.createRoleRaw({
          newRoleEntity: getNewRoleEntity({ USER_TOKEN: ['C', 'D'] }),
          scope: 'ORGANIZATION',
          orgId,
        }),
      );

      const createdToken: TokenEntity = await created(
        managementUserTokensResourceAsApiUser.createTokenRaw({
          orgId,
          envId,
          newTokenEntity: { name: `an awesome token ${faker.datatype.uuid()}` },
          userId: apiUser.id,
        }),
      );

      expect(createdToken).toBeTruthy();

      await noContent(managementUserTokensResourceAsApiUser.revokeToken7Raw({ token: createdToken.id, orgId, userId: apiUser.id }));
      await noContent(managementRolesResourceAsAdminUser.deleteRoleRaw({ role: createdTokenRole.name, orgId, scope: 'ORGANIZATION' }));
    });
  });

  function getNewRoleEntity(permissions: { [key: string]: Array<string> }): NewRoleEntity {
    return {
      name: `CREATED_TOKEN_ROLE_${faker.datatype.uuid()}`,
      description: faker.lorem.sentence(),
      scope: 'ORGANIZATION',
      _default: false,
      permissions,
    };
  }
});
