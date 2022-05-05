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
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { succeed } from '../../lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceUser = new APIsApi(forManagementAsApiUser());
const apisResourceAdmin = new APIsApi(forManagementAsAdminUser());

let userApi;
let adminApi;

describe('API Search', () => {
  beforeAll(async () => {
    [userApi, adminApi] = await Promise.all([
      apisResourceUser.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi({ name: ApisFaker.uniqueWord() }),
      }),
      apisResourceAdmin.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi({ name: ApisFaker.uniqueWord() }),
      }),
    ]);
    return [userApi, adminApi];
  });

  test('should find API of API_USER as ADMIN_USER', async () => {
    const apiListItems = await succeed(apisResourceAdmin.searchApisRaw({ orgId, envId, q: userApi.name }));
    expect(apiListItems).toHaveLength(1);
    expect(apiListItems[0].name).toEqual(userApi.name);
  });

  test('should find API of ADMIN_USER as ADMIN_USER', async () => {
    const apiListItems = await succeed(apisResourceAdmin.searchApisRaw({ orgId, envId, q: adminApi.name }));
    expect(apiListItems).toHaveLength(1);
    expect(apiListItems[0].name).toEqual(adminApi.name);
  });

  test('should not find API of ADMIN_USER as API_USER', async () => {
    const apiListItems = await succeed(apisResourceUser.searchApisRaw({ orgId, envId, q: adminApi.name }));
    expect(apiListItems).toHaveLength(0);
  });

  test('should find API of API_USER as API_USER', async () => {
    const apiListItems = await succeed(apisResourceUser.searchApisRaw({ orgId, envId, q: userApi.name }));
    expect(apiListItems).toHaveLength(1);
    expect(apiListItems[0].name).toEqual(userApi.name);
  });

  afterAll(async () => {
    await apisResourceAdmin.deleteApi({ orgId, envId, api: userApi.id });
    return await apisResourceAdmin.deleteApi({ orgId, envId, api: adminApi.id });
  });
});
