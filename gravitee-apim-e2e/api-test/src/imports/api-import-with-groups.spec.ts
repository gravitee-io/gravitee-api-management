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
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser } from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { GroupsFaker } from '@management-fakers/GroupsFaker';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { created, succeed } from '@lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsAdminUser = new APIsApi(forManagementAsAdminUser());
const configurationsResourceAsAdminUser = new ConfigurationApi(forManagementAsAdminUser());

describe('API - Imports with groups', () => {
  describe('Create API with with group name that already exists', () => {
    const apiId = '7ffe12cc-15b9-48ff-b436-0c9bb18b2816';
    const expectedApiId = 'fbbb2c4f-2aeb-31ed-8943-f4d5b03fa892';
    const groupName = 'architecture';
    let groupId: string;

    test('should create a group with name "architecture"', async () => {
      const createdGroup = await created(
        configurationsResourceAsAdminUser.createGroupRaw({ orgId, envId, newGroupEntity: GroupsFaker.newGroup({ name: groupName }) }),
      );
      expect(createdGroup).toBeTruthy();
      expect(createdGroup.name).toStrictEqual(groupName);
      expect(createdGroup).toHaveProperty('id');
      groupId = createdGroup.id;
    });

    test('should create an API associated to the "architecture" group', async () => {
      const createdApi = await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: ApisFaker.apiImport({ id: apiId, groups: [groupName] }),
        }),
      );
      expect(createdApi).toBeTruthy();
      expect(createdApi.groups).toHaveLength(1);
      expect(createdApi.groups[0]).toStrictEqual(groupId);
    });

    afterAll(async () => {
      await configurationsResourceAsAdminUser.deleteGroup({
        orgId,
        envId,
        group: groupId,
      });

      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: expectedApiId,
      });
    });
  });

  describe('Create API with with group name that does not exists', () => {
    const apiId = '533efd8a-22e1-4483-a8af-0c24a2abd590';
    const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';
    const groupName = 'performances';
    let groupId: string;

    beforeAll(async () => {
      const createdApi = await apisResourceAsAdminUser.importApiDefinition({
        envId,
        orgId,
        body: ApisFaker.apiImport({ id: apiId, groups: [groupName] }),
      });
      expect(createdApi).toBeTruthy();
      expect(createdApi.groups).toHaveLength(1);
      groupId = createdApi.groups[0];
    });

    test('should get the created group', async () => {
      const group = await succeed(configurationsResourceAsAdminUser.getGroupRaw({ orgId, envId, group: groupId }));
      expect(group).toBeTruthy();
      expect(group.name).toStrictEqual(groupName);
    });

    afterAll(async () => {
      await configurationsResourceAsAdminUser.deleteGroup({
        orgId,
        envId,
        group: groupId,
      });

      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: expectedApiId,
      });
    });
  });
});
