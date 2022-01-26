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

import { PlanStatus, PlanValidation, PlanSecurityType, PlanType } from '@model/plan';
import { ApiImportFakers } from '@fakers/api-imports';
import {
  deleteApi,
  exportApi,
  getApiById,
  getApiMembers,
  getApiMetadata,
  importCreateApi,
  importUpdateApi,
} from '@commands/management/api-management-commands';
import { ADMIN_USER } from '@fakers/users/users';
import { ApiMetadataFormat, ApiPageType, ApiPrimaryOwnerType, ApiVisibility } from '@model/apis';
import { deletePage, getPage, getPages } from '@commands/management/api-pages-management-commands';
import { createUser, deleteUser } from '@commands/management/user-management-commands';
import { createRole, deleteRole } from '@commands/management/organization-configuration-management-commands';
import { getPlans } from '@commands/management/api-plans-management-commands';
import { GroupFakers } from '@fakers/groups';
import { createGroup, deleteGroup, getGroup } from '@commands/management/environment-management-commands';

context('API - Imports - Update', () => {
  describe('Update API from import', () => {
    describe('Update API which ID in URL does not exist', () => {
      const api = ApiImportFakers.api({ id: 'unknown-test-id' });

      it('should fail to update API, returning 404', () => {
        importUpdateApi(ADMIN_USER, 'unknown-test-id', api)
          .notFound()
          .should((response) => {
            expect(response.body.message).to.eq('Api [unknown-test-id] can not be found.');
          });
      });
    });

    describe('Update API which ID in URL exists, without ID in body', () => {
      const apiId = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
      const fakeCreateApi = ApiImportFakers.api({ id: apiId });

      // update API data. body doesn't contains API id
      const fakeUpdateApi = ApiImportFakers.api({
        name: 'updatedName',
        version: '1.1',
        description: 'Updated API description',
        visibility: ApiVisibility.PUBLIC,
      });
      fakeUpdateApi.proxy.virtual_hosts[0].path = '/updated/path';

      it('should create API with the specified ID', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi)
          .ok()
          .should((response) => {
            expect(response.body.id).to.eq(apiId);
          });
      });

      it('should update the API with the specified ID, even if no ID in body', () => {
        importUpdateApi(ADMIN_USER, apiId, fakeUpdateApi)
          .ok()
          .should((response) => {
            expect(response.body.id).to.eq(apiId);
          });
      });

      it('should get updated API with updated data', () => {
        getApiById(ADMIN_USER, apiId)
          .ok()
          .should((response) => {
            expect(response.body.id).to.eq(apiId);
            expect(response.body.name).to.eq('updatedName');
            expect(response.body.version).to.eq('1.1');
            expect(response.body.description).to.eq('Updated API description');
            expect(response.body.visibility).to.eq('PUBLIC');
            expect(response.body.proxy.virtual_hosts[0].path).to.eq('/updated/path');
          });
      });

      it('should delete created API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API which ID in URL exists, with another API ID in body', () => {
      const apiId1 = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
      const fakeCreateApi1 = ApiImportFakers.api({ id: apiId1, name: 'originalName' });

      const apiId2 = '67d8020e-b0b3-47d8-9802-0eb0b357d899';
      const fakeCreateApi2 = ApiImportFakers.api({ id: apiId2, name: 'originalName' });

      // that will update api2, with api1 id in body
      const fakeUpdateApi = ApiImportFakers.api({ id: apiId1, name: 'updatedName' });

      it('should create API 1', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi1).ok();
      });

      it('should create API 2', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi2).ok();
      });

      it('should update API 2, event if api1 id in body', () => {
        importUpdateApi(ADMIN_USER, apiId2, fakeUpdateApi)
          .ok()
          .should((response) => {
            expect(response.body.id).to.eq(apiId2);
          });
      });

      it('should get API1 with unchanged data', () => {
        getApiById(ADMIN_USER, apiId1)
          .ok()
          .should((response) => {
            expect(response.body.name).to.eq('originalName');
          });
      });

      it('should get API2 with updated data', () => {
        getApiById(ADMIN_USER, apiId2)
          .ok()
          .should((response) => {
            expect(response.body.name).to.eq('updatedName');
          });
      });

      it('should delete created API 1', () => {
        deleteApi(ADMIN_USER, apiId1).httpStatus(204);
      });

      it('should delete created API 2', () => {
        deleteApi(ADMIN_USER, apiId2).httpStatus(204);
      });
    });

    describe('Update API with an updated context path matching another API context path', () => {
      const apiId1 = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
      const fakeCreateApi1 = ApiImportFakers.api({ id: apiId1 });
      fakeCreateApi1.proxy.virtual_hosts[0].path = '/importTest1';

      const apiId2 = '67d8020e-b0b3-47d8-9802-0eb0b357d899';
      const fakeCreateApi2 = ApiImportFakers.api({ id: apiId2 });
      fakeCreateApi2.proxy.virtual_hosts[0].path = '/importTest2';

      // that will try to update api2, with the same context path as api1
      const fakeUpdateApi = ApiImportFakers.api();
      fakeUpdateApi.proxy.virtual_hosts[0].path = '/importTest1';

      it('should create API 1', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi1).ok();
      });

      it('should create API 2', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi2).ok();
      });

      it('should fail to update API 2 with same context path as API 1', () => {
        importUpdateApi(ADMIN_USER, apiId2, fakeUpdateApi)
          .badRequest()
          .should((response) => {
            expect(response.body.message).to.eq('The path [/importTest1/] is already covered by an other API.');
          });
      });

      it('should delete created API 1', () => {
        deleteApi(ADMIN_USER, apiId1).httpStatus(204);
      });

      it('should delete created API 2', () => {
        deleteApi(ADMIN_USER, apiId2).httpStatus(204);
      });
    });
  });

  describe('Update API from import with pages', () => {
    describe('Update API with existing page matching generated ID', () => {
      const apiId = '08a92f8c-e133-42ec-a92f-8ce13382ec73';
      const generatedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';

      const pageId = '7b95cbe6-099d-4b06-95cb-e6099d7b0609';
      const generatedPageId = 'c02077fc-7c4d-3c93-8404-6184a6221391';

      const fakeApi = ApiImportFakers.api({ id: apiId, environment_id: 'my-test-env' });
      const fakePage = ApiImportFakers.page({ id: pageId });
      fakeApi.pages = [fakePage];

      let apiUpdate;

      it('should create an API with one page of documentation and return a generated API ID', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', generatedApiId);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, generatedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update API page from specified ID', () => {
        const pageUpdate = ApiImportFakers.page({
          id: apiUpdate.pages[1].id,
          name: 'Documentation (updated)',
          content: '# Documentation\n## Contributing\nTo be done.',
        });
        apiUpdate.pages = [pageUpdate];
        importUpdateApi(ADMIN_USER, generatedApiId, apiUpdate).ok().its('body').should('have.property', 'id').should('eq', generatedApiId);
      });

      it('should get updated API page from generated page ID', () => {
        getPage(ADMIN_USER, generatedApiId, generatedPageId)
          .ok()
          .its('body')
          .should((page) => {
            expect(page.name).to.eq('Documentation (updated)');
            expect(page.content).to.eq('# Documentation\n## Contributing\nTo be done.');
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, generatedApiId).noContent();
      });
    });

    describe('Update API with existing page ID, using previous export, from same environment', () => {
      const apiId = '7061532e-c0e5-4894-818d-f747ad1601dc';
      const pageId = '4be08c28-5638-4fec-a90a-51c0cd403b12';

      const fakeApi = ApiImportFakers.api({ id: apiId });
      const fakePage = ApiImportFakers.page({ id: pageId });
      fakeApi.pages.push(fakePage);

      let apiUpdate;

      it('should create an API with one page of documentation', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should get API page from import definition id', () => {
        getPage(ADMIN_USER, apiId, pageId).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, using previous export', () => {
        importUpdateApi(ADMIN_USER, apiId, apiUpdate).ok();
      });

      it('should not have created additional API pages', () => {
        getPages(ADMIN_USER, apiId).ok().its('body').should('have.length', 2);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId);
      });
    });

    describe('Update API with existing page ID, using previous export, from different environment', () => {
      const apiId = '7061532e-c0e5-4894-818d-f747ad1601dc';
      const generatedApiId = '26bddf2a-b3df-345f-9298-76d389dc8920';

      const pageId = '4be08c28-5638-4fec-a90a-51c0cd403b12';
      const generatedPageId = '4b07e8b5-d30d-3568-ad3b-7ba0820d2f22';

      const fakeApi = ApiImportFakers.api({ id: apiId, environment_id: 'my-test-env' });
      const fakePage = ApiImportFakers.page({ id: pageId });
      fakeApi.pages.push(fakePage);

      let apiUpdate;

      it('should create an API with one page of documentation', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', generatedApiId);
      });

      it('should get API page from generated id', () => {
        getPage(ADMIN_USER, generatedApiId, generatedPageId).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, generatedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, using previous export', () => {
        importUpdateApi(ADMIN_USER, generatedApiId, apiUpdate).ok();
      });

      it('should not have created additional API pages', () => {
        getPages(ADMIN_USER, generatedApiId).ok().its('body').should('have.length', 2);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, generatedApiId).noContent();
      });
    });

    describe('Update API with page without ID', () => {
      const apiId = 'f5cc6ea7-1ea1-46dd-a48f-34a0386467b4';
      const fakeApi = ApiImportFakers.api({ id: apiId });
      const pageName = 'documentation';

      let apiUpdate;

      it('should create an API with no documentation page', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, adding one documentation page with a name and without an ID', () => {
        const fakePage = ApiImportFakers.page({ name: pageName });
        cy.wrap(fakePage).should('not.have.a.property', 'id');
        apiUpdate.pages = [ApiImportFakers.page(fakePage)];
        importUpdateApi(ADMIN_USER, apiId, apiUpdate).ok();
      });

      it('should have created the page', () => {
        getPages(ADMIN_USER, apiId).ok().its('body').should('have.length', 2).its(1).should('have.property', 'name').should('eq', pageName);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API, removing pages', () => {
      const apiId = '8fc829e8-b713-469f-8db5-06c702b82eb1';
      const fakeApi = ApiImportFakers.api({ id: apiId });
      fakeApi.pages.push(ApiImportFakers.page(), ApiImportFakers.page());

      let apiUpdate;

      it('should create an API with two pages of documentation', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, omitting some pages', () => {
        apiUpdate.pages = [];
        importUpdateApi(ADMIN_USER, apiId, apiUpdate).ok();
      });

      it('should not have deleted pages', () => {
        getPages(ADMIN_USER, apiId).ok().its('body').should('have.length', 3);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API, duplicating system folder', () => {
      const apiId = 'dfb569b9-a8e1-4ad4-9b84-0dd638ac2f30';
      const fakeApi = ApiImportFakers.api({ id: apiId });
      fakeApi.pages.push(ApiImportFakers.page());

      let apiUpdate;

      it('should create an API with one pages of documentation', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should have created a system folder page', () => {
        cy.wrap(apiUpdate)
          .its('pages')
          .should('have.length', 2)
          .should('satisfy', (pages) => pages.some(({ type }) => type === ApiPageType.SYSTEM_FOLDER));
      });

      it('should reject the import if trying to add a new system folder', () => {
        apiUpdate.pages = Array.of(ApiImportFakers.page({ type: ApiPageType.SYSTEM_FOLDER }));
        importUpdateApi(ADMIN_USER, apiId, apiUpdate).badRequest();
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API page in a page tree on another environment', () => {
      const apiId = '7f1af04f-339d-42e3-8d9e-ce478511ef13';
      const rootFolderId = '29b97194-8786-48cb-8162-d3989ce5ad48';
      const folderId = '7ef6a60d-3c29-459d-b05b-3d74ade03fa6';
      const pageId = '915bc210-445b-4b7b-888b-c676e3fb8c7e';

      const sourceEnvId = 'UAT';

      const fakeRootFolder = ApiImportFakers.page({ id: rootFolderId, type: ApiPageType.FOLDER, content: null });
      const fakeFolder = ApiImportFakers.page({ id: folderId, type: ApiPageType.FOLDER, parentId: rootFolderId, content: null });
      const fakePage = ApiImportFakers.page({ id: pageId, parentId: folderId });
      const fakeApi = ApiImportFakers.api({ id: apiId, pages: [fakeRootFolder, fakeFolder, fakePage], environment_id: sourceEnvId });

      const generatedApiId = '3a6c5568-aa36-3955-ac6f-9834cf00ec8c';
      const generatedFolderId = '3f8ddb41-0d27-37fc-bd30-a6801047f454';
      const generatedPageId = 'cb18fc2b-750f-3d5c-b6cf-7bc5a977b912';

      let apiUpdate;

      it('should create an API with a page tree and return a generated ID', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', generatedApiId);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, generatedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update API page in page tree', () => {
        const pageToUpdate = apiUpdate.pages.find((page) => page.id === generatedPageId);
        pageToUpdate.name = 'updated-page';
        pageToUpdate.content = '# Documentation (updated)';
        importUpdateApi(ADMIN_USER, generatedApiId, apiUpdate).ok().its('body').should('have.property', 'id').should('eq', generatedApiId);
      });

      it('should get updated page in folder', () => {
        getPages(ADMIN_USER, generatedApiId, { parent: generatedFolderId })
          .ok()
          .its('body')
          .should('have.length', 1)
          .its(0)
          .should((page) => {
            expect(page.name).to.eq('updated-page');
            expect(page.content).to.eq('# Documentation (updated)');
          });
      });

      it('should delete page in folder', () => {
        deletePage(ADMIN_USER, generatedApiId, generatedPageId).noContent();
      });

      it('should delete folder in root folder', () => {
        deletePage(ADMIN_USER, generatedApiId, generatedFolderId).noContent();
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, generatedApiId).noContent();
      });
    });

    describe('Update API page in a page tree on same environment', () => {
      const apiId = '29291bd9-ad06-4b80-88ca-8b988be49535';
      const rootFolderId = 'b47709fa-6a22-4c30-b653-dac0f328cda1';
      const folderId = '4cde77ba-7062-49c3-88f8-827f4d1ee7f1';
      const pageId = '3a684ba2-282b-4346-9866-9396dc638213';

      const fakeRootFolder = ApiImportFakers.page({ id: rootFolderId, type: ApiPageType.FOLDER, content: null });
      const fakeFolder = ApiImportFakers.page({ id: folderId, type: ApiPageType.FOLDER, parentId: rootFolderId, content: null });
      const fakePage = ApiImportFakers.page({ id: pageId, parentId: folderId });
      const fakeApi = ApiImportFakers.api({ id: apiId, pages: [fakeRootFolder, fakeFolder, fakePage] });

      let apiUpdate;

      it('should create an API with a page tree and return a generated ID', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', apiId);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update API page folder in page tree', () => {
        const pageToUpdate = apiUpdate.pages.find((page) => page.id === folderId);
        pageToUpdate.name = 'updated-folder';
        importUpdateApi(ADMIN_USER, apiId, apiUpdate).ok().its('body').should('have.property', 'id').should('eq', apiId);
      });

      it('should get updated folder in root folder', () => {
        getPages(ADMIN_USER, apiId, { parent: rootFolderId })
          .ok()
          .its('body')
          .should('have.length', 1)
          .its(0)
          .should('have.property', 'name')
          .should('eq', 'updated-folder');
      });

      it('should delete page in folder', () => {
        deletePage(ADMIN_USER, apiId, pageId).noContent();
      });

      it('should delete folder in root folder', () => {
        deletePage(ADMIN_USER, apiId, folderId).noContent();
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });
  });

  describe('Update API form import with members', () => {
    describe('Update API that already has members, without specifying any members in data', () => {
      const apiId = '533efd8a-22e1-4483-a8af-0c24a2abd590';
      let member;
      let primaryOwner;
      let roleName = 'MY_TEST_ROLE';
      let roleId;
      let fakeApi;

      it('should create user (future member)', () => {
        createUser(ADMIN_USER, ApiImportFakers.user())
          .ok()
          .then((response) => {
            member = response.body;
          });
      });

      it('should create user (future primary owner)', () => {
        createUser(ADMIN_USER, ApiImportFakers.user())
          .ok()
          .then((response) => {
            primaryOwner = response.body;
          });
      });

      it('should create role', () => {
        createRole(ADMIN_USER, ApiImportFakers.role({ name: roleName }))
          .ok()
          .then((response) => {
            roleId = response.body.id;
          });
      });

      it('should create an API, and associate a member with role', () => {
        const fakeMember = ApiImportFakers.member({ sourceId: member.email, roles: [roleId] });
        fakeApi = ApiImportFakers.api({
          id: apiId,
          members: [fakeMember],
          primaryOwner: { id: primaryOwner.id, type: ApiPrimaryOwnerType.USER, email: primaryOwner.email },
        });
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API, without any primaryOwner or members in data', () => {
        fakeApi.members = [];
        fakeApi.primaryOwner = {};
        importUpdateApi(ADMIN_USER, fakeApi.id, fakeApi).ok();
      });

      it('should get API members, which has kept both members that were present before update', () => {
        getApiMembers(ADMIN_USER, apiId)
          .ok()
          .should((response) => {
            expect(response.body).have.length(2);
            expect(response.body).deep.include({ id: primaryOwner.id, displayName: primaryOwner.displayName, role: 'PRIMARY_OWNER' });
            expect(response.body).deep.include({ id: member.id, displayName: member.displayName, role: 'MY_TEST_ROLE' });
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });

      it('should delete role', () => {
        deleteRole(ADMIN_USER, roleName).noContent();
      });

      it('should delete member user', () => {
        deleteUser(ADMIN_USER, member.id).noContent();
      });

      it('should delete primary owner user', () => {
        deleteUser(ADMIN_USER, primaryOwner.id).noContent();
      });
    });

    describe('Update API that has 2 members, updating the role of 1 member', () => {
      const apiId = '533efd8a-22e1-4483-a8af-0c24a2abd590';
      let member1;
      let member2;
      let primaryOwner;
      let role1Name = 'MY_TEST_ROLE';
      let role1Id;
      let role2Name = 'MY_OTHER_ROLE';
      let role2Id;
      let fakeApi;

      it('should create user (member 1)', () => {
        createUser(ADMIN_USER, ApiImportFakers.user())
          .ok()
          .then((response) => {
            member1 = response.body;
          });
      });

      it('should create user (member 2)', () => {
        createUser(ADMIN_USER, ApiImportFakers.user())
          .ok()
          .then((response) => {
            member2 = response.body;
          });
      });

      it('should create user (primary owner)', () => {
        createUser(ADMIN_USER, ApiImportFakers.user())
          .ok()
          .then((response) => {
            primaryOwner = response.body;
          });
      });

      it('should create role1', () => {
        createRole(ADMIN_USER, ApiImportFakers.role({ name: role1Name }))
          .ok()
          .then((response) => {
            role1Id = response.body.id;
          });
      });

      it('should create role2', () => {
        createRole(ADMIN_USER, ApiImportFakers.role({ name: role2Name }))
          .ok()
          .then((response) => {
            role2Id = response.body.id;
          });
      });

      it('should create an API, with associated members', () => {
        // member1 has role1, member2 has role2
        const fakeMember1 = ApiImportFakers.member({ sourceId: member1.email, roles: [role1Id] });
        const fakeMember2 = ApiImportFakers.member({ sourceId: member2.email, roles: [role2Id] });
        fakeApi = ApiImportFakers.api({
          id: apiId,
          members: [fakeMember1, fakeMember2],
          primaryOwner: { id: primaryOwner.id, type: ApiPrimaryOwnerType.USER, email: primaryOwner.email },
        });
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API, without any primaryOwner or members in data', () => {
        // member1 has role2 (changed), member2 has role2 (not changed)
        const fakeMember1 = ApiImportFakers.member({ sourceId: member1.email, roles: [role2Id] });
        const fakeMember2 = ApiImportFakers.member({ sourceId: member2.email, roles: [role2Id] });
        fakeApi.members = [fakeMember1, fakeMember2];
        importUpdateApi(ADMIN_USER, fakeApi.id, fakeApi).ok();
      });

      it('should export the API, resulting with member with updated roles', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .should((response) => {
            expect(response.body.members).have.length(3);
            const member1Roles = response.body.members.filter((m) => m.sourceId == member1.email)[0].roles;
            expect(member1Roles).have.length(2);
            expect(member1Roles).include(role1Id);
            expect(member1Roles).include(role2Id);
            const member2Roles = response.body.members.filter((m) => m.sourceId == member2.email)[0].roles;
            expect(member2Roles).have.length(1);
            expect(member2Roles).include(role2Id);
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });

      it('should delete role 1', () => {
        deleteRole(ADMIN_USER, role1Name).noContent();
      });

      it('should delete role 2', () => {
        deleteRole(ADMIN_USER, role2Name).noContent();
      });

      it('should delete member1 user', () => {
        deleteUser(ADMIN_USER, member1.id).noContent();
      });

      it('should delete member2 user', () => {
        deleteUser(ADMIN_USER, member2.id).noContent();
      });

      it('should delete primary owner user', () => {
        deleteUser(ADMIN_USER, primaryOwner.id).noContent();
      });
    });
  });

  describe('Update API from import with plans', () => {
    describe('Update API with plans without ID', () => {
      const apiId = '08a92f8c-e133-42ec-a92f-8ce13382ec73';
      const fakePlan1 = ApiImportFakers.plan({ name: 'test plan 1', description: 'this is a test plan' });
      const fakePlan2 = ApiImportFakers.plan({ name: 'test plan 2', description: 'this is a test plan' });
      const fakeApi = ApiImportFakers.api({ id: apiId });

      // this update API, creating 2 plans
      const updatedFakeApi = ApiImportFakers.api({ id: apiId, plans: [fakePlan1, fakePlan2] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, apiId, updatedFakeApi).ok();
      });

      it('should get 2 plans created on API', () => {
        getPlans(ADMIN_USER, apiId, PlanStatus.STAGING)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(2);
            expect(response.body[0].description).to.eq('this is a test plan');
            expect(response.body[0].validation).to.eq(PlanValidation.AUTO);
            expect(response.body[0].security).to.eq(PlanSecurityType.KEY_LESS);
            expect(response.body[0].type).to.eq(PlanType.API);
            expect(response.body[0].status).to.eq(PlanStatus.STAGING);
            expect(response.body[0].order).to.eq(0);
            expect(response.body[1].description).to.eq('this is a test plan');
            expect(response.body[1].validation).to.eq(PlanValidation.AUTO);
            expect(response.body[1].security).to.eq(PlanSecurityType.KEY_LESS);
            expect(response.body[1].type).to.eq(PlanType.API);
            expect(response.body[1].status).to.eq(PlanStatus.STAGING);
            expect(response.body[1].order).to.eq(0);
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API with plans with ID', () => {
      const apiId = '08a92f8c-e133-42ec-a92f-8ce13555ec73';
      const fakePlan1 = ApiImportFakers.plan({
        id: '08a92f8c-e133-42ec-a92f-8ce139999999',
        name: 'test plan 1',
        description: 'this is a test plan',
        status: PlanStatus.CLOSED,
      });
      const fakePlan2 = ApiImportFakers.plan({
        id: '08a92f8c-e133-42ec-a92f-8ce138888888',
        name: 'test plan 2',
        description: 'this is a test plan',
        status: PlanStatus.CLOSED,
      });
      const fakeApi = ApiImportFakers.api({ id: apiId });

      // this update API, creating 2 plans
      const updatedFakeApi = ApiImportFakers.api({ id: apiId, plans: [fakePlan1, fakePlan2] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, apiId, updatedFakeApi).ok();
      });

      it('should get 2 plans created on API, with specified status', () => {
        getPlans(ADMIN_USER, apiId, PlanStatus.CLOSED)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(2);
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API with plan without ID matching name of one existing plan', () => {
      const apiId = '08a92f8c-e133-42ec-a92f-8ce13382ec73';
      const fakePlan1 = ApiImportFakers.plan({ name: 'test plan', description: 'this is a test plan' });
      const fakeApi = ApiImportFakers.api({ id: apiId, plans: [fakePlan1] });

      // this update will update the plan of the existing API, cause it has the same name
      const updateFakePlan = ApiImportFakers.plan({ name: 'test plan', description: 'this is the updated description' });
      const updatedFakeApi = ApiImportFakers.api({ id: apiId, plans: [updateFakePlan] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, apiId, updatedFakeApi).ok();
      });

      it('should get the API plan, which has been updated', () => {
        getPlans(ADMIN_USER, apiId, PlanStatus.STAGING)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(1);
            expect(response.body[0].description).to.eq('this is the updated description');
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API with missing plans from already existing API', () => {
      // existing API contains 2 plans
      const apiId = '08a92f8c-e133-42ec-a92f-8ce13382ec73';
      const fakePlan1 = ApiImportFakers.plan({ name: 'test plan 1' });
      const fakePlan2 = ApiImportFakers.plan({ name: 'test plan 2' });
      const fakeApi = ApiImportFakers.api({ id: apiId, plans: [fakePlan1, fakePlan2] });

      // update API contains 1 other plan
      const updateFakePlan = ApiImportFakers.plan({ name: 'test plan 3' });
      const updatedFakeApi = ApiImportFakers.api({ id: apiId, plans: [updateFakePlan] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, apiId, updatedFakeApi).ok();
      });

      it('should get the API plan, containing only the plan that was in the update', () => {
        getPlans(ADMIN_USER, apiId, PlanStatus.STAGING)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(1);
            expect(response.body[0].name).to.eq('test plan 3');
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });
  });

  describe('Update API from import with metadata', () => {
    describe('Update API with some metadata having key that already exists', () => {
      const apiId = 'aa7f03dc-4ccf-434b-80b2-e5af22b0c76a';

      const fakeApi = ApiImportFakers.api({
        id: apiId,
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

      it('should create an API with some metadata having a key with value "team"', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should get the API metadata', () => {
        getApiMetadata(ADMIN_USER, apiId).ok().its('body').should('have.length', 2).should('deep.include', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Ops',
          apiId: 'aa7f03dc-4ccf-434b-80b2-e5af22b0c76a',
        });
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API metadata having the key "team"', () => {
        apiUpdate.metadata = [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'DevOps',
          },
        ];

        importUpdateApi(ADMIN_USER, apiId, apiUpdate).ok();
      });

      it('should get the updated API metadata', () => {
        getApiMetadata(ADMIN_USER, apiId).ok().its('body').should('have.length', 2).should('deep.include', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'DevOps',
          apiId: 'aa7f03dc-4ccf-434b-80b2-e5af22b0c76a',
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API with metadata having key that does not yet exist', () => {
      const apiId = '4fb4f3d7-e556-421c-b03f-5b2d3da3e774';

      const fakeApi = ApiImportFakers.api({ id: apiId });

      let apiUpdate;

      it('should create an API with no metadata', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update API with some metadata having a key that does not yet exist', () => {
        apiUpdate.metadata = [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Info Sec',
          },
        ];

        importUpdateApi(ADMIN_USER, apiId, apiUpdate).ok();
      });

      it('should get the created API metadata', () => {
        getApiMetadata(ADMIN_USER, apiId).ok().its('body').should('deep.include', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Info Sec',
          apiId: '4fb4f3d7-e556-421c-b03f-5b2d3da3e774',
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API with metadata having an undefined key', () => {
      const apiId = 'a67e7015-224c-4c32-abaa-231f58d4e542';

      const fakeApi = ApiImportFakers.api({
        id: apiId,
      });

      let apiUpdate;

      it('should create an API with no metadata', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, adding metadata with an undefined key', () => {
        apiUpdate.metadata = [
          {
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Product',
          },
        ];

        importUpdateApi(ADMIN_USER, apiId, apiUpdate).ok();
      });

      it('should get the API metadata', () => {
        getApiMetadata(ADMIN_USER, apiId).ok().its('body').its(0).should('deep.equal', {
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Product',
          apiId: 'a67e7015-224c-4c32-abaa-231f58d4e542',
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });
  });

  describe('Update API from import with groups', () => {
    describe('Update API with with group name that already exists', () => {
      const apiId = '70fbb369-5672-43e6-8a8c-ff7aa81a6055';
      const groupName = 'customers';
      const fakeGroup = GroupFakers.group({ name: groupName });
      const fakeApi = ApiImportFakers.api({ id: apiId });

      let groupId;
      let apiUpdate;

      it('should create a group with name "customers"', () => {
        createGroup(ADMIN_USER, fakeGroup)
          .created()
          .its('body')
          .should((body) => {
            expect(body.name).to.eq('customers');
          })
          .should('have.property', 'id')
          .then((id) => {
            groupId = id;
          });
      });

      it('should create an API associated with no groups', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').its('groups').should('be.empty');
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, associating it to the group "customers"', () => {
        apiUpdate.groups = ['customers'];

        importUpdateApi(ADMIN_USER, apiId, apiUpdate)
          .ok()
          .its('body')
          .should('have.property', 'groups')
          .should('have.length', 1)
          .its(0)
          .should('eq', groupId);
      });

      it('should delete the group', () => {
        deleteGroup(ADMIN_USER, groupId).noContent();
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Update API with with group name that does not exists', () => {
      const apiId = 'bc071378-7fb5-45df-841a-a2518668ae60';
      const groupName = 'sales';
      const fakeApi = ApiImportFakers.api({ id: apiId, groups: ['support'] });

      let groupId;
      let apiUpdate;

      it('should create an API associated with no groups', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'groups').should('have.length', 1);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, associating it to the group "sales"', () => {
        apiUpdate.groups = [groupName];

        importUpdateApi(ADMIN_USER, apiId, apiUpdate)
          .ok()
          .its('body')
          .should('have.property', 'groups')
          .should('have.length', 1)
          .its(0)
          .then((id) => {
            groupId = id;
          });
      });

      it('should get the created group', () => {
        getGroup(ADMIN_USER, groupId).ok().its('body').should('have.property', 'name').should('eq', 'sales');
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });

      it('should delete the group', () => {
        deleteGroup(ADMIN_USER, groupId).noContent();
      });
    });
  });
});
