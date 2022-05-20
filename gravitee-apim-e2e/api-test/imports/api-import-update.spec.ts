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
import { created, fail, succeed } from '../../lib/jest-utils';
import { Visibility } from '@management-models/Visibility';
import { ApiMetadataFormat, ApisFaker } from '@management-fakers/ApisFaker';
import { PlanStatus } from '@management-models/PlanStatus';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanType } from '@management-models/PlanType';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PagesFaker } from '@management-fakers/PagesFaker';
import { UsersApi } from '@management-apis/UsersApi';
import { UsersFaker } from '@management-fakers/UsersFaker';
import { RolesApi } from '@management-apis/RolesApi';
import { RoleFaker } from '@management-fakers/RoleFaker';
import { RoleScope } from '@management-models/RoleScope';
import { GroupsApi } from '@management-apis/GroupsApi';
import { GroupsFaker } from '@management-fakers/GroupsFaker';

const apisResource = new APIsApi(forManagementAsAdminUser());
const usersResource = new UsersApi(forManagementAsAdminUser());
const rolesResource = new RolesApi(forManagementAsAdminUser());
const groupsResource = new GroupsApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Update API by importing it', () => {
  describe('Update API which ID in URL does not exist', () => {
    const api = ApisFaker.apiImport({ id: 'unknown-test-id' });

    test('should fail to update API, returning 404', async () => {
      await fail(
        apisResource.updateWithDefinitionPUT({
          envId,
          orgId,
          api: 'unknown-test-id',
          body: api,
        }),
        404,
        'Api [unknown-test-id] can not be found.',
      );
    });
  });

  describe('Update API with an ID in URL that exists, without ID in body', () => {
    const apiId = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
    const expectedApiId = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

    const fakeCreateApi = ApisFaker.apiImport({ id: apiId });

    // update API data. body doesn't contains API id
    const fakeUpdateApi = ApisFaker.apiImport({
      name: 'updatedName',
      version: '1.1',
      description: 'Updated API description',
      visibility: Visibility.PUBLIC,
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/updated/path',
          },
        ],
      }),
    });

    test('should create API generating a predictable id', async () => {
      await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi }));
    });

    test('should update the API with the generated ID, even if no ID in body', async () => {
      let updatedApi = await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: fakeUpdateApi }));
      expect(updatedApi.id).toBe(expectedApiId);
    });

    test('should get updated API with updated data', async () => {
      let foundApi = await apisResource.getApi({ envId, orgId, api: expectedApiId });
      expect(foundApi.id).toBe(expectedApiId);
      expect(foundApi.name).toBe('updatedName');
      expect(foundApi.version).toBe('1.1');
      expect(foundApi.description).toBe('Updated API description');
      expect(foundApi.visibility).toBe(Visibility.PUBLIC);
      expect(foundApi.proxy.virtual_hosts[0].path).toBe('/updated/path');
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
    });
  });

  describe('Update API with an ID in URL that exists, with another API ID in body', () => {
    const apiId1 = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
    const expectedApiId1 = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

    const fakeCreateApi1 = ApisFaker.apiImport({ id: apiId1, name: 'originalName' });

    const apiId2 = '662712f4-8364-4e6b-825f-2008d59cc684';
    const expectedApiId2 = 'cb6cf46a-f396-3f80-9681-da23d9f2965c';

    const fakeCreateApi2 = ApisFaker.apiImport({ id: apiId2, name: 'originalName' });

    // that will update api2, with api1 id in body
    const fakeUpdateApi = ApisFaker.apiImport({ id: apiId1, name: 'updatedName' });

    test('should create API 1', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi1 }));
      expect(createdApi.id).toBe(expectedApiId1);
    });

    test('should create API 2', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi2 }));
      expect(createdApi.id).toBe(expectedApiId2);
    });

    test('should update API 2, event if api1 id in body', async () => {
      let updatedApi = await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId2, body: fakeUpdateApi }));
      expect(updatedApi.id).toBe(expectedApiId2);
    });

    test('should get API1 with unchanged data', async () => {
      let foundApi = await apisResource.getApi({ envId, orgId, api: expectedApiId1 });
      expect(foundApi.name).toBe('originalName');
    });

    test('should get API2 with updated data', async () => {
      let foundApi = await apisResource.getApi({ envId, orgId, api: expectedApiId2 });
      expect(foundApi.name).toBe('updatedName');
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId1 });
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId2 });
    });
  });

  describe('Update API with an updated context path matching another API context path', () => {
    const fakeCreateApi1 = ApisFaker.apiImport({
      id: '67d8020e-b0b3-47d8-9802-0eb0b357d84c',
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/importTest1',
          },
        ],
      }),
    });
    const expectedApiId1 = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

    const fakeCreateApi2 = ApisFaker.apiImport({
      id: '72dd3b21-b0cc-44a8-87d3-23e1f52b61fa',
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/importTest2',
          },
        ],
      }),
    });
    const expectedApiId2 = 'e6c25ef8-0946-3ea8-826b-4384aeda865c';

    // that will try to update api2, with the same context path as api1
    const fakeUpdateApi = ApisFaker.apiImport({
      proxy: ApisFaker.proxy({
        virtual_hosts: [
          {
            path: '/importTest1',
          },
        ],
      }),
    });

    test('should create a first API, generating a predictable ID', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi1 }));
      expect(createdApi.id).toBe(expectedApiId1);
    });

    test('should create a second API, generating another predictable ID', async () => {
      let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeCreateApi2 }));
      expect(createdApi.id).toBe(expectedApiId2);
    });

    test('should fail to update API 2 with same context path as API 1', async () => {
      await fail(
        apisResource.updateWithDefinitionPUT({ envId, orgId, api: expectedApiId2, body: fakeUpdateApi }),
        400,
        'The path [/importTest1/] is already covered by an other API.',
      );
    });

    afterAll(async () => {
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId1 });
      await apisResource.deleteApi({ envId, orgId, api: expectedApiId2 });
    });
  });

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

        let updatedApi = await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
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
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
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
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
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
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
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
        let updatedApi = await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
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

  describe('Update API form import with members', () => {
    describe('Update API that already has members, without specifying any members in data', () => {
      const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';

      let member;
      let primaryOwner;
      let roleName = 'MY_TEST_ROLE';
      let roleId;

      let fakeApi;

      test('should create user (future member)', async () => {
        member = await succeed(
          usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
        );
      });

      test('should create user (future primary owner)', async () => {
        primaryOwner = await succeed(
          usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
        );
      });

      test('should create role', async () => {
        let role = await succeed(
          rolesResource.createRoleRaw({ scope: RoleScope.API, orgId, newRoleEntity: RoleFaker.newRoleEntity({ name: roleName }) }),
        );
        roleId = role.id;
      });

      test('should create an API, and associate a member with role', async () => {
        fakeApi = ApisFaker.apiImport({
          id: '533efd8a-22e1-4483-a8af-0c24a2abd590',
          members: [{ source: 'gravitee', sourceId: member.email, roles: [roleId] }],
          primaryOwner: { id: primaryOwner.id, type: 'USER', email: primaryOwner.email },
        });

        let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
        expect(createdApi.id).toBe(expectedApiId);
      });

      test('should update the API, without any primaryOwner or members in data', async () => {
        fakeApi.members = [];
        fakeApi.primaryOwner = {};
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: fakeApi }));
      });

      test('should get API members, which has kept both members that were present before update', async () => {
        let members = await succeed(apisResource.getApiMembersRaw({ envId, orgId, api: expectedApiId }));
        expect(members).toHaveLength(2);
        expect(members).toEqual(
          expect.arrayContaining([
            expect.objectContaining({ id: primaryOwner.id, displayName: primaryOwner.displayName, role: 'PRIMARY_OWNER' }),
            expect.objectContaining({ id: member.id, displayName: member.displayName, role: 'MY_TEST_ROLE' }),
          ]),
        );
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
        await rolesResource.deleteRoleRaw({ scope: RoleScope.API, orgId, role: roleName });
        if (member) {
          await usersResource.deleteUser({ envId, orgId, userId: member.id });
        }
        if (primaryOwner) {
          await usersResource.deleteUser({ envId, orgId, userId: primaryOwner.id });
        }
      });
    });

    describe('Update API that has 2 members, updating the role of 1 member', () => {
      const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';

      let member1;
      let member2;
      let primaryOwner;
      let role1Name = 'MY_TEST_ROLE';
      let role1Id;
      let role2Name = 'MY_OTHER_ROLE';
      let role2Id;
      let fakeApi;

      test('should create user (member 1)', async () => {
        member1 = await succeed(
          usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
        );
      });

      test('should create user (member 2)', async () => {
        member2 = await succeed(
          usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
        );
      });

      test('should create user (primary owner)', async () => {
        primaryOwner = await succeed(
          usersResource.createUserRaw({ envId, orgId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() }),
        );
      });

      test('should create role1', async () => {
        let role = await succeed(
          rolesResource.createRoleRaw({ scope: RoleScope.API, orgId, newRoleEntity: RoleFaker.newRoleEntity({ name: role1Name }) }),
        );
        role1Id = role.id;
      });

      test('should create role2', async () => {
        let role = await succeed(
          rolesResource.createRoleRaw({ scope: RoleScope.API, orgId, newRoleEntity: RoleFaker.newRoleEntity({ name: role2Name }) }),
        );
        role2Id = role.id;
      });

      test('should create an API, with associated members', async () => {
        // member1 has role1, member2 has role2
        fakeApi = ApisFaker.apiImport({
          id: '533efd8a-22e1-4483-a8af-0c24a2abd590',
          members: [
            { source: 'gravitee', sourceId: member1.email, roles: [role1Id] },
            { source: 'gravitee', sourceId: member2.email, roles: [role2Id] },
          ],
          primaryOwner: { id: primaryOwner.id, type: 'USER', email: primaryOwner.email },
        });

        let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
        expect(createdApi.id).toBe(expectedApiId);
      });

      test('should update the API, without any primaryOwner or members in data', async () => {
        // member1 has role2 (changed), member2 has role2 (not changed)
        fakeApi.members = [
          { source: 'gravitee', sourceId: member1.email, roles: [role2Id] },
          { source: 'gravitee', sourceId: member2.email, roles: [role2Id] },
        ];
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: fakeApi }));
      });

      test('should export the API, resulting with member with updated roles', async () => {
        let exportedApi = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
        expect(exportedApi.members).toHaveLength(3);
        const member1Roles = exportedApi.members.filter((m) => m.sourceId == member1.email)[0].roles;
        expect(member1Roles).toHaveLength(2);
        expect(member1Roles).toContain(role1Id);
        expect(member1Roles).toContain(role2Id);
        const member2Roles = exportedApi.members.filter((m) => m.sourceId == member2.email)[0].roles;
        expect(member2Roles).toHaveLength(1);
        expect(member2Roles).toContain(role2Id);
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
        await rolesResource.deleteRoleRaw({ scope: RoleScope.API, orgId, role: role2Name });
        await rolesResource.deleteRoleRaw({ scope: RoleScope.API, orgId, role: role1Name });
        if (member1) {
          await usersResource.deleteUser({ envId, orgId, userId: member1.id });
        }
        if (member2) {
          await usersResource.deleteUser({ envId, orgId, userId: member2.id });
        }
        if (primaryOwner) {
          await usersResource.deleteUser({ envId, orgId, userId: primaryOwner.id });
        }
      });
    });
  });

  describe('Update API from import with plans', () => {
    describe('Update API with plans without ID', () => {
      const expectedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';

      const fakePlan1 = PlansFaker.aPlan({ name: 'test plan 1', description: 'this is a test plan', order: 1 });
      const fakePlan2 = PlansFaker.aPlan({ name: 'test plan 2', description: 'this is a test plan', order: 2 });
      const fakeApi = ApisFaker.apiImport({
        id: '08a92f8c-e133-42ec-a92f-8ce13382ec73',
      });

      // this update API, creating 2 plans
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [fakePlan1, fakePlan2] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get 2 plans created on API', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.STAGING] }));

        expect(plans).toHaveLength(2);
        expect(plans[0]).toEqual(
          expect.objectContaining({
            description: 'this is a test plan',
            validation: 'AUTO',
            security: PlanSecurityType.KEYLESS,
            type: PlanType.API,
            status: PlanStatus.STAGING,
            order: 1,
          }),
        );
        expect(plans[1]).toEqual(
          expect.objectContaining({
            description: 'this is a test plan',
            validation: 'AUTO',
            security: PlanSecurityType.KEYLESS,
            type: PlanType.API,
            status: PlanStatus.STAGING,
            order: 2,
          }),
        );
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with plans with ID', () => {
      const expectedApiId = 'e4998d06-6518-316e-a47b-e5112498c718';

      const fakePlan1 = PlansFaker.aPlan({
        id: '08a92f8c-e133-42ec-a92f-8ce139999999',
        name: 'test plan 1',
        description: 'this is a test plan',
        status: PlanStatus.CLOSED,
      });
      const fakePlan2 = PlansFaker.aPlan({
        id: '08a92f8c-e133-42ec-a92f-8ce138888888',
        name: 'test plan 2',
        description: 'this is a test plan',
        status: PlanStatus.CLOSED,
      });
      const fakeApi = ApisFaker.apiImport({ id: '6d94ad00-2878-44bc-aacc-5d9c2ac35034' });

      // this update API, creating 2 plans
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [fakePlan1, fakePlan2] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get 2 plans created on API, with specified status', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.CLOSED] }));
        expect(plans).toHaveLength(2);
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with plan without ID matching name of one existing plan', () => {
      const expectedApiId = '8f2ef0a4-27ca-3373-ba0c-c24b81f35ce0';

      const fakePlan1 = PlansFaker.aPlan({ name: 'test plan', description: 'this is a test plan' });
      const fakeApi = ApisFaker.apiImport({ id: 'd166c30a-0500-40a0-b414-a4853dc4bad8', plans: [fakePlan1] });

      // this update will update the plan of the existing API, cause it has the same name
      const updateFakePlan = PlansFaker.aPlan({ name: 'test plan', description: 'this is the updated description' });
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [updateFakePlan] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get the API plan, which has been updated', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.STAGING] }));
        expect(plans).toHaveLength(1);
        expect(plans[0].description).toBe('this is the updated description');
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with missing plans from already existing API', () => {
      // existing API contains 2 plans
      const expectedApiId = '3719057e-f218-3f28-a90f-10313fced230';

      const fakePlan1 = PlansFaker.aPlan({ name: 'test plan 1' });
      const fakePlan2 = PlansFaker.aPlan({ name: 'test plan 2' });
      const fakeApi = ApisFaker.apiImport({ id: '492eb123-a635-40a7-9438-fde72f11837e', plans: [fakePlan1, fakePlan2] });

      // update API contains 1 other plan
      const updateFakePlan = PlansFaker.aPlan({ name: 'test plan 3' });
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [updateFakePlan] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get the API plan, containing only the plan that was in the update', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.STAGING] }));
        expect(plans).toHaveLength(1);
        expect(plans[0].name).toBe('test plan 3');
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });
  });

  describe('Update API from import with metadata', () => {
    describe('Update API with some metadata having key that already exists', () => {
      const expectedApiId = 'f1def2c0-07d6-329d-8976-0d6191ee0f4c';

      const fakeApi = ApisFaker.apiImport({
        id: '9c4b1371-ecf5-4a04-8d8c-99a62ae0216a',
        metadata: [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Ops',
          },
        ],
      });

      let apiUpdate;

      test('should create an API with some metadata having a key with value "team"', async () => {
        let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
        expect(createdApi.id).toBe(expectedApiId);
      });

      test('should get the API metadata', async () => {
        let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
        expect(foundMetadata).toHaveLength(2);
        expect(foundMetadata).toEqual(
          expect.arrayContaining([
            expect.objectContaining({
              key: 'team',
              name: 'team',
              format: ApiMetadataFormat.STRING,
              value: 'Ops',
              apiId: expectedApiId,
            }),
          ]),
        );
      });

      test('should export the API', async () => {
        apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
      });

      test('should update the API metadata having the key "team"', async () => {
        apiUpdate.metadata = [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'DevOps',
          },
        ];

        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
      });

      test('should get the updated API metadata', async () => {
        let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
        expect(foundMetadata).toHaveLength(2);
        expect(foundMetadata).toEqual(
          expect.arrayContaining([
            expect.objectContaining({
              key: 'team',
              name: 'team',
              format: ApiMetadataFormat.STRING,
              value: 'DevOps',
              apiId: expectedApiId,
            }),
          ]),
        );
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with metadata having key that does not yet exist', () => {
      const expectedApiId = 'bf327615-1733-3d35-8614-c6713e87a812';

      const fakeApi = ApisFaker.apiImport({ id: '4fb4f3d7-e556-421c-b03f-5b2d3da3e774' });

      let apiUpdate;

      test('should create an API with no metadata', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should export the API', async () => {
        apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
      });

      test('should update API with some metadata having a key that does not yet exist', async () => {
        apiUpdate.metadata = [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Info Sec',
          },
        ];

        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
      });

      test('should get the created API metadata', async () => {
        let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
        expect(foundMetadata).toEqual(
          expect.arrayContaining([
            expect.objectContaining({
              key: 'team',
              name: 'team',
              format: ApiMetadataFormat.STRING,
              value: 'Info Sec',
              apiId: expectedApiId,
            }),
          ]),
        );
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with metadata having an undefined key', () => {
      const expectedApiId = '1a308f74-38d3-3bab-8202-eec1d8c3754c';

      const fakeApi = ApisFaker.apiImport({
        id: 'a67e7015-224c-4c32-abaa-231f58d4e542',
      });

      let apiUpdate;

      test('should create an API with no metadata', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should export the API', async () => {
        apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
      });

      test('should update the API, adding metadata with an undefined key', async () => {
        apiUpdate.metadata = [
          {
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Product',
          },
        ];

        await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
      });

      test('should get the API metadata', async () => {
        let foundMetadata = await succeed(apisResource.getApiMetadatasRaw({ envId, orgId, api: expectedApiId }));
        expect(foundMetadata[0]).toEqual({
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Product',
          apiId: expectedApiId,
        });
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });
  });

  describe('Update API from import with groups', () => {
    describe('Update API with with group name that already exists', () => {
      const expectedApiId = 'd2de4a96-6c5c-33d1-a220-8d41f8aecdb1';

      const groupName = 'customers';
      const fakeGroup = GroupsFaker.newGroup({ name: groupName });
      const fakeApi = ApisFaker.apiImport({ id: '70fbb369-5672-43e6-8a8c-ff7aa81a6055' });

      let groupId;
      let apiUpdate;

      test('should create a group with name "customers"', async () => {
        let createdGroup = await created(groupsResource.createGroupRaw({ envId, orgId, newGroupEntity: fakeGroup }));
        expect(createdGroup.name).toBe('customers');
        expect(createdGroup.id).toBeDefined();
        groupId = createdGroup.id;
      });

      test('should create an API associated with no groups', async () => {
        let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
        expect(createdApi.groups.length).toBe(0);
      });

      test('should export the API', async () => {
        apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
      });

      test('should update the API, associating it to the group "customers"', async () => {
        apiUpdate.groups = ['customers'];

        let updatedApi = await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
        expect(updatedApi.id).toBeTruthy();
        expect(updatedApi.groups).toHaveLength(1);
        expect(updatedApi.groups[0]).toBe(groupId);
      });

      afterAll(async () => {
        await groupsResource.deleteGroup({ envId, orgId, group: groupId });
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with with group name that does not exists', () => {
      const expectedApiId = 'e6908cb0-4e56-3b9c-b205-de8d49016a50';

      const groupName = 'sales';
      const fakeApi = ApisFaker.apiImport({ id: 'bc071378-7fb5-45df-841a-a2518668ae60', groups: ['support'] });

      let groupId;
      let apiUpdate;

      test('should create an API associated with no groups', async () => {
        let createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
        expect(createdApi.groups).toHaveLength(1);
      });

      test('should export the API', async () => {
        apiUpdate = JSON.parse(await succeed(apisResource.exportApiDefinitionRaw({ envId, orgId, api: expectedApiId })));
      });

      test('should update the API, associating it to the group "sales"', async () => {
        apiUpdate.groups = [groupName];

        let updatedApi = await succeed(apisResource.updateWithDefinitionPUTRaw({ envId, orgId, api: expectedApiId, body: apiUpdate }));
        expect(updatedApi.id).toBeTruthy();
        expect(updatedApi.groups).toHaveLength(1);
        groupId = updatedApi.groups[0];
      });

      test('should get the created group', async () => {
        let foundGroup = await succeed(groupsResource.getGroupRaw({ envId, orgId, group: groupId }));
        expect(foundGroup.name).toBe('sales');
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
        await groupsResource.deleteGroup({ envId, orgId, group: groupId });
      });
    });
  });
});
