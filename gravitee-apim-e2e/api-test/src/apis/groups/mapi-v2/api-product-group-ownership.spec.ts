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
import { describeIfClientGatewaySupportingApiProduct } from '@lib/jest-utils';
import { created, noContent, succeed } from '@lib/jest-utils';
import { forManagementAsAdminUser } from '@gravitee/utils/configuration';
import { GroupsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupsApi';
import { GroupMembershipsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupMembershipsApi';
import { UsersApi } from '@gravitee/management-webclient-sdk/src/lib/apis/UsersApi';
import { GroupsFaker } from '@gravitee/fixtures/management/GroupsFaker';
import { UsersFaker } from '@gravitee/fixtures/management/UsersFaker';
import { GroupEntity } from '@gravitee/management-webclient-sdk/src/lib/models';
import { adminAuthHeader, envId, managementV2BaseUrl, orgId } from '@gravitee/utils/api-products';
import fetch from 'node-fetch';
import { ApiProduct, createApiProduct, deleteApiProduct, getApiProduct, listMembers } from './api-product-group-helpers';

const v1GroupsResourceAsAdmin = new GroupsApi(forManagementAsAdminUser());
const v1GroupMembershipResourceAsAdmin = new GroupMembershipsApi(forManagementAsAdminUser());
const v1UsersResourceAsAdmin = new UsersApi(forManagementAsAdminUser());

async function transferOwnership(
  apiProductId: string,
  newPrimaryOwnerId: string,
  userType: 'USER' | 'GROUP',
  opts?: { userReference?: string; currentPrimaryOwnerNewRole?: string },
): Promise<number> {
  const response = await fetch(
    `${managementV2BaseUrl}/environments/${envId}/api-products/${apiProductId}/members/_transfer-ownership`,
    {
      method: 'POST',
      headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        newPrimaryOwnerId,
        userType,
        userReference: opts?.userReference,
        currentPrimaryOwnerNewRole: opts?.currentPrimaryOwnerNewRole ?? 'USER',
      }),
    },
  );
  return response.status;
}

describeIfClientGatewaySupportingApiProduct('API Product + Group — Section C: Ownership Transfer', () => {
  describe('C1: Transfer ownership from user to user', () => {
    let apiProduct: ApiProduct;
    let user: { id: string; referenceId?: string };

    test('should create user and API Product', async () => {
      user = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );
      apiProduct = await createApiProduct(`e2e-ownership-c1-${Date.now()}`);
    });

    test('should transfer ownership to user', async () => {
      const status = await transferOwnership(apiProduct.id, user.id, 'USER', {
        userReference: user.referenceId,
      });
      expect(status).toEqual(204);
    });

    test('should verify new owner and admin downgrade in members list', async () => {
      const members = await listMembers(apiProduct.id);

      // New owner has PRIMARY_OWNER
      const newOwner = members.data.find((m) => m.id === user.id);
      expect(newOwner).toBeDefined();
      expect(newOwner.roles.some((r) => r.name === 'PRIMARY_OWNER')).toBe(true);

      // Admin was downgraded — must not have PRIMARY_OWNER anymore
      const admins = members.data.filter((m) => m.id !== user.id);
      for (const admin of admins) {
        expect(admin.roles.some((r) => r.name === 'PRIMARY_OWNER')).toBe(false);
      }
    });

    afterAll(async () => {
      if (apiProduct?.id) await deleteApiProduct(apiProduct.id);
      if (user?.id) await noContent(v1UsersResourceAsAdmin.deleteUserRaw({ orgId, envId, userId: user.id })).catch(() => {});
    });
  });

  describe('C2: Transfer ownership from user to group', () => {
    let apiProduct: ApiProduct;
    let user: { id: string; reference?: string };
    let group: GroupEntity;

    test('should create user, group, and API Product', async () => {
      user = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

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
              roles: [{ name: 'PRIMARY_OWNER', scope: 'API_PRODUCT' }],
            },
          ],
        }),
      );

      apiProduct = await createApiProduct(`e2e-ownership-c2-${Date.now()}`);
    });

    test('should transfer ownership to group', async () => {
      const status = await transferOwnership(apiProduct.id, group.id, 'GROUP');
      expect(status).toEqual(204);
    });

    test('should verify group is automatically added to API Product groups set', async () => {
      const product = await getApiProduct(apiProduct.id);
      expect(product.groups).toContain(group.id);
    });

    afterAll(async () => {
      if (apiProduct?.id) await deleteApiProduct(apiProduct.id);
      if (group?.id) await noContent(v1GroupsResourceAsAdmin.deleteGroupRaw({ orgId, envId, group: group.id })).catch(() => {});
      if (user?.id) await noContent(v1UsersResourceAsAdmin.deleteUserRaw({ orgId, envId, userId: user.id })).catch(() => {});
    });
  });

  describe('C3: Transfer ownership from group to user', () => {
    let apiProduct: ApiProduct;
    let user: { id: string; reference?: string };
    let targetUser: { id: string; referenceId?: string };
    let group: GroupEntity;

    test('should create users, group, and API Product with group as owner', async () => {
      user = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

      targetUser = await succeed(
        v1UsersResourceAsAdmin.createUserRaw({
          orgId,
          envId,
          newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
        }),
      );

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
              roles: [{ name: 'PRIMARY_OWNER', scope: 'API_PRODUCT' }],
            },
          ],
        }),
      );

      apiProduct = await createApiProduct(`e2e-ownership-c3-${Date.now()}`);

      // Transfer ownership to group first
      const status = await transferOwnership(apiProduct.id, group.id, 'GROUP');
      expect(status).toEqual(204);

      // Verify group is in groups set
      const product = await getApiProduct(apiProduct.id);
      expect(product.groups).toContain(group.id);
    });

    test('should transfer ownership from group to user', async () => {
      const status = await transferOwnership(apiProduct.id, targetUser.id, 'USER', {
        userReference: targetUser.referenceId,
      });
      expect(status).toEqual(204);
    });

    test('should verify group is automatically removed from API Product groups set', async () => {
      const product = await getApiProduct(apiProduct.id);
      expect(product.groups ?? []).not.toContain(group.id);
    });

    afterAll(async () => {
      if (apiProduct?.id) await deleteApiProduct(apiProduct.id);
      if (group?.id) await noContent(v1GroupsResourceAsAdmin.deleteGroupRaw({ orgId, envId, group: group.id })).catch(() => {});
      if (user?.id) await noContent(v1UsersResourceAsAdmin.deleteUserRaw({ orgId, envId, userId: user.id })).catch(() => {});
      if (targetUser?.id) await noContent(v1UsersResourceAsAdmin.deleteUserRaw({ orgId, envId, userId: targetUser.id })).catch(() => {});
    });
  });
});
