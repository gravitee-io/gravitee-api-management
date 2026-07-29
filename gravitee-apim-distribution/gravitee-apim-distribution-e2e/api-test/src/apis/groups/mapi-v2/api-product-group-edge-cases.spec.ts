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
import { orgId, envId } from '@gravitee/utils/api-products';
import { faker } from '@faker-js/faker';
import { ApiProduct, createApiProduct, deleteApiProduct, getApiProduct, updateApiProduct } from './api-product-group-helpers';

const v1GroupsResourceAsAdmin = new GroupsApi(forManagementAsAdminUser());

describeIfClientGatewaySupportingApiProduct('API Product + Group — Section E: Data Integrity & Edge Cases', () => {
  let apiProduct: ApiProduct;

  beforeAll(async () => {
    apiProduct = await createApiProduct(`e2e-edge-cases-${Date.now()}`);
  });

  test('E1: should accept non-existent group ID silently', async () => {
    const fakeGroupId = faker.string.uuid();

    const updated = await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [fakeGroupId],
    });
    expect(updated.groups).toContain(fakeGroupId);

    const fetched = await getApiProduct(apiProduct.id);
    expect(fetched.groups).toContain(fakeGroupId);

    // Clean up: remove fake group
    await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [],
    });
  });

  test('E2: should cascade-remove group ID from API Product after group deletion', async () => {
    const group = await created(
      v1GroupsResourceAsAdmin.createGroupRaw({
        orgId,
        envId,
        newGroupEntity: GroupsFaker.newGroup(),
      }),
    );

    await updateApiProduct(apiProduct.id, {
      name: apiProduct.name,
      description: apiProduct.description,
      version: apiProduct.version,
      groups: [group.id],
    });

    // Verify group is assigned
    let product = await getApiProduct(apiProduct.id);
    expect(product.groups).toContain(group.id);

    // Delete the group
    await noContent(v1GroupsResourceAsAdmin.deleteGroupRaw({ orgId, envId, group: group.id }));

    // Verify cascade cleanup removes the group reference
    product = await getApiProduct(apiProduct.id);
    expect(product.groups ?? []).not.toContain(group.id);
  });

  afterAll(async () => {
    if (apiProduct?.id) await deleteApiProduct(apiProduct.id);
  });
});
