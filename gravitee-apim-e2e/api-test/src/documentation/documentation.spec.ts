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
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { forManagementAsAdminUser, forManagementAsApiUser, forPortalAsApplicationFrenchUser, forPortalAsSimpleUser } from '@client-conf/*';
import { UsersApi } from '@management-apis/UsersApi';
import { RoleScope } from '@management-models/RoleScope';
import { APIsApi } from '@management-apis/APIsApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { Visibility } from '@management-models/Visibility';
import { APIPagesApi } from '@management-apis/APIPagesApi';
import { created, succeed, unauthorized } from '@lib/jest-utils';
import { PageType } from '@management-models/PageType';
import { ApiApi, GetPageByApiIdAndPageIdIncludeEnum } from '@portal-apis/ApiApi';
import { MetadataFormat } from '@management-models/MetadataFormat';
import { APIMetadataApi } from '@management-apis/APIMetadataApi';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const configurationManagementApiAsAdminUser = new ConfigurationApi(forManagementAsAdminUser());
const usersManagementApiAsAdminUser = new UsersApi(forManagementAsAdminUser());
const apisManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());
const apiPagesManagementApiAsApiUser = new APIPagesApi(forManagementAsApiUser());
const apiPortalAsApplicationFrenchUser = new ApiApi(forPortalAsApplicationFrenchUser());
const apiPortalAsSimpleUser = new ApiApi(forPortalAsSimpleUser());

let createdReaderGroup;
let userMember;
let createdApi;
let homepage;
let createdPage;
let createdFolder;
let pageInFolder;
let pageOutsideFolder;
let createdMetadata;
let createdPageMetadata;
let createdLink;

