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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import faker from '@faker-js/faker';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { forManagementAsAdminUser, forManagementAsAppUser, forPortalAsApiUser, forPortalAsSimpleUser } from '@client-conf/*';
import { forbidden, succeed } from '../../lib/jest-utils';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { UsersApi } from '@management-apis/UsersApi';
import { RoleScope } from '@management-models/RoleScope';
import { ApplicationApi } from '@portal-apis/ApplicationApi';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { UpdateApplicationEntityFromJSON, UpdateApplicationEntityFromJSONTyped } from '@management-models/UpdateApplicationEntity';
import { GroupEntity } from '@management-models/GroupEntity';
import { MemberEntity } from '@management-models/MemberEntity';
import { SearchableUser } from '@management-models/SearchableUser';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { GroupsFaker } from '@management-fakers/GroupsFaker';

const applicationManagementApiAsAppUser = new ApplicationsApi(forManagementAsAppUser());
const configurationManagementApiAsAdminUser = new ConfigurationApi(forManagementAsAdminUser());
const usersManagementApiAsAdminUser = new UsersApi(forManagementAsAdminUser());
const applicationPortalApiAsApiUser = new ApplicationApi(forPortalAsApiUser());
const applicationPortalApiAsSimpleUser = new ApplicationApi(forPortalAsSimpleUser());
const orgId = 'DEFAULT';
const envId = 'DEFAULT';

let application: ApplicationEntity;
let group: GroupEntity;
let userMember: SearchableUser;

describe('Applications - Membership', () => {
  beforeAll(async () => {
    application = await applicationManagementApiAsAppUser.createApplication({
      orgId,
      envId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });

    group = await configurationManagementApiAsAdminUser.createGroup({
      orgId,
      envId,
      newGroupEntity: GroupsFaker.newGroup({ event_rules: [{ event: 'API_CREATE' }] }),
    });

    // Get user member
    const q = 'user';
    const response = await usersManagementApiAsAdminUser.searchUsers({ orgId, envId, q });
    userMember = response.find((user) => user.displayName === q);

    // Add reader member to group
    await configurationManagementApiAsAdminUser.addOrUpdateGroupMemberRaw({
      orgId,
      envId,
      group: group.id,
      groupMembership: [
        {
          reference: userMember.reference,
          roles: [
            {
              scope: RoleScope.API,
              name: 'USER',
            },
            {
              scope: RoleScope.APPLICATION,
              name: `USER`,
            },
          ],
        },
      ],
    });
  });

  describe('Check documentation on portal', () => {
    test('Get applications does not contain created application', async () => {
      const applications = await succeed(applicationPortalApiAsApiUser.getApplicationsRaw({ size: -1 }));
      expect(applications.data.map(({ id }) => id)).not.toContain(application.id);
    });

    test('Get application forbidden', async () => {
      await forbidden(applicationPortalApiAsApiUser.getApplicationByApplicationIdRaw({ applicationId: application.id }));
    });
  });

  describe('Add Member', () => {
    beforeAll(async () => {
      const q = 'api1';
      const response = await usersManagementApiAsAdminUser.searchUsers({ orgId, envId, q });
      const apiMember = response.find((user) => user.displayName === q);

      await applicationManagementApiAsAppUser.addOrUpdateApplicationMemberRaw({
        orgId,
        envId,
        application: application.id,
        applicationMembership: {
          reference: apiMember.reference,
          role: 'USER',
        },
      });
    });

    describe('Check documentation on portal', () => {
      test('Get applications does not contain created application', async () => {
        const applications = await succeed(applicationPortalApiAsApiUser.getApplicationsRaw({ size: -1 }));
        expect(applications.data.map(({ id }) => id)).toEqual(expect.arrayContaining([application.id]));
      });

      test('Get application ok', async () => {
        await succeed(applicationPortalApiAsApiUser.getApplicationByApplicationIdRaw({ applicationId: application.id }));
      });
    });
  });

  describe('Exclude group', () => {
    test('INCLUDED Get applications does not contain created application', async () => {
      const applications = await succeed(applicationPortalApiAsSimpleUser.getApplicationsRaw({ size: -1 }));
      expect(applications.data.map(({ id }) => id)).not.toEqual(expect.arrayContaining([application.id]));
    });
    test('INCLUDED Get applications contains created application', async () => {
      const updateApplicationEntity = UpdateApplicationEntityFromJSON({ ...application, groups: [group.id] });
      await succeed(
        applicationManagementApiAsAppUser.updateApplicationRaw({ orgId, envId, application: application.id, updateApplicationEntity }),
      );

      const applications = await succeed(applicationPortalApiAsSimpleUser.getApplicationsRaw({ size: -1 }));
      expect(applications.data.map(({ id }) => id)).toEqual(expect.arrayContaining([application.id]));
    });
  });

  afterAll(async () => {
    await applicationManagementApiAsAppUser.deleteApplication({
      orgId,
      envId,
      application: application.id,
    });
    await configurationManagementApiAsAdminUser.deleteGroup({ orgId, envId, group: group.id });
  });
});
