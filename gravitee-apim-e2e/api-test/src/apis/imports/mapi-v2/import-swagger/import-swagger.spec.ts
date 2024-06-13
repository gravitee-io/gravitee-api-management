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
import { forManagementAsAdminUser, forManagementV2AsAdminUser } from '@gravitee/utils/configuration';
import { APIDocumentationApi, APIsApi as APIsApiV2, ApiV4 } from '../../../../../../lib/management-v2-webclient-sdk/src/lib';
import { CategoryEntity, GroupEntity, TagEntity } from '../../../../../../lib/management-webclient-sdk/src/lib/models';
import * as openapiv3 from '@api-test-resources/openapi-withExtensions.json';
import { afterAll, afterEach, beforeAll, describe, expect, test } from '@jest/globals';
import { ShardingTagsApi } from '../../../../../../lib/management-webclient-sdk/src/lib/apis/ShardingTagsApi';
import { created, succeed } from '@lib/jest-utils';
import { GroupsApi } from '../../../../../../lib/management-webclient-sdk/src/lib/apis/GroupsApi';
import { CategoriesApi } from '../../../../../../lib/management-webclient-sdk/src/lib/apis/CategoriesApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const v2ApisResourceAsAdmin = new APIsApiV2(forManagementV2AsAdminUser());
const shardingTagsAsAdmin = new ShardingTagsApi(forManagementAsAdminUser());
const groupsAsAdmin = new GroupsApi(forManagementAsAdminUser());
const categoryAsAdmin = new CategoriesApi(forManagementAsAdminUser());
const documentationAsAdmin = new APIDocumentationApi(forManagementV2AsAdminUser());

describe('API - Imports OpenAPI specification', () => {
  let specification = JSON.stringify(openapiv3);
  let createdTag: TagEntity;
  let createdGroup: GroupEntity;
  let createdCategory: CategoryEntity;
  let importedApi: ApiV4;
  let apiId: string;

  beforeAll(async () => {
    createdTag = await succeed(shardingTagsAsAdmin.createTagRaw({ orgId, newTagEntity: { name: 'tag1' } }));
    createdGroup = await created(groupsAsAdmin.createGroupRaw({ orgId, envId, newGroupEntity: { name: 'group1' } }));
    createdCategory = await succeed(categoryAsAdmin.createCategoryRaw({ orgId, envId, newCategoryEntity: { name: 'cat1' } }));
  });

  afterAll(async () => {
    await shardingTagsAsAdmin.deleteTagRaw({ orgId, tag: createdTag.id });
    await groupsAsAdmin.deleteGroupRaw({ orgId, envId, group: createdGroup.id });
    await categoryAsAdmin.deleteCategoryRaw({ orgId, envId, categoryId: createdCategory.id });
  });

  afterEach(async () => {
    await v2ApisResourceAsAdmin.deleteApi({ envId, apiId: apiId });
  });

  test('should import OpenAPI specification', async () => {
    const createdApi = await created(
      v2ApisResourceAsAdmin.createApiFromSwaggerRaw({ envId, importSwaggerDescriptor: { payload: specification } }),
    );

    importedApi = await v2ApisResourceAsAdmin.getApi({ envId, apiId: createdApi.id });

    apiId = createdApi.id;

    expect(importedApi).toBeTruthy();
    expect(importedApi.id).toBeTruthy();
    expect(importedApi.name).toBe('Gravitee.io Swagger API');
    expect(importedApi.apiVersion).toBe('1.2.3');
    expect(importedApi.description).toBe('Description of Gravitee.io Swagger API');
    expect(importedApi.visibility).toBe('PRIVATE');
    expect(importedApi.groups).toHaveLength(1);
    expect(importedApi.categories).toStrictEqual(['cat1']);
    expect(importedApi.tags).toStrictEqual(['tag1']);
    expect(importedApi.lifecycleState).toBe('CREATED');
    expect(importedApi.state).toBe('STOPPED');
    expect(importedApi.listeners).toStrictEqual([
      {
        type: 'HTTP',
        servers: undefined,
        cors: undefined,
        pathMappings: undefined,
        entrypoints: [
          {
            type: 'http-proxy',
            qos: 'AUTO',
            configuration: undefined,
            dlq: undefined,
          },
        ],
        paths: [
          {
            host: 'myHost',
            path: '/myPath/',
            overrideAccess: false,
          },
        ],
      },
    ]);
    expect(importedApi.flows).toEqual([
      expect.objectContaining({
        selectors: [
          {
            type: 'CONDITION',
            condition: '',
          },
          {
            type: 'HTTP',
            path: '/pets',
            pathOperator: 'EQUALS',
            methods: ['GET'],
          },
        ],
      }),
      expect.objectContaining({
        selectors: [
          {
            type: 'CONDITION',
            condition: '',
          },
          {
            type: 'HTTP',
            path: '/pets',
            pathOperator: 'EQUALS',
            methods: ['POST'],
          },
        ],
      }),
      expect.objectContaining({
        selectors: [
          {
            type: 'CONDITION',
            condition: '',
          },
          {
            type: 'HTTP',
            path: '/pets/:petId',
            pathOperator: 'EQUALS',
            methods: ['GET'],
          },
        ],
      }),
      expect.objectContaining({
        selectors: [
          {
            type: 'CONDITION',
            condition: '',
          },
          {
            type: 'HTTP',
            path: '/pets/:petId',
            pathOperator: 'EQUALS',
            methods: ['DELETE'],
          },
        ],
      }),
    ]);
    expect(importedApi.labels).toStrictEqual(['label1', 'label2']);
    expect(importedApi.endpointGroups).toEqual([
      {
        name: 'default-group',
        type: 'http-proxy',
        loadBalancer: { type: 'ROUND_ROBIN' },
        endpoints: [
          expect.objectContaining({
            name: 'default',
            type: 'http-proxy',
            weight: 1,
            inheritConfiguration: true,
            configuration: {
              target: 'https://demo.gravitee.io/gateway/echo',
            },
            secondary: false,
            services: { healthCheck: undefined },
            sharedConfigurationOverride: undefined,
            tenants: undefined,
          }),
        ],
        services: { discovery: undefined, healthCheck: undefined },
        sharedConfiguration: expect.any(Object),
      },
    ]);

    expect(importedApi.properties).toStrictEqual([
      {
        key: 'prop1',
        value: 'propValue1',
        encrypted: true,
        encryptable: undefined,
        dynamic: false,
      },
      {
        key: 'prop2',
        value: 'propValue2',
        encrypted: false,
        encryptable: undefined,
        dynamic: false,
      },
    ]);
  });

  describe('import documentation', () => {
    test('should create documentation page based on OpenAPI specification', async () => {
      const createdApi = await created(
        v2ApisResourceAsAdmin.createApiFromSwaggerRaw({
          envId,
          importSwaggerDescriptor: { payload: specification, withDocumentation: true },
        }),
      );

      apiId = createdApi.id;

      const pages = await succeed(documentationAsAdmin.getApiPagesRaw({ envId, apiId: createdApi.id }));

      expect(pages.pages.length).toBe(1);
    });

    test('should not create documentation page based on OpenAPI specification', async () => {
      const createdApi = await created(
        v2ApisResourceAsAdmin.createApiFromSwaggerRaw({
          envId,
          importSwaggerDescriptor: { payload: specification, withDocumentation: false },
        }),
      );

      apiId = createdApi.id;

      const pages = await succeed(documentationAsAdmin.getApiPagesRaw({ envId, apiId: createdApi.id }));

      expect(pages.pages.length).toBe(0);
    });
  });
});