describe('Documentation', () => {
  beforeAll(async () => {
    // Create reader group
    createdReaderGroup = await configurationManagementApiAsAdminUser.createGroup({
      orgId,
      envId,
      newGroupEntity: {
        name: `E2e Doc Reader -${faker.random.word()}-${faker.datatype.uuid()}`,
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
    const response = await usersManagementApiAsAdminUser.searchUsers({ orgId, envId, q });
    userMember = response.find((user) => user.displayName === q);

    // Add reader member to group
    await configurationManagementApiAsAdminUser.addOrUpdateGroupMemberRaw({
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
    createdApi = await apisManagementApiAsApiUser.importApiDefinition({
      orgId,
      envId,
      body: ApisFaker.apiImport({
        name: 'Test documentation e2e',
        description: 'This is an API',
      }),
    });

    // publish api
    await apisManagementApiAsApiUser.updateApiRaw({
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({
        ...createdApi,
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        visibility: Visibility.PUBLIC,
      }),
      orgId,
      envId,
    });
  });

  afterAll(async () => {
    await apisManagementApiAsApiUser.updateApiRaw({
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({ ...createdApi, lifecycle_state: ApiLifecycleState.UNPUBLISHED }),
      orgId,
      envId,
    });
    await configurationManagementApiAsAdminUser.deleteGroupRaw({ orgId, envId, group: createdReaderGroup.id });
    await apiPagesManagementApiAsApiUser.deleteApiPageRaw({ orgId, envId, api: createdApi.id, page: createdPageMetadata.id });
    await apiPagesManagementApiAsApiUser.deleteApiPageRaw({ orgId, envId, api: createdApi.id, page: pageOutsideFolder.id });
    await apiPagesManagementApiAsApiUser.deleteApiPageRaw({ orgId, envId, api: createdApi.id, page: pageInFolder.id });
    await apiPagesManagementApiAsApiUser.deleteApiPageRaw({ orgId, envId, api: createdApi.id, page: createdFolder.id });
    await apiPagesManagementApiAsApiUser.deleteApiPageRaw({ orgId, envId, api: createdApi.id, page: createdLink.id });
    await apiPagesManagementApiAsApiUser.deleteApiPageRaw({ orgId, envId, api: createdApi.id, page: createdPage.id });
    await apiPagesManagementApiAsApiUser.deleteApiPageRaw({ orgId, envId, api: createdApi.id, page: homepage.id });
    await apisManagementApiAsApiUser.deleteApiRaw({ orgId, envId, api: createdApi.id });
  });

  describe('Create UNPUBLISHED homepage and createdPage', () => {
    test('should create an homepage', async () => {
      homepage = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
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

    test('should create a createdPage', async () => {
      createdPage = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
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

      expect(createdPage.parentPath).toEqual('');
      expect(createdPage.published).toBeFalsy();
    });

    describe('Check documentation on portal', () => {
      test('Get homepage empty result', async () => {
        const pages = await apiPortalAsApplicationFrenchUser.getPagesByApiId({ apiId: createdApi.id, homepage: true });
        expect(pages.data).toHaveLength(0);
      });

      test('Get pages empty result', async () => {
        const pages = await apiPortalAsApplicationFrenchUser.getPagesByApiId({ apiId: createdApi.id, homepage: false });
        expect(pages.data).toHaveLength(0);
      });
    });
  });

  describe('Publish previous pages', () => {
    test('Publish homepage', async () => {
      homepage = await succeed(
        apiPagesManagementApiAsApiUser.partialUpdateApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          page: homepage.id,
          updatePageEntity: {
            published: true,
          },
        }),
      );

      expect(homepage.published).toBeTruthy();
    });

    test('Publish createdPage', async () => {
      createdPage = await succeed(
        apiPagesManagementApiAsApiUser.partialUpdateApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          page: createdPage.id,
          updatePageEntity: {
            published: true,
          },
        }),
      );
      expect(createdPage.published).toBeTruthy();
    });

    describe('Check documentation on portal', () => {
      test('Get homepage contains created homepage', async () => {
        const pages = await succeed(
          apiPortalAsApplicationFrenchUser.getPagesByApiIdRaw({
            apiId: createdApi.id,
            homepage: true,
          }),
        );
        expect(pages.data.length).toBeGreaterThan(0);
        expect(pages.data.find(({ id }) => id === homepage.id)).toBeDefined();
      });

      test('Get pages contains created createdPage', async () => {
        const pages = await succeed(
          apiPortalAsApplicationFrenchUser.getPagesByApiIdRaw({
            apiId: createdApi.id,
            homepage: false,
          }),
        );
        expect(pages.data.length).toBeGreaterThan(0);
        expect(pages.data.find(({ id }) => id === createdPage.id)).toBeDefined();
      });
    });
  });

  describe('Create FOLDER with one createdPage and move one into it', () => {
    test('Create createdFolder', async () => {
      createdFolder = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
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

      expect(createdFolder.parentPath).toEqual('');
      expect(createdFolder.published).toBeFalsy();
    });

    test('Publish createdFolder', async () => {
      createdFolder = await succeed(
        apiPagesManagementApiAsApiUser.partialUpdateApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          page: createdFolder.id,
          updatePageEntity: {
            published: true,
          },
        }),
      );
      expect(createdFolder.published).toBeTruthy();
    });

    test('Create PUBLISHED createdPage inside createdFolder', async () => {
      pageInFolder = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'In createdFolder createdPage',
            type: PageType.MARKDOWN,
            parentId: createdFolder.id,
            content: 'this createdPage is created directly inside createdFolder',
            published: true,
          },
        }),
      );

      expect(pageInFolder.type).toEqual(PageType.MARKDOWN);
      expect(pageInFolder.parentId).toEqual(createdFolder.id);
      expect(pageInFolder.published).toBeTruthy();
    });

    test('Create PUBLISHED createdPage outside createdFolder', async () => {
      pageOutsideFolder = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'Outside createdFolder',
            type: PageType.MARKDOWN,
            parentId: '',
            content: 'This is a createdPage created outside the createdFolder',
            published: true,
          },
        }),
      );

      expect(pageOutsideFolder.parentPath).toEqual('');
      expect(pageOutsideFolder.published).toBeTruthy();
    });

    test('Move outside createdPage inside createdFolder', async () => {
      pageOutsideFolder = await succeed(
        apiPagesManagementApiAsApiUser.partialUpdateApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          page: pageOutsideFolder.id,
          updatePageEntity: {
            parentId: createdFolder.id,
          },
        }),
      );
      expect(pageInFolder.parentId).toEqual(createdFolder.id);
    });

    describe('Check documentation on portal', () => {
      test('Get homepage contains created homepage', async () => {
        const pages = await succeed(
          apiPortalAsApplicationFrenchUser.getPagesByApiIdRaw({
            apiId: createdApi.id,
            homepage: true,
          }),
        );
        expect(pages.data.length).toBeGreaterThan(0);
        expect(pages.data.find(({ id }) => id === homepage.id)).toBeDefined();
      });

      test('Get pages contains created createdPage', async () => {
        const pages = await succeed(
          apiPortalAsApplicationFrenchUser.getPagesByApiIdRaw({
            apiId: createdApi.id,
            homepage: false,
          }),
        );
        expect(pages.data.length).toBeGreaterThan(0);
        expect(pages.data.find(({ id }) => id === createdPage.id)).toBeDefined();
      });

      test('Get createdPage OK', async () => {
        const pageResult = await succeed(
          apiPortalAsApplicationFrenchUser.getPageByApiIdAndPageIdRaw({
            apiId: createdApi.id,
            pageId: pageOutsideFolder.id,
            include: [GetPageByApiIdAndPageIdIncludeEnum.Content],
          }),
        );
        expect(pageResult.name).toEqual(pageOutsideFolder.name);
        expect(pageResult.content).toEqual(pageOutsideFolder.content);
      });
    });
  });

  describe('Add metadata with templating', () => {
    const metadataApi = new APIMetadataApi(forManagementAsApiUser());

    test('Add API state metadata', async () => {
      createdMetadata = await created(
        metadataApi.createApiMetadataRaw({
          orgId,
          envId,
          api: createdApi.id,
          newApiMetadataEntity: { format: MetadataFormat.STRING, name: 'api-state', value: '${api.state}' },
        }),
      );
      expect(createdMetadata.key).toEqual('api-state');
    });
    test('Create PUBLISHED createdPage using templating', async () => {
      const content = "${api.visibility}\\n\\n${api.metadata['api-state']}";
      createdPageMetadata = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'metadata',
            type: PageType.MARKDOWN,
            parentId: '',
            content,
            published: true,
          },
        }),
      );

      expect(createdPageMetadata.parentPath).toEqual('');
      expect(createdPageMetadata.published).toBeTruthy();
      expect(createdPageMetadata.content).toEqual(content);
    });
    describe('Check documentation on portal', () => {
      test('Get createdPage OK', async () => {
        const pageResult = await succeed(
          apiPortalAsApplicationFrenchUser.getPageByApiIdAndPageIdRaw({
            apiId: createdApi.id,
            pageId: createdPageMetadata.id,
            include: [GetPageByApiIdAndPageIdIncludeEnum.Content],
          }),
        );
        expect(pageResult.name).toEqual(createdPageMetadata.name);
        expect(pageResult.content).toEqual(`PUBLIC\\n\\nSTOPPED`);
      });
    });
  });

  describe('Exclude group', () => {
    test('READER can get pages', async () => {
      const pages = await succeed(
        apiPortalAsSimpleUser.getPagesByApiIdRaw({
          apiId: createdApi.id,
          size: -1,
          homepage: false,
        }),
      );
      expect(pages.data.length).toBeGreaterThan(0);
      const pageIds = pages.data.map(({ id }) => id);
      expect(pageIds).toEqual(
        expect.arrayContaining([createdPage.id, createdFolder.id, pageInFolder.id, pageOutsideFolder.id, createdPageMetadata.id]),
      );
      expect(pageIds).not.toContain(homepage.id);
    });
    test('READER can get createdPage', async () => {
      const pageResult = await succeed(
        apiPortalAsSimpleUser.getPageByApiIdAndPageIdRaw({
          apiId: createdApi.id,
          pageId: createdPage.id,
          include: [GetPageByApiIdAndPageIdIncludeEnum.Content],
        }),
      );
      expect(pageResult.name).toEqual(createdPage.name);
      expect(pageResult.content).toEqual(createdPage.content);
    });
    test('Exclude READER group', async () => {
      const updatePageEntity = { ...createdPage, excluded_groups: [createdReaderGroup.id] };
      const page = await succeed(
        apiPagesManagementApiAsApiUser.updateApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          page: createdPage.id,
          updatePageEntity,
        }),
      );
      expect(page.excluded_groups).toEqual(expect.arrayContaining([createdReaderGroup.id]));
      expect(page.visibility).toEqual('PRIVATE');
      expect(page.excludedAccessControls).toBeTruthy();
      expect(page.accessControls).toBeDefined();
      expect(page.accessControls.length).toBeGreaterThan(0);
      const excludedGroups = page.accessControls
        .filter((accessControl) => accessControl.referenceType.toUpperCase() === 'GROUP')
        .map(({ referenceId }) => referenceId);
      expect(excludedGroups).toEqual(expect.arrayContaining([createdReaderGroup.id]));
    });
    test('READER cannot get pages', async () => {
      const pages = await succeed(
        apiPortalAsSimpleUser.getPagesByApiIdRaw({
          apiId: createdApi.id,
          size: -1,
          homepage: false,
        }),
      );
      expect(pages.data.length).toBeGreaterThan(0);
      const pageIds = pages.data.map(({ id }) => id);
      expect(pageIds).toEqual(expect.arrayContaining([createdFolder.id, pageInFolder.id, pageOutsideFolder.id, createdPageMetadata.id]));
      expect(pageIds).not.toContain(homepage.id);
    });
    test('READER cannot get createdPage', async () => {
      await unauthorized(
        apiPortalAsSimpleUser.getPageByApiIdAndPageIdRaw({
          apiId: createdApi.id,
          pageId: createdPage.id,
          include: [GetPageByApiIdAndPageIdIncludeEnum.Content],
        }),
      );
    });
  });

  describe('Translate', () => {
    test('Translate createdPage', async () => {
      const translate = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            type: PageType.TRANSLATION,
            parentId: createdPage.id,
            configuration: {
              inheritContent: 'false',
              lang: 'fr',
            },
            name: 'Documentation traduite',
            content: 'Documentation traduite',
          },
        }),
      );

      expect(translate.published).toBeTruthy();
      expect(translate.type).toEqual(PageType.TRANSLATION);
    });
    describe('Check documentation on portal', () => {
      test('Get pages contains pages with translation', async () => {
        const pages = await succeed(
          apiPortalAsApplicationFrenchUser.getPagesByApiIdRaw({
            apiId: createdApi.id,
            size: -1,
            homepage: false,
          }),
        );
        expect(pages.data.length).toBeGreaterThan(0);
        const pageIds = pages.data.map(({ id }) => id);
        expect(pageIds).toEqual(
          expect.arrayContaining([createdPage.id, createdFolder.id, pageInFolder.id, pageOutsideFolder.id, createdPageMetadata.id]),
        );
        expect(pageIds).not.toContain(homepage.id);

        const translatedPage = pages.data.find(({ id }) => id === createdPage.id);
        expect(translatedPage.name).toEqual('Documentation traduite');
      });

      test('Get createdPage translated', async () => {
        const translatedPage = await succeed(
          apiPortalAsApplicationFrenchUser.getPageByApiIdAndPageIdRaw({
            apiId: createdApi.id,
            pageId: createdPage.id,
            include: [GetPageByApiIdAndPageIdIncludeEnum.Content],
          }),
        );
        expect(translatedPage.name).toEqual('Documentation traduite');
        expect(translatedPage.content).toEqual('Documentation traduite');
      });
    });
  });

  describe('Aside links', () => {
    let asideFolder;
    test('Get pages contains aside createdFolder', async () => {
      const systemFolders = await succeed(
        apiPagesManagementApiAsApiUser.getApiPagesRaw({ orgId, envId, api: createdApi.id, type: PageType.SYSTEMFOLDER }),
      );
      asideFolder = systemFolders.find((systemFolder) => systemFolder.name === 'Aside' && systemFolder.type === PageType.SYSTEMFOLDER);
      expect(asideFolder).toBeDefined();
    });
    test('Add aside link to existing createdPage', async () => {
      createdLink = await created(
        apiPagesManagementApiAsApiUser.createApiPageRaw({
          orgId,
          envId,
          api: createdApi.id,
          newPageEntity: {
            name: 'Linked createdPage',
            type: PageType.LINK,
            parentId: asideFolder.id,
            configuration: {
              resourceType: 'page',
              inherit: 'false',
              isFolder: 'false',
            },
            content: createdPage.id,
          },
        }),
      );

      expect(createdLink.published).toBeTruthy();
      expect(createdLink.type).toEqual(PageType.LINK);
      expect(createdLink.parentPath).toEqual('/Aside');
    });
    describe('Check documentation on portal', () => {
      test('Get links from aside', async () => {
        const links = await succeed(apiPortalAsApplicationFrenchUser.getApiLinksRaw({ apiId: createdApi.id }));
        const { resourceRef } = links.slots.aside[0].links.find((link) => link.name === createdLink.name);
        expect(resourceRef).toEqual(createdPage.id);
      });
    });
  });
});
