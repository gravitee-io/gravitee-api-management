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
import { test, describe, expect, afterAll } from '@jest/globals';
import { APIsApi, ApiV4 } from '../../../../../../lib/management-v2-webclient-sdk/src/lib';
import { forManagementAsApiUser, forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2PagesFaker } from '@gravitee/fixtures/management/MAPIV2PagesFaker';
import { APIPagesApi } from '../../../../../../lib/management-webclient-sdk/src/lib/apis/APIPagesApi';
import { PageEntity } from '../../../../../../lib/management-webclient-sdk/src/lib/models';
import faker from '@faker-js/faker';
import { MAPIV2MediaFaker } from '@gravitee/fixtures/management/MAPIV2MediaFaker';
import { ImagesUtils } from '@gravitee/utils/images';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v1ApiPagesResourceAsApiPublisher = new APIPagesApi(forManagementAsApiUser());

describe('API - V4 - Import - Gravitee Definition - With pages', () => {
  describe('Create v4 API from import with pages', () => {
    describe('Create v4 API with one page without ID', () => {
      let importedApi: ApiV4;
      let importedPage: PageEntity;
      const page = MAPIV2PagesFaker.page();

      test('should import v4 API with one page without id', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4({
              pages: [page],
            }),
          }),
        );
        expect(importedApi).toBeTruthy();
      });

      test('should get created v4 API with generated ID', async () => {
        const apiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
      });

      test('should get list of pages with correct data', async () => {
        const pagesResponse = await succeed(
          v1ApiPagesResourceAsApiPublisher.getApiPagesRaw({
            orgId,
            envId,
            api: importedApi.id,
          }),
        );
        expect(pagesResponse).toHaveLength(2);
        // By default, one page is here a system folder
        expect(pagesResponse.filter((p) => p.type === 'SYSTEM_FOLDER')).toHaveLength(1);

        importedPage = pagesResponse.find((p) => p.name === page.name);
        expect(importedPage.id).toBeDefined();
      });

      afterAll(async () => {
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: importedPage.id,
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
    describe('Create v4 API with one page with an ID', () => {
      const fixedApiId = faker.datatype.uuid();
      let importedApi: ApiV4;
      let importedPage: PageEntity;
      const page = MAPIV2PagesFaker.page({
        id: faker.datatype.uuid(),
      });

      test('should import v4 API with one page without id', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4({
              api: MAPIV2ApisFaker.apiV4({ id: fixedApiId }),
              pages: [page],
            }),
          }),
        );
        expect(importedApi).toBeTruthy();
        expect(importedApi.id).toStrictEqual(fixedApiId);
      });

      test('should get created v4 API with generated ID', async () => {
        const apiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
      });

      test('should get list of pages with correct data', async () => {
        const pagesResponse = await succeed(
          v1ApiPagesResourceAsApiPublisher.getApiPagesRaw({
            orgId,
            envId,
            api: importedApi.id,
          }),
        );
        expect(pagesResponse).toHaveLength(2);
        // By default, one page is here a system folder
        expect(pagesResponse.filter((p) => p.type === 'SYSTEM_FOLDER')).toHaveLength(1);

        importedPage = pagesResponse.find((p) => p.name === page.name);
        expect(importedPage.id).toStrictEqual(page.id);
      });

      afterAll(async () => {
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: importedPage.id,
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
    describe('Create v4 API with one page with a media', () => {
      const fixedApiId = faker.datatype.uuid();
      let importedApi: ApiV4;
      let importedPage: PageEntity;
      const page = MAPIV2PagesFaker.page({
        id: faker.datatype.uuid(),
        name: 'A page with a media',
        type: 'MARKDOWN',
        content:
          '![my-media.jpeg](/management/organizations/DEFAULT/environments/DEFAULT/apis/origin-api-id/media/DB0A773F02AF003348F1B09734717266)',
        attachedMedia: [
          {
            hash: 'F032C51E02846C33057139856BE3CF8D',
            name: 'an-attached-media.jpeg',
            attachedAt: new Date(),
          },
        ],
      });

      const inPageMedia = MAPIV2MediaFaker.media({
        hash: 'DB0A773F02AF003348F1B09734717266',
        fileName: 'my-media.jpeg',
        data: ImagesUtils.fakeImage15x15,
      });
      const attachedMedia = MAPIV2MediaFaker.media({
        hash: 'F032C51E02846C33057139856BE3CF8D',
        fileName: 'an-attached-media.jpeg',
        data: ImagesUtils.fakeImage150x35,
      });

      test('should import v4 API with one page without id', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4({
              api: MAPIV2ApisFaker.apiV4({ id: fixedApiId }),
              pages: [page],
              apiMedia: [inPageMedia, attachedMedia],
            }),
          }),
        );
        expect(importedApi).toBeTruthy();
        expect(importedApi.id).toStrictEqual(fixedApiId);
      });

      test('should get created v4 API with generated ID', async () => {
        const apiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
      });

      test('should get list of pages with correct data', async () => {
        const pagesResponse = await succeed(
          v1ApiPagesResourceAsApiPublisher.getApiPagesRaw({
            orgId,
            envId,
            api: importedApi.id,
          }),
        );
        expect(pagesResponse).toHaveLength(2);
        // By default, one page is here a system folder
        expect(pagesResponse.filter((p) => p.type === 'SYSTEM_FOLDER')).toHaveLength(1);

        importedPage = pagesResponse.find((p) => p.name === page.name);
        expect(importedPage.id).toStrictEqual(page.id);
        expect(importedPage.content).toEqual(page.content);
        expect(importedPage.attached_media).toHaveLength(1);
        expect(importedPage.attached_media[0].mediaHash).toEqual(attachedMedia.hash);
      });

      afterAll(async () => {
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: importedPage.id,
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
    describe('Create v4 API with one pages tree', () => {
      let importedApi: ApiV4;
      let rootFolderRecalculatedId: string;
      let folderRecalculatedId: string;
      let firstChildRecalculatedId: string;
      let secondChildRecalculatedId: string;
      /**
       * Creating tree:
       * rootFolder
       * |_folder
       *   |_firstChildPage
       *   |_secondChildPage
       */
      const rootFolder = MAPIV2PagesFaker.page({
        id: faker.datatype.uuid(),
        type: 'ROOT',
      });
      const folder = MAPIV2PagesFaker.page({
        id: faker.datatype.uuid(),
        parentId: rootFolder.id,
        type: 'FOLDER',
        content: undefined,
      });
      const firstChildPage = MAPIV2PagesFaker.page({
        id: faker.datatype.uuid(),
        parentId: folder.id,
      });
      const secondChildPage = MAPIV2PagesFaker.page({
        id: faker.datatype.uuid(),
        parentId: folder.id,
      });

      test('should import v4 API with pages tree without id', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4({
              pages: [rootFolder, folder, firstChildPage, secondChildPage],
            }),
          }),
        );
        expect(importedApi).toBeTruthy();
        expect(importedApi.id).toBeDefined();
      });

      test('should get created v4 API with generated ID', async () => {
        const apiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
      });

      test('should get list of pages with correct data', async () => {
        const pagesResponse = await succeed(
          v1ApiPagesResourceAsApiPublisher.getApiPagesRaw({
            orgId,
            envId,
            api: importedApi.id,
          }),
        );
        expect(pagesResponse).toHaveLength(5);
        // By default, one page is here a system folder
        expect(pagesResponse.filter((p) => p.type === 'SYSTEM_FOLDER')).toHaveLength(1);

        rootFolderRecalculatedId = pagesResponse.find((p) => p.name === rootFolder.name).id;
        folderRecalculatedId = pagesResponse.find((p) => p.name === folder.name).id;
        firstChildRecalculatedId = pagesResponse.find((p) => p.name === firstChildPage.name).id;
        secondChildRecalculatedId = pagesResponse.find((p) => p.name === secondChildPage.name).id;
      });

      // HACK:
      // with test.each, jest is using the initial value of variables
      // in this case, they are not yet initialized (it's done in the previous step)
      // using functions allow to delay the access to variables
      const getRootFolderIdFn = () => rootFolderRecalculatedId;
      const getFolderIdFn = () => folderRecalculatedId;
      const getFirstChildIdFn = () => firstChildRecalculatedId;
      const getSecondChildIdFn = () => secondChildRecalculatedId;

      test.each`
        parentIdFn           | root     | pagesCount | expectedIdFn          | name
        ${undefined}         | ${true}  | ${2}       | ${getRootFolderIdFn}  | ${'rootFolder'}
        ${getRootFolderIdFn} | ${false} | ${1}       | ${getFolderIdFn}      | ${'folder'}
        ${getFolderIdFn}     | ${false} | ${2}       | ${getFirstChildIdFn}  | ${'firstChildPage'}
        ${getFolderIdFn}     | ${false} | ${2}       | ${getSecondChildIdFn} | ${'secondChildPage'}
      `('should get list of pages with correct data for $name', async ({ parentIdFn, root, pagesCount, expectedIdFn }) => {
        const pagesResponse = await succeed(
          v1ApiPagesResourceAsApiPublisher.getApiPagesRaw({
            orgId,
            envId,
            api: importedApi.id,
            parent: parentIdFn == undefined ? undefined : parentIdFn(),
            root,
          }),
        );
        const expectedId = expectedIdFn === undefined ? undefined : expectedIdFn();
        expect(pagesResponse).toHaveLength(pagesCount);
        if (root) {
          // By default, one page is here a system folder
          expect(pagesResponse.filter((p) => p.type === 'SYSTEM_FOLDER')).toHaveLength(1);
          const rootResult = pagesResponse.find((p) => p.type === 'ROOT');
          expect(rootResult.name).toStrictEqual(rootFolder.name);
          expect(rootResult.id).toStrictEqual(expectedId);
        } else if (expectedId === folderRecalculatedId) {
          expect(pagesResponse).toHaveLength(1);
          expect(pagesResponse[0].parentId).toStrictEqual(parentIdFn());
          expect(pagesResponse[0].type).toEqual('FOLDER');
          expect(pagesResponse[0].id).toStrictEqual(expectedId);
        } else {
          expect(pagesResponse).toHaveLength(2);
          const firstChildResponsePage = pagesResponse.find((p) => p.id === firstChildRecalculatedId);
          const secondChildResponsePage = pagesResponse.find((p) => p.id === secondChildRecalculatedId);
          expect(firstChildResponsePage.parentId).toStrictEqual(parentIdFn());
          expect(firstChildResponsePage.type).toEqual('MARKDOWN');
          expect(secondChildResponsePage.parentId).toStrictEqual(parentIdFn());
          expect(secondChildResponsePage.type).toEqual('MARKDOWN');
        }
      });

      afterAll(async () => {
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: firstChildRecalculatedId,
          }),
        );
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: secondChildRecalculatedId,
          }),
        );
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: folderRecalculatedId,
          }),
        );
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: rootFolderRecalculatedId,
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
});
