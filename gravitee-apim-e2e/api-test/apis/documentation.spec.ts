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
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { forManagementAsAdminUser, forManagementAsApiUser, forPortal, forPortalAsApplicationUser } from '@client-conf/*';
import { UsersApi } from '@management-apis/UsersApi';
import { RoleScope } from '@management-models/RoleScope';
import { APIsApi } from '@management-apis/APIsApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { Visibility } from '@management-models/Visibility';
import { APIPagesApi } from '@management-apis/APIPagesApi';
import { created, succeed } from '../../lib/jest-utils';
import { PageType } from '@management-models/PageType';
import { ApiApi } from '@portal-apis/ApiApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const configurationApi = new ConfigurationApi(forManagementAsAdminUser());
const usersApi = new UsersApi(forManagementAsAdminUser());
const apisApi = new APIsApi(forManagementAsApiUser());
const apiPagesApi = new APIPagesApi(forManagementAsApiUser());
const apiApi = new ApiApi(forPortalAsApplicationUser());

let createdReaderGroup;
let userMember;
let createdApi;
let homepage;
let page;
let folder;
let pageInFolder;
let pageOutsideFolder;

describe('Documentation', () => {
  beforeAll(async () => {
    // Create reader group
    createdReaderGroup = await configurationApi.createGroup({
      orgId,
      envId,
      newGroupEntity: {
        name: 'Postman Reader',
        event_rules: [
          {
            event: 'API_CREATE',
          },
        ],
        lock_api_role: false,
        lock_application_role: false,
        disable_membership_notifications: false,
      },
    });

    // Get user member
    const q = 'user';
    const response = await usersApi.searchUsers({ orgId, envId, q });
    userMember = response.find((user) => user.displayName === q);

    // Add reader member to group
    await configurationApi.addOrUpdateGroupMember({
      orgId,
      envId,
      group: createdReaderGroup.id,
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

    // import api
    createdApi = await apisApi.importApiDefinition({
      orgId,
      envId,
      body: {
        proxy: {
          endpoints: [
            {
              name: 'default',
              target: 'https://api.gravitee.io/echo',
              inherit: true,
            },
          ],
          context_path: '/postman-documentation',
        },
        pages: [],
        plans: [],
        tags: [],
        name: 'Test Postman',
        description: 'This is an API',
        version: '1',
      },
    });

    // publish api
    await apisApi.updateApi({
      api: createdApi.id,
      updateApiEntity: ApisFaker.updateApiFromApiEntity(createdApi, {
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        visibility: Visibility.PUBLIC,
      }),
      orgId,
      envId,
    });
  });

  afterAll(async () => {
    await configurationApi.deleteGroup({ orgId, envId, group: createdReaderGroup.id });
    await apisApi.deleteApi({ orgId, envId, api: createdApi.id });
  });

  describe('Create UNPUBLISHED homepage and page', () => {
    test('should create an homepage', async () => {
      homepage = await created(
        apiPagesApi.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'Homepage',
            type: PageType.MARKDOWN,
            parentId: '',
            content: 'This is my homepage',
            homepage: true,
          },
        }),
      );

      expect(homepage.parentPath).toEqual('');
      expect(homepage.published).toBeFalsy();
    });

    test('should create a page', async () => {
      page = await created(
        apiPagesApi.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'documentation.md',
            type: PageType.MARKDOWN,
            parentId: '',
            content: '# Here is a documentation file\n\nFake documentation',
          },
        }),
      );

      expect(page.parentPath).toEqual('');
      expect(page.published).toBeFalsy();
    });
  });

  describe('Check documentation on portal', () => {
    test('Get homepage empty result', async () => {
      const pages = await apiApi.getPagesByApiId({ apiId: createdApi.id, homepage: true });
      expect(pages.data).toHaveLength(0);
    });

    test('Get pages empty result', async () => {
      const pages = await apiApi.getPagesByApiId({ apiId: createdApi.id, homepage: false });
      expect(pages.data).toHaveLength(0);
    });
  });

  describe('Publish previous pages', async () => {
    await succeed(
      apiPagesApi.partialUpdateApiPageRaw({
        orgId,
        envId,
        api: createdApi.id,
        page: homepage.id,
        updatePageEntity: {
          published: true,
        },
      }),
    );

    await succeed(
      apiPagesApi.partialUpdateApiPageRaw({
        orgId,
        envId,
        api: createdApi.id,
        page: page.id,
        updatePageEntity: {
          published: true,
        },
      }),
    );
  });

  describe('Check documentation on portal after publication', () => {
    test('Get homepage contains created homepage', async () => {
      const pages = await apiApi.getPagesByApiId({ apiId: createdApi.id, homepage: true });
      expect(pages.data.length).toBeGreaterThan(0);
    });

    test('Get pages contains created page', async () => {
      const pages = await apiApi.getPagesByApiId({ apiId: createdApi.id, homepage: false });
      expect(pages.data.length).toBeGreaterThan(0);
    });
  });

  describe('Create FOLDER with one page and move one into it', () => {
    test('Create folder', async () => {
      folder = await created(
        apiPagesApi.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'Folder',
            type: PageType.FOLDER,
            parentId: '',
          },
        }),
      );

      expect(page.parentPath).toEqual('');
      expect(page.published).toBeFalsy();
    });

    test('Publish folder', async () => {
      page = await succeed(
        apiPagesApi.partialUpdateApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          page: folder.id,
          updatePageEntity: {
            published: true,
          },
        }),
      );
      expect(page.published).toBeTruthy();
    });

    test('Create PUBLISHED page inside folder', async () => {
      pageInFolder = await created(
        apiPagesApi.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'In folder page',
            type: PageType.MARKDOWN,
            parentId: folder.id,
            content: 'this page is created directly inside folder',
            published: true,
          },
        }),
      );

      expect(pageInFolder.parentPath).toEqual(folder.id);
      expect(pageInFolder.published).toBeTruthy();
    });

    test('Create PUBLISHED page outside folder', async () => {
      pageOutsideFolder = await created(
        apiPagesApi.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'Outside folder',
            type: PageType.MARKDOWN,
            parentId: '',
            content: 'This is a page created outside the folder',
            published: true,
          },
        }),
      );

      expect(pageOutsideFolder.parentPath).toEqual('');
      expect(pageOutsideFolder.published).toBeTruthy();
    });

    test('Move outside page inside folder', async () => {
      pageOutsideFolder = await succeed(
        apiPagesApi.partialUpdateApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          page: pageOutsideFolder.id,
          updatePageEntity: {
            parentId: folder.id,
          },
        }),
      );
      expect(pageOutsideFolder.parentPath).toEqual(folder.id);
    });
  });
});
