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
import { created, noContent } from '@lib/jest-utils';
import { forManagementAsAdminUser } from '@gravitee/utils/configuration';
import { GroupsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupsApi';
import { GroupsFaker } from '@gravitee/fixtures/management/GroupsFaker';
import { GroupEntity } from '@gravitee/management-webclient-sdk/src/lib/models';
import { orgId, envId } from '@gravitee/utils/api-products';
import { ApiProduct, createApiProduct, deleteApiProduct, getApiProduct, updateApiProduct } from './api-product-group-helpers';

const v1GroupsResourceAsAdmin = new GroupsApi(forManagementAsAdminUser());

describeIfClientGatewaySupportingApiProduct('API Product + Group — Section A: Group Assignment via Update', () => {
  let apiProduct: ApiProduct;
  let group1: GroupEntity;
  let group2: GroupEntity;

  beforeAll(async () => {
    group1 = await created(
      v1GroupsResourceAsAdmin.createGroupRaw({
        orgId,
        envId,
        newGroupEntity: GroupsFaker.newGroup(),
      }),
    );

    group2 = await created(
      v1GroupsResourceAsAdmin.createGroupRaw({
        orgId,
        envId,
        newGroupEntity: GroupsFaker.newGroup(),
      }),
    );

    apiProduct = await createApiProduct(`e2e-group-assignment-${Date.now()}`);
  });

  test('A1: should assign one group to API Product', async () => {
    const updated = await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [group1.id],
    });
    expect(updated.groups).toContain(group1.id);
    expect(updated.groups).toHaveLength(1);

    const fetched = await getApiProduct(apiProduct.id);
    expect(fetched.groups).toContain(group1.id);
  });

  test('A2: should assign multiple groups to API Product', async () => {
    const updated = await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [group1.id, group2.id],
    });
    expect(updated.groups).toHaveLength(2);
    expect(updated.groups).toContain(group1.id);
    expect(updated.groups).toContain(group2.id);
  });

  test('A3: should remove all groups from API Product', async () => {
    const updated = await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [],
    });
    expect(updated.groups ?? []).toHaveLength(0);

    const fetched = await getApiProduct(apiProduct.id);
    expect(fetched.groups ?? []).toHaveLength(0);
  });

  afterAll(async () => {
    if (apiProduct?.id) await deleteApiProduct(apiProduct.id);
    if (group1?.id) await noContent(v1GroupsResourceAsAdmin.deleteGroupRaw({ orgId, envId, group: group1.id })).catch(() => {});
    if (group2?.id) await noContent(v1GroupsResourceAsAdmin.deleteGroupRaw({ orgId, envId, group: group2.id })).catch(() => {});
  });
});
