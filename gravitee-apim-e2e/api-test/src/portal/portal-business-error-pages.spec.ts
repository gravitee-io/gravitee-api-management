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

import { APP_USER, forManagementAsAdminUser, forPortalAsAnonymous, forPortalAsAppUser } from '@client-conf/*';
import { APIsApi } from '@management-apis/APIsApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { fail } from '@lib/jest-utils';
import { ApiEntity } from '@management-models/ApiEntity';
import { PagesFaker } from '@management-fakers/PagesFaker';
import { PortalApi } from '@portal-apis/PortalApi';
import { PortalApi as ManagementPortalApi } from '@management-apis/PortalApi';
import { PageEntity } from '@management-models/PageEntity';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { GroupEntity } from '@management-models/GroupEntity';
import { UsersApi } from '@management-apis/UsersApi';
import { SearchableUser } from '@management-models/SearchableUser';
import { RoleScope } from '@management-models/RoleScope';
import faker from '@faker-js/faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisManagementApiAsAdmin = new APIsApi(forManagementAsAdminUser());
const configurationAsAdmin = new ConfigurationApi(forManagementAsAdminUser());
const managementPortalApiAsAdmin = new ManagementPortalApi(forManagementAsAdminUser());
const usersApiAsAdmin = new UsersApi(forManagementAsAdminUser());
const portalApiAsAnonymous = new PortalApi(forPortalAsAnonymous());
const portalApiAsAppUser = new PortalApi(forPortalAsAppUser());

describe('Portal: Business Error - pages', () => {
  let createdApi: ApiEntity;
  beforeAll(async () => {
    createdApi = await apisManagementApiAsAdmin.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi(),
    });
  });

  describe('400', () => {
    test('should not create page without type', async () => {
      await fail(
        apisManagementApiAsAdmin.createApiPageRaw({
          envId,
          orgId,
          api: createdApi.id,
          newPageEntity: PagesFaker.newPage({ type: undefined }),
        }),
        400,
      );
    });
  });

  describe('401 - Authentication tests', () => {
    let createdPage: PageEntity;
    beforeAll(async () => {
      createdPage = await apisManagementApiAsAdmin.createApiPage({
        envId,
        orgId,
        api: createdApi.id,
        newPageEntity: PagesFaker.newPage(),
      });
    });

    test('should not not be authorized to get the page', async () => {
      await fail(
        portalApiAsAnonymous.getPageByPageIdRaw({
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
        portalApiAsAnonymous.getPageContentByPageIdRaw({
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
      await apisManagementApiAsAdmin.deleteApiPage({ orgId, envId, page: createdPage.id, api: createdApi.id });
    });
  });

  describe('401 - Group exclusion tests', () => {
    let createdPage: PageEntity;
    let createdGroup: GroupEntity;
    let appUsers: SearchableUser[];

    beforeAll(async () => {
      createdPage = await apisManagementApiAsAdmin.createApiPage({
        envId,
        orgId,
        api: createdApi.id,
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
      await managementPortalApiAsAdmin.updatePortalPageRaw({
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
        portalApiAsAppUser.getPageByPageIdRaw({
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
        portalApiAsAppUser.getPageContentByPageIdRaw({
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
      await apisManagementApiAsAdmin.deleteApiPage({ orgId, envId, page: createdPage.id, api: createdApi.id });
      await configurationAsAdmin.deleteGroup({ orgId, envId, group: createdGroup.id });
    });
  });

  describe('404', () => {
    test('should not find the page', async () => {
      const pageId = faker.datatype.uuid();
      await fail(
        portalApiAsAnonymous.getPageByPageIdRaw({
          pageId,
        }),
        404,
        {
          code: 'errors.page.notFound',
          message: `Page [${pageId}] can not be found.`,
          parameters: {
            page: pageId,
          },
        },
      );
    });
  });

  afterAll(async () => {
    await apisManagementApiAsAdmin.deleteApi({ orgId, envId, api: createdApi.id });
  });
});
