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
import { test, describe, afterAll, expect } from '@jest/globals';
import { APIsApi, ApiV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementAsAdminUser, forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { created, noContent, succeed } from '@lib/jest-utils';
import { ApiEntity, GroupEntity } from '@gravitee/management-webclient-sdk/src/lib/models';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { GroupsApi } from '../../../../../lib/management-webclient-sdk/src/lib/apis/GroupsApi';
import { GroupsFaker } from '@gravitee/fixtures/management/GroupsFaker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v1GroupsResourceAsAdmin = new GroupsApi(forManagementAsAdminUser());

describe('API - V4 - Import - Gravitee Definition - With groups', () => {
  describe('Create v4 API from import with groups', () => {
    let importedApi: ApiV4;
    let group: GroupEntity;

    test('should create group', async () => {
      group = await created(
        v1GroupsResourceAsAdmin.createGroupRaw({
          orgId,
          envId,
          newGroupEntity: GroupsFaker.newGroup(),
        }),
      );
    });

    test('should import v4 API with member', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            api: MAPIV2ApisFaker.apiV4({
              groups: [group.id],
            }),
          }),
        }),
      );
      expect(importedApi).toBeTruthy();
      expect(importedApi.groups).toHaveLength(1);
      expect(importedApi.groups).toContain(group.id);
    });

    test('should get group with imported api assigned to it', async () => {
      // No v2 resource to get members of an API
      const groupMemberShips = await succeed(
        v1GroupsResourceAsAdmin.getGroupMembershipsRaw({
          orgId,
          envId,
          group: group.id,
          type: 'api',
        }),
      );
      expect(groupMemberShips).toBeTruthy();
      expect(groupMemberShips).toHaveLength(1);
      expect((groupMemberShips[0] as ApiEntity).id).toEqual(importedApi.id);
    });

    afterAll(async () => {
      await noContent(
        v1GroupsResourceAsAdmin.deleteGroupRaw({
          orgId,
          envId,
          group: group.id,
        }),
      );
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });
  });
});
