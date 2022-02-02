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

    describe('Update API which an ID in URL that exists, without ID in body', () => {
      const apiId = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
      const expectedApiId = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

      const fakeCreateApi = ApiImportFakers.api({ id: apiId });

      // update API data. body doesn't contains API id
      const fakeUpdateApi = ApiImportFakers.api({
        name: 'updatedName',
        version: '1.1',
        description: 'Updated API description',
        visibility: ApiVisibility.PUBLIC,
        proxy: ApiImportFakers.proxy({
          virtual_hosts: [
            {
              path: '/updated/path',
            },
          ],
        }),
      });

      it('should create API generating a predictable id', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should update the API with the generated ID, even if no ID in body', () => {
        importUpdateApi(ADMIN_USER, expectedApiId, fakeUpdateApi)
          .ok()
          .its('body')
          .should('have.property', 'id')
          .should('eq', expectedApiId);
      });

      it('should get updated API with updated data', () => {
        getApiById(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .should(({ id, name, version, description, visibility, proxy }) => {
            expect(id).to.eq(expectedApiId);
            expect(name).to.eq('updatedName');
            expect(version).to.eq('1.1');
            expect(description).to.eq('Updated API description');
            expect(visibility).to.eq('PUBLIC');
            expect(proxy.virtual_hosts[0].path).to.eq('/updated/path');
          });
      });

      it('should delete created API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API which an ID in URL that exists, with another API ID in body', () => {
      const apiId1 = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
      const expectedApiId1 = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

      const fakeCreateApi1 = ApiImportFakers.api({ id: apiId1, name: 'originalName' });

      const apiId2 = '662712f4-8364-4e6b-825f-2008d59cc684';
      const expectedApiId2 = 'cb6cf46a-f396-3f80-9681-da23d9f2965c';

      const fakeCreateApi2 = ApiImportFakers.api({ id: apiId2, name: 'originalName' });

      // that will update api2, with api1 id in body
      const fakeUpdateApi = ApiImportFakers.api({ id: apiId1, name: 'updatedName' });

      it('should create API 1', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi1).ok().its('body').should('have.property', 'id').should('eq', expectedApiId1);
      });

      it('should create API 2', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi2).ok().its('body').should('have.property', 'id').should('eq', expectedApiId2);
      });

      it('should update API 2, event if api1 id in body', () => {
        importUpdateApi(ADMIN_USER, expectedApiId2, fakeUpdateApi)
          .ok()
          .its('body')
          .should('have.property', 'id')
          .should('eq', expectedApiId2);
      });

      it('should get API1 with unchanged data', () => {
        getApiById(ADMIN_USER, expectedApiId1)
          .ok()
          .should((response) => {
            expect(response.body.name).to.eq('originalName');
          });
      });

      it('should get API2 with updated data', () => {
        getApiById(ADMIN_USER, expectedApiId2)
          .ok()
          .should((response) => {
            expect(response.body.name).to.eq('updatedName');
          });
      });

      it('should delete created API 1', () => {
        deleteApi(ADMIN_USER, expectedApiId1).httpStatus(204);
      });

      it('should delete created API 2', () => {
        deleteApi(ADMIN_USER, expectedApiId2).httpStatus(204);
      });
    });

    describe('Update API with an updated context path matching another API context path', () => {
      const fakeCreateApi1 = ApiImportFakers.api({
        id: '67d8020e-b0b3-47d8-9802-0eb0b357d84c',
        proxy: ApiImportFakers.proxy({
          virtual_hosts: [
            {
              path: '/importTest1',
            },
          ],
        }),
      });
      const expectedApiId1 = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

      const fakeCreateApi2 = ApiImportFakers.api({
        id: '72dd3b21-b0cc-44a8-87d3-23e1f52b61fa',
        proxy: ApiImportFakers.proxy({
          virtual_hosts: [
            {
              path: '/importTest2',
            },
          ],
        }),
      });
      const expectedApiId2 = 'e6c25ef8-0946-3ea8-826b-4384aeda865c';

      // that will try to update api2, with the same context path as api1
      const fakeUpdateApi = ApiImportFakers.api({
        proxy: ApiImportFakers.proxy({
          virtual_hosts: [
            {
              path: '/importTest1',
            },
          ],
        }),
      });

      it('should create a first API, generating a predictable ID', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi1).ok().its('body').should('have.property', 'id').should('eq', expectedApiId1);
      });

      it('should create a second API, generating another predictable ID', () => {
        importCreateApi(ADMIN_USER, fakeCreateApi2).ok().its('body').should('have.property', 'id').should('eq', expectedApiId2);
      });

      it('should fail to update API 2 with same context path as API 1', () => {
        importUpdateApi(ADMIN_USER, expectedApiId2, fakeUpdateApi)
          .badRequest()
          .its('body')
          .should('have.property', 'message')
          .should('eq', 'The path [/importTest1/] is already covered by an other API.');
      });

      it('should delete created API 1', () => {
        deleteApi(ADMIN_USER, expectedApiId1).noContent();
      });

      it('should delete created API 2', () => {
        deleteApi(ADMIN_USER, expectedApiId2).noContent();
      });
    });
  });

  describe('Update API from import with pages', () => {
    describe('Update API with existing page matching generated ID', () => {
      const fakeApi = ApiImportFakers.api({
        id: '08a92f8c-e133-42ec-a92f-8ce13382ec73',
        pages: [ApiImportFakers.page({ id: '7b95cbe6-099d-4b06-95cb-e6099d7b0609' })],
      });
      const expectedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';
      const expectedPageId = '0b827e1e-afe2-3863-8533-a723c486d4ef';

      let apiUpdate;

      it('should create an API with one page of documentation and return a generated API ID', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update API page from generated ID', () => {
        const pageUpdate = ApiImportFakers.page({
          id: apiUpdate.pages[1].id,
          name: 'Documentation (updated)',
          content: '# Documentation\n## Contributing\nTo be done.',
        });
        apiUpdate.pages = [pageUpdate];
        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should get updated API page from generated page ID', () => {
        getPage(ADMIN_USER, expectedApiId, expectedPageId)
          .ok()
          .its('body')
          .should((page) => {
            expect(page.name).to.eq('Documentation (updated)');
            expect(page.content).to.eq('# Documentation\n## Contributing\nTo be done.');
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API with existing page ID, using previous export', () => {
      const fakeApi = ApiImportFakers.api({
        id: '7061532e-c0e5-4894-818d-f747ad1601dc',
        pages: [ApiImportFakers.page({ id: '4be08c28-5638-4fec-a90a-51c0cd403b12' })],
      });
      const expectedApiId = '26bddf2a-b3df-345f-9298-76d389dc8920';
      const expectedPageId = 'da768516-8c43-3077-93c2-bd75514c7575';

      let apiUpdate;

      it('should create an API with one page of documentation', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should get API page from generated id', () => {
        getPage(ADMIN_USER, expectedApiId, expectedPageId).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, using previous export', () => {
        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok();
      });

      it('should not have created additional API pages', () => {
        getPages(ADMIN_USER, expectedApiId).ok().its('body').should('have.length', 2);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId);
      });
    });

    describe('Update API with page without ID', () => {
      const apiId = 'f5cc6ea7-1ea1-46dd-a48f-34a0386467b4';
      const expectedApiId = '90bc8558-697f-37b4-b71b-9c56da60bd99';

      const fakeApi = ApiImportFakers.api({ id: apiId });
      const pageName = 'documentation';

      let apiUpdate;

      it('should create an API with no documentation page', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
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
        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok();
      });

      it('should have created the page', () => {
        getPages(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .should('have.length', 2)
          .its(1)
          .should('have.property', 'name')
          .should('eq', pageName);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API, removing pages', () => {
      const fakeApi = ApiImportFakers.api({
        id: '8fc829e8-b713-469f-8db5-06c702b82eb1',
        pages: [ApiImportFakers.page(), ApiImportFakers.page()],
      });
      const expectedApiId = 'acd83068-c27c-382b-a704-e9d64fc61d9c';

      let apiUpdate;

      it('should create an API with two pages of documentation', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, omitting some pages', () => {
        apiUpdate.pages = [];
        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok();
      });

      it('should not have deleted pages', () => {
        getPages(ADMIN_USER, expectedApiId).ok().its('body').should('have.length', 3);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API, duplicating system folder', () => {
      const fakeApi = ApiImportFakers.api({
        id: 'dfb569b9-a8e1-4ad4-9b84-0dd638ac2f30',
        pages: [ApiImportFakers.page()],
      });
      const expectedApiId = '0223d962-7162-3c45-b014-cd74520da77d';

      let apiUpdate;

      it('should create an API with one pages of documentation', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
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

      it('should not have duplicated the system folder', () => {
        getPages(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .should('have.length', 2)
          .should((pages) => {
            const systemFolders = pages.filter(({ type }) => type === ApiPageType.SYSTEM_FOLDER);
            expect(systemFolders).to.have.length(1);
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API page in a page tree', () => {
      const fakeApi = ApiImportFakers.api({
        id: '7f1af04f-339d-42e3-8d9e-ce478511ef13',
        pages: [
          ApiImportFakers.page({
            id: '29b97194-8786-48cb-8162-d3989ce5ad48',
            type: ApiPageType.FOLDER,
            content: null,
          }),
          ApiImportFakers.page({
            id: '7ef6a60d-3c29-459d-b05b-3d74ade03fa6',
            type: ApiPageType.FOLDER,
            parentId: '29b97194-8786-48cb-8162-d3989ce5ad48',
            content: null,
          }),
          ApiImportFakers.page({
            id: '915bc210-445b-4b7b-888b-c676e3fb8c7e',
            parentId: '7ef6a60d-3c29-459d-b05b-3d74ade03fa6',
          }),
        ],
      });
      const expectedApiId = '3a6c5568-aa36-3955-ac6f-9834cf00ec8c';
      const expectedFolderId = 'a7451cc1-bd10-3a06-be22-14d8e3d44145';
      const expectedPageId = '844a43b0-4e77-3a05-9880-137d9d64c224';

      let apiUpdate;

      it('should create an API with a page tree and return a generated ID', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update API page in page tree', () => {
        const pageToUpdate = apiUpdate.pages.find((page) => page.id === expectedPageId);
        pageToUpdate.name = 'updated-page';
        pageToUpdate.content = '# Documentation (updated)';
        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should get updated page in folder', () => {
        getPages(ADMIN_USER, expectedApiId, { parent: expectedFolderId })
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
        deletePage(ADMIN_USER, expectedApiId, expectedPageId).noContent();
      });

      it('should delete folder in root folder', () => {
        deletePage(ADMIN_USER, expectedApiId, expectedFolderId).noContent();
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
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
          id: '533efd8a-22e1-4483-a8af-0c24a2abd590',
          members: [fakeMember],
          primaryOwner: { id: primaryOwner.id, type: ApiPrimaryOwnerType.USER, email: primaryOwner.email },
        });

        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should update the API, without any primaryOwner or members in data', () => {
        fakeApi.members = [];
        fakeApi.primaryOwner = {};
        importUpdateApi(ADMIN_USER, expectedApiId, fakeApi).ok();
      });

      it('should get API members, which has kept both members that were present before update', () => {
        getApiMembers(ADMIN_USER, expectedApiId)
          .ok()
          .should((response) => {
            expect(response.body).have.length(2);
            expect(response.body).deep.include({ id: primaryOwner.id, displayName: primaryOwner.displayName, role: 'PRIMARY_OWNER' });
            expect(response.body).deep.include({ id: member.id, displayName: member.displayName, role: 'MY_TEST_ROLE' });
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
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
      const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';

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
          id: '533efd8a-22e1-4483-a8af-0c24a2abd590',
          members: [fakeMember1, fakeMember2],
          primaryOwner: { id: primaryOwner.id, type: ApiPrimaryOwnerType.USER, email: primaryOwner.email },
        });
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should update the API, without any primaryOwner or members in data', () => {
        // member1 has role2 (changed), member2 has role2 (not changed)
        const fakeMember1 = ApiImportFakers.member({ sourceId: member1.email, roles: [role2Id] });
        const fakeMember2 = ApiImportFakers.member({ sourceId: member2.email, roles: [role2Id] });
        fakeApi.members = [fakeMember1, fakeMember2];
        importUpdateApi(ADMIN_USER, expectedApiId, fakeApi).ok();
      });

      it('should export the API, resulting with member with updated roles', () => {
        exportApi(ADMIN_USER, expectedApiId)
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
        deleteApi(ADMIN_USER, expectedApiId).noContent();
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
      const expectedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';

      const fakePlan1 = ApiImportFakers.plan({ name: 'test plan 1', description: 'this is a test plan', order: 1 });
      const fakePlan2 = ApiImportFakers.plan({ name: 'test plan 2', description: 'this is a test plan', order: 2 });
      const fakeApi = ApiImportFakers.api({
        id: '08a92f8c-e133-42ec-a92f-8ce13382ec73',
      });

      // this update API, creating 2 plans
      const updatedFakeApi = ApiImportFakers.api({ id: expectedApiId, plans: [fakePlan1, fakePlan2] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, expectedApiId, updatedFakeApi).ok();
      });

      it('should get 2 plans created on API', () => {
        getPlans(ADMIN_USER, expectedApiId, PlanStatus.STAGING)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(2);
            expect(response.body[0].description).to.eq('this is a test plan');
            expect(response.body[0].validation).to.eq(PlanValidation.AUTO);
            expect(response.body[0].security).to.eq(PlanSecurityType.KEY_LESS);
            expect(response.body[0].type).to.eq(PlanType.API);
            expect(response.body[0].status).to.eq(PlanStatus.STAGING);
            expect(response.body[0].order).to.eq(1);
            expect(response.body[1].description).to.eq('this is a test plan');
            expect(response.body[1].validation).to.eq(PlanValidation.AUTO);
            expect(response.body[1].security).to.eq(PlanSecurityType.KEY_LESS);
            expect(response.body[1].type).to.eq(PlanType.API);
            expect(response.body[1].status).to.eq(PlanStatus.STAGING);
            expect(response.body[1].order).to.eq(2);
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API with plans with ID', () => {
      const expectedApiId = 'e4998d06-6518-316e-a47b-e5112498c718';

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
      const fakeApi = ApiImportFakers.api({ id: '6d94ad00-2878-44bc-aacc-5d9c2ac35034' });

      // this update API, creating 2 plans
      const updatedFakeApi = ApiImportFakers.api({ id: expectedApiId, plans: [fakePlan1, fakePlan2] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, expectedApiId, updatedFakeApi).ok();
      });

      it('should get 2 plans created on API, with specified status', () => {
        getPlans(ADMIN_USER, expectedApiId, PlanStatus.CLOSED)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(2);
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API with plan without ID matching name of one existing plan', () => {
      const expectedApiId = '8f2ef0a4-27ca-3373-ba0c-c24b81f35ce0';

      const fakePlan1 = ApiImportFakers.plan({ name: 'test plan', description: 'this is a test plan' });
      const fakeApi = ApiImportFakers.api({ id: 'd166c30a-0500-40a0-b414-a4853dc4bad8', plans: [fakePlan1] });

      // this update will update the plan of the existing API, cause it has the same name
      const updateFakePlan = ApiImportFakers.plan({ name: 'test plan', description: 'this is the updated description' });
      const updatedFakeApi = ApiImportFakers.api({ id: expectedApiId, plans: [updateFakePlan] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, expectedApiId, updatedFakeApi).ok();
      });

      it('should get the API plan, which has been updated', () => {
        getPlans(ADMIN_USER, expectedApiId, PlanStatus.STAGING)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(1);
            expect(response.body[0].description).to.eq('this is the updated description');
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API with missing plans from already existing API', () => {
      // existing API contains 2 plans
      const expectedApiId = '3719057e-f218-3f28-a90f-10313fced230';

      const fakePlan1 = ApiImportFakers.plan({ name: 'test plan 1' });
      const fakePlan2 = ApiImportFakers.plan({ name: 'test plan 2' });
      const fakeApi = ApiImportFakers.api({ id: '492eb123-a635-40a7-9438-fde72f11837e', plans: [fakePlan1, fakePlan2] });

      // update API contains 1 other plan
      const updateFakePlan = ApiImportFakers.plan({ name: 'test plan 3' });
      const updatedFakeApi = ApiImportFakers.api({ id: expectedApiId, plans: [updateFakePlan] });

      it('should create the API', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should update the API', () => {
        importUpdateApi(ADMIN_USER, expectedApiId, updatedFakeApi).ok();
      });

      it('should get the API plan, containing only the plan that was in the update', () => {
        getPlans(ADMIN_USER, expectedApiId, PlanStatus.STAGING)
          .ok()
          .should((response) => {
            expect(response.body).to.have.length(1);
            expect(response.body[0].name).to.eq('test plan 3');
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });
  });

  describe('Update API from import with metadata', () => {
    describe('Update API with some metadata having key that already exists', () => {
      const expectedApiId = 'f1def2c0-07d6-329d-8976-0d6191ee0f4c';

      const fakeApi = ApiImportFakers.api({
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

      it('should create an API with some metadata having a key with value "team"', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should get the API metadata', () => {
        getApiMetadata(ADMIN_USER, expectedApiId).ok().its('body').should('have.length', 2).should('deep.include', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Ops',
          apiId: expectedApiId,
        });
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
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

        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok();
      });

      it('should get the updated API metadata', () => {
        getApiMetadata(ADMIN_USER, expectedApiId).ok().its('body').should('have.length', 2).should('deep.include', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'DevOps',
          apiId: expectedApiId,
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API with metadata having key that does not yet exist', () => {
      const expectedApiId = 'bf327615-1733-3d35-8614-c6713e87a812';

      const fakeApi = ApiImportFakers.api({ id: '4fb4f3d7-e556-421c-b03f-5b2d3da3e774' });

      let apiUpdate;

      it('should create an API with no metadata', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
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

        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok();
      });

      it('should get the created API metadata', () => {
        getApiMetadata(ADMIN_USER, expectedApiId).ok().its('body').should('deep.include', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Info Sec',
          apiId: expectedApiId,
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API with metadata having an undefined key', () => {
      const expectedApiId = '1a308f74-38d3-3bab-8202-eec1d8c3754c';

      const fakeApi = ApiImportFakers.api({
        id: 'a67e7015-224c-4c32-abaa-231f58d4e542',
      });

      let apiUpdate;

      it('should create an API with no metadata', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
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

        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate).ok();
      });

      it('should get the API metadata', () => {
        getApiMetadata(ADMIN_USER, expectedApiId).ok().its('body').its(0).should('deep.equal', {
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Product',
          apiId: expectedApiId,
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });
  });

  describe('Update API from import with groups', () => {
    describe('Update API with with group name that already exists', () => {
      const expectedApiId = 'd2de4a96-6c5c-33d1-a220-8d41f8aecdb1';

      const groupName = 'customers';
      const fakeGroup = GroupFakers.group({ name: groupName });
      const fakeApi = ApiImportFakers.api({ id: '70fbb369-5672-43e6-8a8c-ff7aa81a6055' });

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
        exportApi(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, associating it to the group "customers"', () => {
        apiUpdate.groups = ['customers'];

        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate)
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
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Update API with with group name that does not exists', () => {
      const expectedApiId = 'e6908cb0-4e56-3b9c-b205-de8d49016a50';

      const groupName = 'sales';
      const fakeApi = ApiImportFakers.api({ id: 'bc071378-7fb5-45df-841a-a2518668ae60', groups: ['support'] });

      let groupId;
      let apiUpdate;

      it('should create an API associated with no groups', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'groups').should('have.length', 1);
      });

      it('should export the API', () => {
        exportApi(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .then((apiExport) => {
            apiUpdate = apiExport;
          });
      });

      it('should update the API, associating it to the group "sales"', () => {
        apiUpdate.groups = [groupName];

        importUpdateApi(ADMIN_USER, expectedApiId, apiUpdate)
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
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });

      it('should delete the group', () => {
        deleteGroup(ADMIN_USER, groupId).noContent();
      });
    });
  });
});
