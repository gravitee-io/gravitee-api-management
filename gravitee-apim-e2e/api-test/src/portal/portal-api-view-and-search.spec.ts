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
import { afterAll, beforeAll, describe, test, expect } from '@jest/globals';

import { forManagementAsAdminUser, forPortalAsAdminUser } from '@client-conf/*';
import { APIsApi } from '@management-apis/APIsApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { UpdateApiEntity, UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { ApiApi } from '@portal-apis/ApiApi';
import { succeed } from '@lib/jest-utils';
import { ApiEntity } from '@management-models/ApiEntity';
import { FilterApiQuery } from '@portal-models/FilterApiQuery';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { CategoryEntity } from '@management-models/CategoryEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisManagementApiAsAdmin = new APIsApi(forManagementAsAdminUser());
const apiPortalApiAsAdmin = new ApiApi(forPortalAsAdminUser());
const configurationApiAsAdmin = new ConfigurationApi(forManagementAsAdminUser());

async function createAndPublish(apiManagementClient: APIsApi, attributes?: Partial<UpdateApiEntity>): Promise<ApiEntity> {
  let createdApi = await apiManagementClient.createApi({
    orgId,
    envId,
    newApiEntity: ApisFaker.newApi(),
  });
  await apiManagementClient.updateApi({
    orgId,
    envId,
    api: createdApi.id,
    updateApiEntity: UpdateApiEntityFromJSON({ ...createdApi, lifecycle_state: ApiLifecycleState.PUBLISHED, ...attributes }),
  });
  return createdApi;
}

describe('Portal - View and search APIs', () => {
  let apiWith1Label: ApiEntity;
  let apiWith2Labels: ApiEntity;
  let apiFeatured: ApiEntity;
  let apiWithCategory: ApiEntity;
  let createdCategory: CategoryEntity;

  beforeAll(async () => {
    // create all APIs needed for testing
    createdCategory = await succeed(
      configurationApiAsAdmin.createCategoryRaw({
        orgId,
        envId,
        newCategoryEntity: { name: 'cat1' },
      }),
    );
    apiWithCategory = await createAndPublish(apisManagementApiAsAdmin, { categories: ['cat1'], description: 'API with one category' });
    apiWith1Label = await createAndPublish(apisManagementApiAsAdmin, { labels: ['testlabel1'], description: 'API with one label' });
    apiWith2Labels = await createAndPublish(apisManagementApiAsAdmin, {
      labels: ['testlabel1', 'testlabel2'],
      description: 'API with two labels',
    });
    apiFeatured = await createAndPublish(apisManagementApiAsAdmin, { description: 'Featured API' });
    await succeed(configurationApiAsAdmin.createTopApiRaw({ orgId, envId, newTopApiEntity: { api: apiFeatured.id } }));
  });

  afterAll(async () => {
    // delete all APIs that were created for testing
    await apisManagementApiAsAdmin.deleteApi({ orgId, envId, api: apiWith1Label.id });
    await apisManagementApiAsAdmin.deleteApi({ orgId, envId, api: apiWith2Labels.id });
    await apisManagementApiAsAdmin.deleteApi({ orgId, envId, api: apiFeatured.id });
    await apisManagementApiAsAdmin.deleteApi({ orgId, envId, api: apiWithCategory.id });
    await configurationApiAsAdmin.deleteCategory({ orgId, envId, categoryId: createdCategory.id });
  });

  describe('View APIs in the APIM portal', function () {
    describe('Filter API list regarding FilterApiQuery', () => {
      test('should list all published APIs', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ filter: FilterApiQuery.ALL }));
        expect(getApisResponse.data).toHaveLength(4);
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWith1Label.id)).toBeTruthy();
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWith2Labels.id)).toBeTruthy();
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiFeatured.id)).toBeTruthy();
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWithCategory.id)).toBeTruthy();
      });

      test('should only list featured APIs', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ filter: FilterApiQuery.FEATURED }));
        expect(getApisResponse.data).toHaveLength(1);
        expect(getApisResponse.data[0].id).toBe(apiFeatured.id);
      });
    });

    describe('Exclude APIs from list', function () {
      test('should exclude APIs with certain query param', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ filter2: FilterApiQuery.FEATURED }));
        expect(getApisResponse.data).toHaveLength(3);
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWith1Label.id)).toBeTruthy();
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWith2Labels.id)).toBeTruthy();
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWithCategory.id)).toBeTruthy();
      });
    });

    describe('Filter API list regarding category', function () {
      test('should list all APIs with certain category', async () => {
        let getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ category: 'cat1' }));
        expect(getApisResponse.data).toHaveLength(1);
        expect(getApisResponse.data[0].id).toBe(apiWithCategory.id);
      });
    });

    describe('Filter API list regarding labels', () => {
      test('should find APIs by using one of its labels', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ label: 'testlabel1' }));
        expect(getApisResponse.data).toHaveLength(2);
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWith1Label.id)).toBeTruthy();
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWith2Labels.id)).toBeTruthy();
      });

      test('should find APIs by using another one of its labels', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ label: 'testlabel2' }));
        expect(getApisResponse.data).toHaveLength(1);
        expect(getApisResponse.data.some((filteredApi) => filteredApi.id === apiWith2Labels.id));
      });

      test('should not find any APIs if unknown label is used', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ label: 'unknownlabel' }));
        expect(getApisResponse.data).toHaveLength(0);
      });
    });

    describe('Limit API list using paging', () => {
      test('should limit API to certain value', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ size: 2 }));
        expect(getApisResponse.data).toHaveLength(2);
      });

      test('should show certain page when limiting API to certain value', async () => {
        const getApisResponse = await succeed(apiPortalApiAsAdmin.getApisRaw({ size: 3, page: 2 }));
        expect(getApisResponse.data).toHaveLength(1);
      });
    });
  });

  describe('Search APIs in the APIM portal', function () {
    test('should find all APIs with certain string in its definition', async () => {
      const searchResponse = await succeed(apiPortalApiAsAdmin.searchApisRaw({ q: 'Featured' }));
      expect(searchResponse.data).toHaveLength(1);
      expect(searchResponse.data.some((foundApi) => foundApi.description === 'Featured API')).toBeTruthy();
    });

    test('should find API when using API-ID as search string', async () => {
      const searchResponse = await succeed(apiPortalApiAsAdmin.searchApisRaw({ q: apiWith1Label.id }));
      expect(searchResponse.data).toHaveLength(1);
      expect(searchResponse.data.some((foundApi) => foundApi.description === 'API with one label')).toBeTruthy();
    });

    test('should not find any APIs if search string does not match anything', async () => {
      const searchResponse = await succeed(apiPortalApiAsAdmin.searchApisRaw({ q: 'unmatchedString' }));
      expect(searchResponse.data).toHaveLength(0);
    });
  });
});
