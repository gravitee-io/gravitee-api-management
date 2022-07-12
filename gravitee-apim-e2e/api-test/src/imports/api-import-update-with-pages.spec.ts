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
import { succeed } from '@lib/jest-utils';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PagesFaker } from '@management-fakers/PagesFaker';

const apisResource = new APIsApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Update API from import with pages', () => {
  describe('Update API with existing page matching generated ID', () => {
    const fakeApi = ApisFaker.apiImport({
      id: '08a92f8c-e133-42ec-a92f-8ce13382ec73',
      pages: [PagesFaker.page({ id: '7b95cbe6-099d-4b06-95cb-e6099d7b0609' })],
    });
    const expectedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';
    const expectedPageId = '0b827e1e-afe2-3863-8533-a723c486d4ef';

    let apiUpdate;

    test('should create an API with one page of documentation and return a generated API ID', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update API page from generated ID', async () => {
      const pageUpdate = PagesFaker.page({
        id: apiUpdate.pages[1].id,
        name: 'Documentation (updated)',
        content: '# Documentation\n## Contributing\nTo be done.',
      });
      apiUpdate.pages = [pageUpdate];

      let updatedApi = await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
      expect(updatedApi.id).toBe(expectedApiId);
    });

    test('should get updated API page from generated page ID', async () => {
      let createdPage = await succeed(apisResource.getApiPageRaw({ envId, orgId, api: expectedApiId, page: expectedPageId }));
      expect(createdPage.name).toBe('Documentation (updated)');
      expect(createdPage.content).toBe('# Documentation\n## Contributing\nTo be done.');
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API with existing page ID, using previous export', () => {
    const fakeApi = ApisFaker.apiImport({
      id: '7061532e-c0e5-4894-818d-f747ad1601dc',
      pages: [PagesFaker.page({ id: '4be08c28-5638-4fec-a90a-51c0cd403b12' })],
    });
    const expectedApiId = '26bddf2a-b3df-345f-9298-76d389dc8920';
    const expectedPageId = 'da768516-8c43-3077-93c2-bd75514c7575';

    let apiUpdate;

    test('should create an API with one page of documentation', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should get API page from generated id', async () => {
      await succeed(apisResource.getApiPageRaw({ envId, orgId, api: expectedApiId, page: expectedPageId }));
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update the API, using previous export', async () => {
      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
    });

    test('should not have created additional API pages', async () => {
      let pages = await succeed(apisResource.getApiPagesRaw({ envId, orgId, api: expectedApiId }));
      expect(pages).toHaveLength(2);
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API with page without ID', () => {
    const apiId = 'f5cc6ea7-1ea1-46dd-a48f-34a0386467b4';
    const expectedApiId = '90bc8558-697f-37b4-b71b-9c56da60bd99';

    const fakeApi = ApisFaker.apiImport({ id: apiId });
    const pageName = 'documentation';

    let apiUpdate;

    test('should create an API with no documentation page', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update the API, adding one documentation page with a name and without an ID', async () => {
      const fakePage = PagesFaker.page({ name: pageName });
      expect(fakePage.id).toBeUndefined();
      apiUpdate.pages = [PagesFaker.page(fakePage)];
      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
    });

    test('should have created the page', async () => {
      let pages = await succeed(apisResource.getApiPagesRaw({ envId, orgId, api: expectedApiId }));
      expect(pages).toHaveLength(2);
      expect(pages[1].name).toBe(pageName);
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API, removing pages', () => {
    const fakeApi = ApisFaker.apiImport({
      id: '8fc829e8-b713-469f-8db5-06c702b82eb1',
      pages: [PagesFaker.page(), PagesFaker.page()],
    });
    const expectedApiId = 'acd83068-c27c-382b-a704-e9d64fc61d9c';

    let apiUpdate;

    test('should create an API with two pages of documentation', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update the API, omitting some pages', async () => {
      apiUpdate.pages = [];
      await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
    });

    test('should not have deleted pages', async () => {
      let pages = await succeed(apisResource.getApiPagesRaw({ envId, orgId, api: expectedApiId }));
      expect(pages).toHaveLength(3);
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API, duplicating system folder', () => {
    const fakeApi = ApisFaker.apiImport({
      id: 'dfb569b9-a8e1-4ad4-9b84-0dd638ac2f30',
      pages: [PagesFaker.page()],
    });
    const expectedApiId = '0223d962-7162-3c45-b014-cd74520da77d';

    let apiUpdate;

    test('should create an API with one pages of documentation', async () => {
      await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should have created a system folder page', async () => {
      expect(apiUpdate.pages).toHaveLength(2);
      expect(apiUpdate.pages.some(({ type }) => type === 'SYSTEM_FOLDER')).toBeTruthy();
    });

    test('should not have duplicated the system folder', async () => {
      let pages = await succeed(apisResource.getApiPagesRaw({ envId, orgId, api: expectedApiId }));
      expect(pages).toHaveLength(2);
      const systemFolders = pages.filter(({ type }) => type === 'SYSTEM_FOLDER');
      expect(systemFolders).toHaveLength(1);
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API page in a page tree', () => {
    const fakeApi = ApisFaker.apiImport({
      id: '7f1af04f-339d-42e3-8d9e-ce478511ef13',
      pages: [
        PagesFaker.page({
          id: '29b97194-8786-48cb-8162-d3989ce5ad48',
          type: 'FOLDER',
          content: null,
        }),
        PagesFaker.page({
          id: '7ef6a60d-3c29-459d-b05b-3d74ade03fa6',
          type: 'FOLDER',
          parentId: '29b97194-8786-48cb-8162-d3989ce5ad48',
          content: null,
        }),
        PagesFaker.page({
          id: '915bc210-445b-4b7b-888b-c676e3fb8c7e',
          parentId: '7ef6a60d-3c29-459d-b05b-3d74ade03fa6',
        }),
      ],
    });
    const expectedApiId = '3a6c5568-aa36-3955-ac6f-9834cf00ec8c';
    const expectedFolderId = 'a7451cc1-bd10-3a06-be22-14d8e3d44145';
    const expectedPageId = '844a43b0-4e77-3a05-9880-137d9d64c224';

    let apiUpdate;

    test('should create an API with a page tree and return a generated ID', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      expect(createdApi.id).toBe(expectedApiId);
    });

    test('should export the API', async () => {
      apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
    });

    test('should update API page in page tree', async () => {
      const pageToUpdate = apiUpdate.pages.find((page) => page.id === expectedPageId);
      pageToUpdate.name = 'updated-page';
      pageToUpdate.content = '# Documentation (updated)';
      let updatedApi = await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
      expect(updatedApi.id).toBe(expectedApiId);
    });

    test('should get updated page in folder', async () => {
      let pages = await succeed(apisResource.getApiPagesRaw({ envId, orgId, api: expectedApiId, parent: expectedFolderId }));
      expect(pages).toHaveLength(1);
      expect(pages[0].name).toBe('updated-page');
      expect(pages[0].content).toBe('# Documentation (updated)');
    });

    afterAll(async () => {
      await apisResource.deleteApiPage({ envId, orgId, api: expectedApiId, page: expectedPageId });
      await apisResource.deleteApiPage({ envId, orgId, api: expectedApiId, page: expectedFolderId });
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });
});
