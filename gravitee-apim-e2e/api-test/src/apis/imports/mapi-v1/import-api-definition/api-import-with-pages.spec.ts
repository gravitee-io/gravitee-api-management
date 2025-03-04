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
import { afterAll, afterEach, beforeAll, describe, expect, test } from '@jest/globals';
import * as swaggerv2 from '@api-test-resources/petstore_swaggerv2.json';
import * as openapiv3 from '@api-test-resources/petstore_openapiv3.json';
import { src as wsdlapi } from '@api-test-resources/wsdl.xml';
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@gravitee/utils/configuration';
import { ImportSwaggerDescriptorEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ImportSwaggerDescriptorEntity';
import { Format } from '@gravitee/management-webclient-sdk/src/lib/models/Format';
import { Type } from '@gravitee/management-webclient-sdk/src/lib/models/Type';
import { APIPagesApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIPagesApi';
import { fail, succeed } from '@lib/jest-utils';
import { ApiEntity, ApiEntityStateEnum } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { Visibility } from '@gravitee/management-webclient-sdk/src/lib/models/Visibility';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PageEntity } from '@gravitee/management-webclient-sdk/src/lib/models/PageEntity';
import { PagesFaker } from '@gravitee/fixtures/management/PagesFaker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const definitionVersion = '2.0.0';

const apisResourceAsAdminUser = new APIsApi(forManagementAsAdminUser());
const apisResourceApiUser = new APIsApi(forManagementAsApiUser());
const pagesResourcesApiUser = new APIPagesApi(forManagementAsApiUser());

describe('API - Imports with pages', () => {
  describe('Create API with one page without an ID', () => {
    const fakeApi = ApisFaker.apiImport({ pages: [PagesFaker.page()] });
    let api: ApiEntity;
    let pageId: string;
    let pages: PageEntity[];

    beforeAll(async () => {
      api = await apisResourceAsAdminUser.importApiDefinition({
        envId,
        orgId,
        body: fakeApi,
      });
    });

    test('should get API documentation pages from generated API ID', async () => {
      pages = await succeed(apisResourceAsAdminUser.getApiPagesRaw({ envId, orgId, api: api.id }));
      expect(pages).toHaveLength(2);
      expect(pages[1]).toHaveProperty('id');
      pageId = pages[1].id;
    });

    test('should get page from generated page ID', async () => {
      const page = await succeed(apisResourceAsAdminUser.getApiPageRaw({ orgId, envId, api: api.id, page: pageId }));
      expect(page).toBeTruthy();
      expect(page.order).toStrictEqual(1);
      expect(page.type).toStrictEqual('MARKDOWN');
      expect(page.name).toBeDefined();
      expect(page.content).toBeDefined();
      expect(page.published).toStrictEqual(false);
      expect(page.homepage).toStrictEqual(false);
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: api.id,
      });
    });
  });

  describe('Create API with one page with an ID', () => {
    const apiId = '08a92f8c-e133-42ec-a92f-8ce13382ec73';
    const expectedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';
    const pageId = '7b95cbe6-099d-4b06-95cb-e6099d7b0609';
    const expectedPageId = '0b827e1e-afe2-3863-8533-a723c486d4ef';
    const fakeApi = ApisFaker.apiImport({ id: apiId, pages: [PagesFaker.page({ id: pageId })] });
    let pages: PageEntity[];

    test('should create an API with one page of documentation and return specified ID', async () => {
      const api = await succeed(
        apisResourceAsAdminUser.importApiDefinitionRaw({
          envId,
          orgId,
          body: fakeApi,
        }),
      );
      expect(api).toBeTruthy();
      expect(api.id).toStrictEqual(expectedApiId);
    });

    test('should get API documentation pages from specified API ID', async () => {
      pages = await succeed(apisResourceAsAdminUser.getApiPagesRaw({ envId, orgId, api: expectedApiId }));
      expect(pages).toHaveLength(2);
      expect(pages[1].id).toStrictEqual(expectedPageId);
    });

    test('should get API page from generated page ID', async () => {
      const page = await succeed(apisResourceAsAdminUser.getApiPageRaw({ orgId, envId, api: expectedApiId, page: expectedPageId }));
      expect(page).toBeTruthy();
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: expectedApiId,
      });
    });
  });

  describe('Create API with a page tree', () => {
    const apiId = '7f1af04f-339d-42e3-8d9e-ce478511ef13';
    const rootFolderId = '29b97194-8786-48cb-8162-d3989ce5ad48';
    const folderId = '7ef6a60d-3c29-459d-b05b-3d74ade03fa6';
    const pageId = '915bc210-445b-4b7b-888b-c676e3fb8c7e';

    const generatedApiId = '3a6c5568-aa36-3955-ac6f-9834cf00ec8c';
    const generatedRootFolderId = 'e53f5b35-0798-3c2b-83b0-c080d06bbf03';
    const generatedFolderId = 'a7451cc1-bd10-3a06-be22-14d8e3d44145';
    const generatedPageId = '844a43b0-4e77-3a05-9880-137d9d64c224';

    const fakeRootFolder = PagesFaker.page({ id: rootFolderId, type: 'ROOT', content: null });
    const fakeFolder = PagesFaker.page({ id: folderId, type: 'FOLDER', parentId: rootFolderId, content: null });
    const fakePage = PagesFaker.page({ id: pageId, parentId: folderId });
    const fakeApi = ApisFaker.apiImport({ id: apiId, pages: [fakePage, fakeFolder, fakeRootFolder] });

    beforeAll(async () => {
      await apisResourceAsAdminUser.importApiDefinition({
        envId,
        orgId,
        body: fakeApi,
      });
    });

    test.each`
      parent                   | root     | pagesSize | expectedId
      ${undefined}             | ${true}  | ${2}      | ${generatedRootFolderId}
      ${generatedRootFolderId} | ${false} | ${1}      | ${generatedFolderId}
      ${generatedFolderId}     | ${false} | ${1}      | ${generatedPageId}
    `(
      'should get root pages when parent is $parent',
      async ({ parent, root, pagesSize, expectedId }: { parent?: string; root: boolean; pagesSize: number; expectedId: string }) => {
        const pages = await succeed(apisResourceAsAdminUser.getApiPagesRaw({ envId, orgId, api: generatedApiId, parent, root }));
        expect(pages).toHaveLength(pagesSize);
        expect(pages[pagesSize - 1].id).toStrictEqual(expectedId);
      },
    );

    test('should get API page from generated page ID', async () => {
      const page = await succeed(apisResourceAsAdminUser.getApiPageRaw({ envId, orgId, api: generatedApiId, page: generatedPageId }));
      expect(page).toBeTruthy();
    });

    afterAll(async () => {
      await apisResourceAsAdminUser.deleteApi({
        envId,
        orgId,
        api: generatedApiId,
      });
    });
  });

  describe('Swagger import', () => {
    const apiImportArray = [
      { name: 'Swagger v2 file', payload: JSON.stringify(swaggerv2), type: Type.INLINE, format: Format.API, expectedFlowsLength: 20 },
      { name: 'OpenAPI v3 file', payload: JSON.stringify(openapiv3), type: Type.INLINE, format: Format.API, expectedFlowsLength: 19 },
      { name: 'WSDL file', payload: wsdlapi, type: Type.INLINE, format: Format.WSDL, expectedFlowsLength: 42 },
      // Use only on local env, expose to flaky and webservices.oorsprong.org doesn't belong to gravitee !
      // { name:'OpenAPI url', payload: 'https://docs.gravitee.io/apim/3.x/management-api/3.17/swagger.json', type: Type.URL, format: Format.API },
      // { name:'WSDL url', payload: 'http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL', type: Type.URL, format: Format.WSDL },
    ];

    describe.each(apiImportArray)('API import via $name', ({ payload, type, format, expectedFlowsLength }) => {
      let apiId: string;
      const importSwaggerDescriptorEntity: ImportSwaggerDescriptorEntity = {
        with_policies: [],
        format,
        type,
        payload,
      };

      afterEach(async () => await apisResourceAsAdminUser.deleteApi({ orgId, envId, api: apiId }));

      test('should import API without creating a documentation', async () => {
        const api = await succeed(
          apisResourceApiUser.importSwaggerApiRaw({
            orgId,
            envId,
            definitionVersion,
            importSwaggerDescriptorEntity,
          }),
          201,
        );

        apiId = api.id;
        expect(api.id).not.toEqual('');
        expect(api.visibility).toEqual(Visibility.PRIVATE);
        expect(api.state).toEqual(ApiEntityStateEnum.STOPPED);

        const pages = await succeed(pagesResourcesApiUser.getApiPagesRaw({ orgId, envId, api: api.id }));
        expect(pages).toHaveLength(1);
      });

      test('should import API and create a swagger documentation', async () => {
        const api = await succeed(
          apisResourceApiUser.importSwaggerApiRaw({
            orgId,
            envId,
            definitionVersion,
            importSwaggerDescriptorEntity: { ...importSwaggerDescriptorEntity, with_documentation: true },
          }),
          201,
        );

        apiId = api.id;
        expect(api.id).not.toEqual('');
        expect(api.visibility).toEqual(Visibility.PRIVATE);
        expect(api.state).toEqual(ApiEntityStateEnum.STOPPED);

        const pages = await succeed(pagesResourcesApiUser.getApiPagesRaw({ orgId, envId, api: api.id }));

        expect(pages).toHaveLength(2);
        expect(pages[1].id).not.toEqual('');
        expect(pages[1].type).toEqual('SWAGGER');
        expect(pages[1].content).not.toEqual('');
      });

      test('should fail to import the same Swagger API again', async () => {
        const api = await succeed(
          apisResourceApiUser.importSwaggerApiRaw({
            orgId,
            envId,
            definitionVersion,
            importSwaggerDescriptorEntity,
          }),
          201,
        );

        apiId = api.id;

        await fail(
          apisResourceApiUser.importSwaggerApiRaw({
            orgId,
            envId,
            definitionVersion,
            importSwaggerDescriptorEntity,
          }),
          400,
          `API paths are invalid (Path [${api.context_path}/] already exists)`,
        );
      });

      test('should import API and create a path (to add policies) for every declared Swagger path', async () => {
        const api = await succeed(
          apisResourceApiUser.importSwaggerApiRaw({
            orgId,
            envId,
            definitionVersion,
            importSwaggerDescriptorEntity: {
              ...importSwaggerDescriptorEntity,
              with_policy_paths: true,
            },
          }),
          201,
        );

        apiId = api.id;
        expect(api.flows).toHaveLength(expectedFlowsLength);
      });
    });

    test('should fail when trying to import an empty file/URL', async () => {
      await fail(
        apisResourceApiUser.importSwaggerApi({
          orgId,
          envId,
          definitionVersion,
          importSwaggerDescriptorEntity: { payload: '' },
        }),
        400,
      );
    });
  });
});
