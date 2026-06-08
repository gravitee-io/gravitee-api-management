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
import { ApiProduct, createApiProduct, deleteApiProduct, updateApiProduct, addMember, listMembers } from './api-product-group-helpers';

const v1GroupsResourceAsAdmin = new GroupsApi(forManagementAsAdminUser());
const v1GroupMembershipResourceAsAdmin = new GroupMembershipsApi(forManagementAsAdminUser());
const v1UsersResourceAsAdmin = new UsersApi(forManagementAsAdminUser());

describeIfClientGatewaySupportingApiProduct('API Product + Group — Section B: Member Management', () => {
  let apiProduct: ApiProduct;
  let user: { id: string; reference?: string };
  let groupUser: { id: string; reference?: string };
  let group1: GroupEntity;

  beforeAll(async () => {
    user = await succeed(
      v1UsersResourceAsAdmin.createUserRaw({
        orgId,
        envId,
        newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
      }),
    );

    groupUser = await succeed(
      v1UsersResourceAsAdmin.createUserRaw({
        orgId,
        envId,
        newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
      }),
    );

    group1 = await created(
      v1GroupsResourceAsAdmin.createGroupRaw({
        orgId,
        envId,
        newGroupEntity: GroupsFaker.newGroup(),
      }),
    );

    // Add groupUser to group1 with USER role
    await succeed(
      v1GroupMembershipResourceAsAdmin.addOrUpdateGroupMemberRaw({
        envId,
        orgId,
        group: group1.id,
        groupMembership: [
          {
            id: groupUser.id,
            reference: groupUser.reference,
            roles: [{ name: 'USER', scope: 'API_PRODUCT' }],
          },
        ],
      }),
    );

    apiProduct = await createApiProduct(`e2e-group-members-${Date.now()}`);
  });

  describe('User member CRUD', () => {
    let memberId: string;

    test('B1: should add user as member with USER role', async () => {
      const { status, body: member } = await addMember(apiProduct.id, user.id, 'USER');
      expect(status).toEqual(201);
      expect(member.id).toBeDefined();
      memberId = member.id;

      const members = await listMembers(apiProduct.id);
      const found = members.data.find((m) => m.id === user.id);
      expect(found).toBeDefined();
      expect(found.roles.some((r) => r.name === 'USER')).toBe(true);
    });

    test('B2: should update member role from USER to OWNER', async () => {
      const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/members/${memberId}`, {
        method: 'PUT',
        headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
        body: JSON.stringify({ roleName: 'OWNER' }),
      });
      expect(updateResponse.status).toEqual(200);

      // Verify role actually changed
      const members = await listMembers(apiProduct.id);
      const found = members.data.find((m) => m.id === user.id);
      expect(found).toBeDefined();
      expect(found.roles.some((r) => r.name === 'OWNER')).toBe(true);
      expect(found.roles.some((r) => r.name === 'USER')).toBe(false);
    });

    test('B3: should remove member', async () => {
      const deleteResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/members/${memberId}`, {
        method: 'DELETE',
        headers: { Authorization: adminAuthHeader },
      });
      expect(deleteResponse.status).toEqual(204);

      const members = await listMembers(apiProduct.id);
      const found = members.data.find((m) => m.id === user.id);
      expect(found).toBeUndefined();
    });
  });

  test('B4: should reject adding member with PRIMARY_OWNER role', async () => {
    const { status } = await addMember(apiProduct.id, user.id, 'PRIMARY_OWNER');
    expect(status).toEqual(400);
  });

  test('B5: group member should not appear in direct members list', async () => {
    // Assign group1 (which contains groupUser) to the API Product
    await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [group1.id],
    });

    const members = await listMembers(apiProduct.id);

    // groupUser is in the group but NOT a direct member — must not appear in /members
    const found = members.data.find((m) => m.id === groupUser.id);
    expect(found).toBeUndefined();
  });

  afterAll(async () => {
    if (apiProduct?.id) await deleteApiProduct(apiProduct.id);
    if (group1?.id) await noContent(v1GroupsResourceAsAdmin.deleteGroupRaw({ orgId, envId, group: group1.id })).catch(() => {});
    if (user?.id) await noContent(v1UsersResourceAsAdmin.deleteUserRaw({ orgId, envId, userId: user.id })).catch(() => {});
    if (groupUser?.id) await noContent(v1UsersResourceAsAdmin.deleteUserRaw({ orgId, envId, userId: groupUser.id })).catch(() => {});
  });
});
