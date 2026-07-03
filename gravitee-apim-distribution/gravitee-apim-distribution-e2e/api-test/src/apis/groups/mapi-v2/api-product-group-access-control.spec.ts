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
import { afterAll, beforeAll, expect, test } from '@jest/globals';
import { describeIfClientGatewaySupportingApiProduct } from '@lib/jest-utils';
import { created, noContent, succeed } from '@lib/jest-utils';
import { API_USER, forManagementAsAdminUser } from '@gravitee/utils/configuration';
import { GroupsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupsApi';
import { GroupMembershipsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupMembershipsApi';
import { GroupsFaker } from '@gravitee/fixtures/management/GroupsFaker';
import { GroupEntity } from '@gravitee/management-webclient-sdk/src/lib/models';
import { adminAuthHeader, apiAuthHeader, simpleAuthHeader, envId, managementV2BaseUrl, orgId } from '@gravitee/utils/api-products';
import fetch from 'node-fetch';
import { ApiProduct, createApiProduct, deleteApiProduct, updateApiProduct, addMember, lookupUserId } from './api-product-group-helpers';

const v1GroupsResourceAsAdmin = new GroupsApi(forManagementAsAdminUser());
const v1GroupMembershipResourceAsAdmin = new GroupMembershipsApi(forManagementAsAdminUser());

describeIfClientGatewaySupportingApiProduct('API Product + Group — Section F: Access Control via Group Membership', () => {
  let apiProduct: ApiProduct;
  let group: GroupEntity;
  let apiUserId: string;

  beforeAll(async () => {
    group = await created(
      v1GroupsResourceAsAdmin.createGroupRaw({
        orgId,
        envId,
        newGroupEntity: GroupsFaker.newGroup(),
      }),
    );

    apiProduct = await createApiProduct(`e2e-access-control-${Date.now()}`);

    // Assign group to the API Product
    await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [group.id],
    });

    // Look up api1 user's actual UUID
    apiUserId = await lookupUserId(API_USER.username);

    // Add api1 user to the group with USER role on API_PRODUCT scope
    await succeed(
      v1GroupMembershipResourceAsAdmin.addOrUpdateGroupMemberRaw({
        envId,
        orgId,
        group: group.id,
        groupMembership: [
          {
            id: apiUserId,
            roles: [{ name: 'USER', scope: 'API_PRODUCT' }],
          },
        ],
      }),
    );
  });

  // F1-F4 require configured test users (apiAuthHeader / simpleAuthHeader).
  // Use conditional test so unconfigured envs report "skipped", not a silent green pass.
  const itIfApiAuth = apiAuthHeader ? test : test.skip;
  const itIfSimpleAuth = simpleAuthHeader ? test : test.skip;

  itIfApiAuth('F1: group member should be able to read API Product', async () => {
    const getResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'GET',
      headers: { Authorization: apiAuthHeader },
    });
    expect(getResponse.status).toEqual(200);

    const product = (await getResponse.json()) as ApiProduct;
    expect(product.id).toEqual(apiProduct.id);
  });

  itIfSimpleAuth('F2: non-member user should be denied access to API Product', async () => {
    const getResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'GET',
      headers: { Authorization: simpleAuthHeader },
    });
    expect([401, 403]).toContain(getResponse.status);
  });

  itIfApiAuth('F3: group member with USER role should not be able to update API Product', async () => {
    const updateResponse = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}`, {
      method: 'PUT',
      headers: { Authorization: apiAuthHeader, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: apiProduct.name,
        description: 'Attempted update by group member',
        version: apiProduct.version,
      }),
    });
    expect([401, 403]).toContain(updateResponse.status);
  });

  itIfApiAuth('F4: dual membership — direct OWNER role elevates group USER permissions', async () => {
    // Add api1 as direct member with OWNER role (higher than group's USER role)
    const { status } = await addMember(apiProduct.id, apiUserId, 'OWNER');
    expect([200, 201]).toContain(status);

    // Verify effective permissions reflect OWNER capabilities (not just USER read-only)
    const permissionsResponse = await fetch(
      `${managementV2BaseUrl}/environments/${envId}/api-products/${apiProduct.id}/members/permissions`,
      {
        method: 'GET',
        headers: { Authorization: apiAuthHeader },
      },
    );
    expect(permissionsResponse.status).toEqual(200);
    const permissions = (await permissionsResponse.json()) as Record<string, unknown>;
    const permissionKeys = Object.keys(permissions);
    expect(permissionKeys.length).toBeGreaterThan(0);
    expect(permissionKeys).toEqual(expect.arrayContaining(['DEFINITION', 'MEMBER']));

    // OWNER should have write permissions — at least one key should contain 'U' (update) or 'C' (create)
    const allValues = Object.values(permissions).map(String);
    const hasWritePermission = allValues.some((v) => v.includes('U') || v.includes('C') || v.includes('D'));
    expect(hasWritePermission).toBe(true);
  });

  afterAll(async () => {
    if (apiProduct?.id) await deleteApiProduct(apiProduct.id);
    if (group?.id) await noContent(v1GroupsResourceAsAdmin.deleteGroupRaw({ orgId, envId, group: group.id })).catch(() => {});
  });
});
