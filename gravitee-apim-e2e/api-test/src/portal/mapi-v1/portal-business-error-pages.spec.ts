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

import { APP_USER, forManagementAsAdminUser, forPortalAsAnonymous, forPortalAsAppUser } from '@gravitee/utils/configuration';
import { PortalApi as PortalApi_portal } from '@gravitee/portal-webclient-sdk/src/lib/apis/PortalApi';
import { PortalApi as ManagementApi_portal } from '@gravitee/management-webclient-sdk/src/lib/apis/PortalApi';
import { ConfigurationApi as ManagementApi_configuration } from '@gravitee/management-webclient-sdk/src/lib/apis/ConfigurationApi';
import { UsersApi as ManagementApi_users } from '@gravitee/management-webclient-sdk/src/lib/apis/UsersApi';
import { GroupEntity } from '@gravitee/management-webclient-sdk/src/lib/models/GroupEntity';
import { PageEntity } from '@gravitee/management-webclient-sdk/src/lib/models/PageEntity';
import { RoleScope } from '@gravitee/management-webclient-sdk/src/lib/models/RoleScope';
import { SearchableUser } from '@gravitee/management-webclient-sdk/src/lib/models/SearchableUser';
import { PagesFaker } from '@gravitee/fixtures/management/PagesFaker';
import { fail } from '@lib/jest-utils';
import { faker } from '@faker-js/faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const configurationAsAdmin = new ManagementApi_configuration(forManagementAsAdminUser());
const portalManagementApiAsAdmin = new ManagementApi_portal(forManagementAsAdminUser());
const usersApiAsAdmin = new ManagementApi_users(forManagementAsAdminUser());

const portalPortalApiAsAnonymous = new PortalApi_portal(forPortalAsAnonymous());
const portalPortalApiAsAppUser = new PortalApi_portal(forPortalAsAppUser());

describe('Portal: Business Error - pages', () => {
  describe('401 - Authentication tests', () => {
    let createdPage: PageEntity;
    beforeAll(async () => {
      createdPage = await portalManagementApiAsAdmin.createPortalPage({
        envId,
        orgId,
        newPageEntity: PagesFaker.newPage(),
      });
    });

    test('should not not be authorized to get the page', async () => {
      await fail(
        portalPortalApiAsAnonymous.getPageByPageIdRaw({
          pageId: createdPage.id,
        }),
        401,
        {
          code: 'errors.unauthorized',
          message: `You must be authenticated to access this resource`,
        },
      );
    });

    test('should not not be authorized to get the page content', async () => {
      await fail(
        portalPortalApiAsAnonymous.getPageContentByPageIdRaw({
          pageId: createdPage.id,
        }),
        401,
        {
          code: 'errors.unauthorized',
          message: `You must be authenticated to access this resource`,
        },
      );
    });

    afterAll(async () => {
      await portalManagementApiAsAdmin.deletePortalPage({ orgId, envId, page: createdPage.id });
    });
  });

  describe('401 - Group exclusion tests', () => {
    let createdPage: PageEntity;
    let createdGroup: GroupEntity;
    let appUsers: SearchableUser[];

    beforeAll(async () => {
      createdPage = await portalManagementApiAsAdmin.createPortalPage({
        envId,
        orgId,
        newPageEntity: PagesFaker.newPage(),
      });

      // create group to exclude
      createdGroup = await configurationAsAdmin.createGroup({
        envId,
        orgId,
        newGroupEntity: {
          event_rules: [],
          name: 'excludePortalPage',
        },
      });

      // exclude created group
      await portalManagementApiAsAdmin.updatePortalPageRaw({
        orgId,
        envId,
        page: createdPage.id,
        updatePageEntity: {
          excluded_groups: [createdGroup.id],
          published: true,
          order: createdPage.order,
        },
      });

      // search APP_USER reference
      appUsers = await usersApiAsAdmin.searchUsers({
        orgId,
        envId,
        q: APP_USER.username,
      });
      expect(appUsers.length).toBeGreaterThanOrEqual(1);

      // add APP_USER reference to the group
      await configurationAsAdmin.addOrUpdateGroupMember({
        envId,
        orgId,
        group: createdGroup.id,
        groupMembership: [
          {
            reference: appUsers[0].reference,
            roles: [
              { scope: RoleScope.API, name: 'USER' },
              { scope: RoleScope.APPLICATION, name: 'USER' },
            ],
          },
        ],
      });
    });

    test('should not not be authorized to get the page', async () => {
      await fail(
        portalPortalApiAsAppUser.getPageByPageIdRaw({
          pageId: createdPage.id,
        }),
        401,
        {
          code: 'errors.unauthorized',
          message: `You must be authenticated to access this resource`,
        },
      );
    });

    test('should not not be authorized to get the page content', async () => {
      await fail(
        portalPortalApiAsAppUser.getPageContentByPageIdRaw({
          pageId: createdPage.id,
        }),
        401,
        {
          code: 'errors.unauthorized',
          message: `You must be authenticated to access this resource`,
        },
      );
    });

    afterAll(async () => {
      await portalManagementApiAsAdmin.deletePortalPage({ orgId, envId, page: createdPage.id });
      await configurationAsAdmin.deleteGroup({ orgId, envId, group: createdGroup.id });
    });
  });

  describe('404', () => {
    test('should not find the page', async () => {
      const pageId = faker.string.uuid();
      await fail(
        portalPortalApiAsAnonymous.getPageByPageIdRaw({
          pageId,
        }),
        404,
        {
          code: 'errors.page.notFound',
          message: `Page [${pageId}] cannot be found.`,
          parameters: {
            page: pageId,
          },
        },
      );
    });
  });
});
