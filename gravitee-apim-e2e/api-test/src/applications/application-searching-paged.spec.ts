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
import { afterAll, beforeAll, expect } from '@jest/globals';
import faker from '@faker-js/faker';
import { find } from 'lodash';
import {
  ADMIN_USER,
  API_USER,
  APP_USER,
  forManagementAsAdminUser,
  forManagementAsApiUser,
  forManagementAsAppUser,
} from '@gravitee/utils/configuration';

import { forbidden, succeed } from '@lib/jest-utils';
import { ApplicationsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ApplicationsApi';
import { ApplicationEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApplicationEntity';
import { GroupsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupsApi';
import { GroupEntity } from '@gravitee/management-webclient-sdk/src/lib/models/GroupEntity';
import { GroupMembershipsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/GroupMembershipsApi';
import { MemberRoleEntityFromJSON } from '@gravitee/management-webclient-sdk/src/lib/models/MemberRoleEntity';
import { SearchableUser } from '@gravitee/management-webclient-sdk/src/lib/models/SearchableUser';
import { UsersApi } from '@gravitee/management-webclient-sdk/src/lib/apis/UsersApi';

const applicationManagementApiAsAdminUser = new ApplicationsApi(forManagementAsAdminUser());
const applicationManagementApiAsAppUser = new ApplicationsApi(forManagementAsAppUser());
const applicationManagementApiAsApiUser = new ApplicationsApi(forManagementAsApiUser());
const groupsApi = new GroupsApi(forManagementAsAdminUser());
const usersResourceAsApiUser = new UsersApi(forManagementAsApiUser());
const groupMembershipsApi = new GroupMembershipsApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

let createdApplication1: ApplicationEntity;
let createdApplication2: ApplicationEntity;
let createdApplication3: ApplicationEntity;
let createdApplication4: ApplicationEntity;
let createdApplication5: ApplicationEntity;
let createdGroup: GroupEntity;

describe('Applications - Searching - Paged', () => {
  beforeAll(async () => {
    const createdAppsAsAppUser = [1, 2, 3]
      .map((num) => ({
        name: `${faker.random.word()}-${faker.datatype.uuid()}`,
        description: `Application ${num} for "Applications searching scenario"`,
      }))
      .map(
        async (newApplicationEntity) =>
          await applicationManagementApiAsAppUser.createApplication({
            orgId,
            envId,
            newApplicationEntity,
          }),
      );

    [createdApplication1, createdApplication2, createdApplication3] = await Promise.all(createdAppsAsAppUser);

    [createdGroup] = await Promise.all(
      [
        {
          name: `${faker.random.word()}-${faker.datatype.uuid()}`,
        },
      ].map(
        async (newGroupEntity) =>
          await groupsApi.createGroup({
            orgId,
            envId,
            newGroupEntity,
          }),
      ),
    );

    const createdAppsAsApiUser = [4, 5]
      .map((num) => ({
        name: `${faker.random.word()}-${faker.datatype.uuid()}`,
        description: `Application ${num} for "Applications searching scenario"`,
        groups: [createdGroup.id],
      }))
      .map(
        async (newApplicationEntity) =>
          await applicationManagementApiAsApiUser.createApplication({
            orgId,
            envId,
            newApplicationEntity,
          }),
      );

    [createdApplication4, createdApplication5] = await Promise.all(createdAppsAsApiUser);

    // get user member
    const users: SearchableUser[] = await usersResourceAsApiUser.searchUsers({
      envId,
      orgId,
      q: process.env.API_USERNAME,
    });
    const userMember = find(users, (user) => user.displayName === process.env.API_USERNAME);

    // add member user to the group
    await groupMembershipsApi.addOrUpdateGroupMember({
      envId,
      orgId,
      group: createdGroup.id,
      groupMembership: [
        {
          id: userMember.id,
          reference: userMember.reference,
          roles: [
            { scope: 'API', name: 'USER' },
            { scope: 'APPLICATION', name: 'USER' },
          ].map(MemberRoleEntityFromJSON),
        },
      ],
    });

    // archive the application5
    const deletedAppsAsApiUser = [createdApplication5].map(
      async (application) =>
        await applicationManagementApiAsApiUser.deleteApplication({
          orgId,
          envId,
          application: application.id,
        }),
    );
    await Promise.all(deletedAppsAsApiUser);
  });

  test(`Search all for ${APP_USER.username}`, async () => {
    const applications = await succeed(applicationManagementApiAsAppUser.getApplicationsPagedRaw({ orgId, envId }));
    expect(applications.page.size).toBeGreaterThanOrEqual(3);
    expect(applications.data.map(({ id }) => id)).toEqual(
      expect.arrayContaining([createdApplication1, createdApplication2, createdApplication3].map(({ id }) => id)),
    );
  });

  test(`Search all ARCHIVED for ${APP_USER.username}`, async () => {
    await forbidden(applicationManagementApiAsAppUser.getApplicationsPagedRaw({ orgId, envId, status: 'ARCHIVED' }));
  });

  test(`Search an app by name for ${APP_USER.username}`, async () => {
    const applications = await succeed(
      applicationManagementApiAsAppUser.getApplicationsPagedRaw({ orgId, envId, query: createdApplication3.name }),
    );
    expect(applications.page.size).toBeGreaterThanOrEqual(1);
    expect(applications.data[0].name).toEqual(createdApplication3.name);
  });

  test(`Search all for ${ADMIN_USER.username}`, async () => {
    const applications = await succeed(applicationManagementApiAsAdminUser.getApplicationsPagedRaw({ orgId, envId }));
    expect(applications.page.size).toBeGreaterThanOrEqual(4);
    expect(applications.data.map(({ id }) => id)).toEqual(
      expect.arrayContaining([createdApplication1, createdApplication2, createdApplication3, createdApplication4].map(({ id }) => id)),
    );
  });

  test(`Search all for ${APP_USER.username} by page (page=1&size=2) order by name`, async () => {
    const applications = await succeed(
      applicationManagementApiAsAdminUser.getApplicationsPagedRaw({ orgId, envId, page: 1, size: 1, order: 'name' }),
    );

    expect(applications.page.size).toEqual(1);
    expect(applications.page.total_elements).toBeGreaterThanOrEqual(1);
    expect(applications.page.total_pages).toBeGreaterThanOrEqual(1);
  });

  test(`Search all for ${ADMIN_USER.username} by page (page=1&size=2) order by -updated_at`, async () => {
    const applications = await succeed(
      applicationManagementApiAsAdminUser.getApplicationsPagedRaw({ orgId, envId, page: 1, size: 2, order: '-name' }),
    );
    expect(applications.page.size).toEqual(2);
    expect(applications.page.total_elements).toBeGreaterThanOrEqual(4);
    expect(applications.page.total_pages).toBeGreaterThanOrEqual(2);
  });

  test(`Search all ACTIVE for ${API_USER.username}`, async () => {
    const applications = await succeed(applicationManagementApiAsApiUser.getApplicationsPagedRaw({ orgId, envId, status: 'ACTIVE' }));
    const archivedApplications = applications.data.filter((a) => a.status === 'ARCHIVED');
    expect(archivedApplications.length).toEqual(0);
  });

  afterAll(async () => {
    const deletedAppsAsAppUser = [createdApplication1, createdApplication2, createdApplication3].map(
      async (application) =>
        await applicationManagementApiAsAppUser.deleteApplication({
          orgId,
          envId,
          application: application.id,
        }),
    );
    await Promise.all(deletedAppsAsAppUser);
    const deletedAppsAsApiUser = [createdApplication4].map(
      async (application) =>
        await applicationManagementApiAsApiUser.deleteApplication({
          orgId,
          envId,
          application: application.id,
        }),
    );
    await Promise.all(deletedAppsAsApiUser);
    const deletedGroup = [createdGroup].map(
      async (group) =>
        await groupsApi.deleteGroup({
          orgId,
          envId,
          group: group.id,
        }),
    );
    await Promise.all(deletedGroup);
  });
});
