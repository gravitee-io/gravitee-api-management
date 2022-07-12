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
import { created, succeed } from '@lib/jest-utils';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { GroupsApi } from '@management-apis/GroupsApi';
import { GroupsFaker } from '@management-fakers/GroupsFaker';

const apisResource = new APIsApi(forManagementAsAdminUser());
const groupsResource = new GroupsApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Update API from import with groups', () => {
  describe('Update API with with group name that already exists', () => {
    const expectedApiId = 'd2de4a96-6c5c-33d1-a220-8d41f8aecdb1';

    const groupName = 'customers';
    const fakeGroup = GroupsFaker.newGroup({ name: groupName });
    const fakeApi = ApisFaker.apiImport({ id: '70fbb369-5672-43e6-8a8c-ff7aa81a6055' });

    let groupId;
    let apiUpdate;

    test('should create a group with name "customers"', async () => {
      let createdGroup = await created(groupsResource.createGroupRaw({ envId, orgId, newGroupEntity: fakeGroup }));
      expect(createdGroup.name).toBe('customers');
      expect(createdGroup.id).toBeDefined();
      groupId = createdGroup.id;
    });

    test('should create an API associated with no groups', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.groups.length).toBe(0);
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update the API, associating it to the group "customers"', async () => {
      apiUpdate.groups = ['customers'];

      let updatedApi = await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
      expect(updatedApi.id).toBeTruthy();
      expect(updatedApi.groups).toHaveLength(1);
      expect(updatedApi.groups[0]).toBe(groupId);
    });

    afterAll(async () => {
      await groupsResource.deleteGroup({ envId, orgId, group: groupId });
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API with with group name that does not exists', () => {
    const expectedApiId = 'e6908cb0-4e56-3b9c-b205-de8d49016a50';

    const groupName = 'sales';
    const fakeApi = ApisFaker.apiImport({ id: 'bc071378-7fb5-45df-841a-a2518668ae60', groups: ['support'] });

    let groupId;
    let apiUpdate;

    test('should create an API associated with no groups', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.groups).toHaveLength(1);
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update the API, associating it to the group "sales"', async () => {
      apiUpdate.groups = [groupName];

      let updatedApi = await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
      expect(updatedApi.id).toBeTruthy();
      expect(updatedApi.groups).toHaveLength(1);
      groupId = updatedApi.groups[0];
    });

    test('should get the created group', async () => {
      let foundGroup = await succeed(groupsResource.getGroupRaw({ envId, orgId, group: groupId }));
      expect(foundGroup.name).toBe('sales');
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      await groupsResource.deleteGroup({ envId, orgId, group: groupId });
    });
  });
});
