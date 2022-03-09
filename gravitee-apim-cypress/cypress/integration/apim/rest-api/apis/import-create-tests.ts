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

import { PlanSecurityType, PlanStatus, PlanType, PlanValidation } from '@model/plan';
import { ApiImportFakers } from '@fakers/api-imports';
import { ADMIN_USER, API_PUBLISHER_USER, LOW_PERMISSION_USER } from '@fakers/users/users';
import {
  deleteApi,
  deployApi,
  getApiById,
  getApiMembers,
  getApiMetadata,
  importCreateApi,
  importSwaggerApi,
} from '@commands/management/api-management-commands';
import { deletePage, getPage, getPages } from '@commands/management/api-pages-management-commands';
import { getPlan } from '@commands/management/api-plans-management-commands';
import { ApiMetadataFormat, ApiPageType, ApiPrimaryOwnerType } from '@model/apis';
import { GroupFakers } from '@fakers/groups';
import { createGroup, deleteGroup, getGroup } from '@commands/management/environment-management-commands';
import { createUser, deleteUser, getCurrentUser } from '@commands/management/user-management-commands';
import { createRole, deleteRole } from '@commands/management/organization-configuration-management-commands';
import { ApiImport } from '@model/api-imports';
import { requestGateway } from 'support/common/http.commands';

context('API - Imports', () => {
  describe('Create API from import', () => {
    describe('Create API without ID', () => {
      let apiId;
      const fakeApi = ApiImportFakers.api();

      it('should create API and return generated ID', () => {
        importCreateApi(ADMIN_USER, fakeApi)
          .ok()
          .should((response) => {
            apiId = response.body.id;
            expect(apiId).to.not.be.null;
            expect(apiId).to.not.be.empty;
          });
      });

      it('should get created API with generated ID', () => {
        getApiById(ADMIN_USER, apiId)
          .ok()
          .should((response) => {
            expect(response.body.id).to.eq(apiId);
          });
      });

      it('should delete created API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Create empty API with an already existing context path', () => {
      const apiId1 = '67d8020e-b0b3-47d8-9802-0eb0b357d84c';
      const generatedApiId = '12d31c2a-1cdb-3824-a03e-606bc290bd7c';

      const fakeApi1 = ApiImportFakers.api({ id: apiId1 });
      fakeApi1.proxy.virtual_hosts[0].path = '/testimport/';

      // api2 has different ID, but same context path as api 1
      const apiId2 = '67d8020e-b0b3-47d8-9802-0eb0b357d84d';
      const fakeApi2 = ApiImportFakers.api({ id: apiId2 });
      fakeApi2.proxy.virtual_hosts[0].path = '/testimport/';

      it('should create API with the specified ID', () => {
        importCreateApi(ADMIN_USER, fakeApi1).ok().its('body').should('have.property', 'id').should('eq', generatedApiId);
      });

      it('should fail to create API with the same context path', () => {
        importCreateApi(ADMIN_USER, fakeApi2)
          .badRequest()
          .should((response) => {
            expect(response.body.message).to.eq('The path [/testimport/] is already covered by an other API.');
          });
      });

      it('should delete created API', () => {
        deleteApi(ADMIN_USER, generatedApiId).httpStatus(204);
      });
    });
  });

  describe('Create API from import with pages', () => {
    describe('Create API with one page without an ID', () => {
      const fakeApi = ApiImportFakers.api({ pages: [ApiImportFakers.page()] });

      let apiId, pageId;

      it('should create an API with one page of documentation and return a generated API ID', () => {
        importCreateApi(ADMIN_USER, fakeApi)
          .ok()
          .its('body')
          .should('have.property', 'id')
          .then((id) => {
            apiId = id;
          });
      });

      it('should get API documentation pages from generated API ID', () => {
        getPages(ADMIN_USER, apiId)
          .ok()
          .its('body')
          .should('have.length', 2)
          .its(1)
          .should('have.property', 'id')
          .then((id) => {
            pageId = id;
          });
      });

      it('should get page from generated page ID', () => {
        getPage(ADMIN_USER, apiId, pageId)
          .ok()
          .its('body')
          .should((page) => {
            expect(page.order).to.eq(1);
            expect(page.type).to.eq('MARKDOWN');
            expect(page.name).to.not.be.empty;
            expect(page.content).to.not.be.empty;
            expect(page.published).to.be.false;
            expect(page.homepage).to.be.false;
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, apiId).noContent();
      });
    });

    describe('Create API with one page with an ID', () => {
      const apiId = '08a92f8c-e133-42ec-a92f-8ce13382ec73';
      const expectedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';

      const pageId = '7b95cbe6-099d-4b06-95cb-e6099d7b0609';
      const expectedPageId = '0b827e1e-afe2-3863-8533-a723c486d4ef';

      const fakePage = ApiImportFakers.page({ id: pageId });
      const fakeApi = ApiImportFakers.api({ id: apiId, pages: [fakePage] });

      it('should create an API with one page of documentation and return specified ID', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should get API documentation pages from specified API ID', () => {
        getPages(ADMIN_USER, expectedApiId)
          .ok()
          .its('body')
          .should('have.length', 2)
          .its(1)
          .should('have.property', 'id')
          .should('eq', expectedPageId);
      });

      it('should get API page from generated page ID', () => {
        getPage(ADMIN_USER, expectedApiId, expectedPageId).ok().its('body').should('have.property', 'api').should('eq', expectedApiId);
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
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

      const fakeRootFolder = ApiImportFakers.page({ id: rootFolderId, type: ApiPageType.FOLDER, content: null });
      const fakeFolder = ApiImportFakers.page({ id: folderId, type: ApiPageType.FOLDER, parentId: rootFolderId, content: null });
      const fakePage = ApiImportFakers.page({ id: pageId, parentId: folderId });
      const fakeApi = ApiImportFakers.api({ id: apiId, pages: [fakeRootFolder, fakeFolder, fakePage] });

      it('should create an API with a page tree and return a generated ID', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', generatedApiId);
      });

      it('should get root folder from generated API ID', () => {
        getPages(ADMIN_USER, generatedApiId, { root: true })
          .ok()
          .its('body')
          .should('have.length', 2)
          .its(1)
          .should('have.property', 'id')
          .should('eq', generatedRootFolderId);
      });

      it('should get folder in root folder', () => {
        getPages(ADMIN_USER, generatedApiId, { parent: generatedRootFolderId })
          .ok()
          .its('body')
          .should('have.length', 1)
          .its(0)
          .should('have.property', 'id')
          .should('eq', generatedFolderId);
      });

      it('should get page in folder', () => {
        getPages(ADMIN_USER, generatedApiId, { parent: generatedFolderId })
          .ok()
          .its('body')
          .should('have.length', 1)
          .its(0)
          .should('have.property', 'id')
          .should('eq', generatedPageId);
      });

      it('should get API page from generated page ID', () => {
        getPage(ADMIN_USER, generatedApiId, generatedPageId).ok().its('body').should('have.property', 'api').should('eq', generatedApiId);
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

    describe('Create API with more than one system folder', () => {
      const pages = Array.from({ length: 2 }).map(() => ApiImportFakers.page({ type: ApiPageType.SYSTEM_FOLDER }));
      const fakeApi = ApiImportFakers.api({ pages });

      it('should reject the import', () => {
        importCreateApi(ADMIN_USER, fakeApi).badRequest();
      });
    });
  });

  describe('Create API from import with plans', () => {
    describe('Create API with plans without an ID', () => {
      let planId1;
      let planId2;

      const apiId = 'c4ddaa66-4646-4fca-a80b-284aa7407941';
      const expectedApiId = '7ab9bd67-5540-396b-91ca-91479994fdd6';

      const fakePlan1 = ApiImportFakers.plan({ name: 'test plan', description: 'this is a test plan' });
      const fakePlan2 = ApiImportFakers.plan({ name: 'test plan', description: 'this is a test plan' });
      const fakeApi = ApiImportFakers.api({ id: apiId, plans: [fakePlan1, fakePlan2] });

      it('should create an API and returns created plans in response', () => {
        importCreateApi(ADMIN_USER, fakeApi)
          .ok()
          .should((response) => {
            expect(response.body.plans).to.be.length(2);
            planId1 = response.body.plans[0].id;
            planId2 = response.body.plans[1].id;
            expect(planId1).to.not.be.null;
            expect(planId1).to.not.be.empty;
            expect(planId2).to.not.be.null;
            expect(planId2).to.not.be.empty;
            expect(planId1).to.not.eq(planId2);
          });
      });

      it('should get plan1 with correct data', () => {
        getPlan(ADMIN_USER, expectedApiId, planId1)
          .ok()
          .should((response) => {
            expect(response.body.id).to.eq(planId1);
            expect(response.body.name).to.eq('test plan');
            expect(response.body.description).to.eq('this is a test plan');
            expect(response.body.validation).to.eq(PlanValidation.AUTO);
            expect(response.body.security).to.eq(PlanSecurityType.KEY_LESS);
            expect(response.body.type).to.eq(PlanType.API);
            expect(response.body.status).to.eq(PlanStatus.STAGING);
            expect(response.body.order).to.eq(1);
          });
      });

      it('should get plan2 with correct data', () => {
        getPlan(ADMIN_USER, expectedApiId, planId2)
          .ok()
          .should((response) => {
            expect(response.body.id).to.eq(planId2);
            expect(response.body.name).to.eq('test plan');
            expect(response.body.description).to.eq('this is a test plan');
            expect(response.body.validation).to.eq(PlanValidation.AUTO);
            expect(response.body.security).to.eq(PlanSecurityType.KEY_LESS);
            expect(response.body.type).to.eq(PlanType.API);
            expect(response.body.status).to.eq(PlanStatus.STAGING);
            expect(response.body.order).to.eq(1);
          });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });
  });

  describe('Create API from import with metadata', () => {
    describe('Create API with metadata having key that does not yet exist', () => {
      const apiId = 'bc1287cb-b732-4ba1-b609-1e34d375b585';
      const expectedApiId = 'c954073c-0812-3544-b313-ad4f0001ffac';

      let fakeApi: ApiImport;
      fakeApi = ApiImportFakers.api({
        id: apiId,
        metadata: [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'API Management',
          },
        ],
      });

      it('should create an API with metadata', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should get the created API metadata', () => {
        getApiMetadata(ADMIN_USER, expectedApiId).ok().its('body').its(0).should('deep.equal', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'API Management',
          apiId: 'c954073c-0812-3544-b313-ad4f0001ffac',
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Create API with metadata having key already that already exists on an other API', () => {
      const firstApiId = 'b68e1a9c-a344-460d-b0ac-1d86d61b70cf';
      const firstExpectedApiId = 'd63a3aa0-46ba-3a93-9104-00ae4240645e';

      const secondApiId = '5668f9f0-12af-4541-b834-c374faedfb57';
      const secondExpectedApiId = 'b639cf39-7d66-3b36-af37-656233c4794b';

      const firstApi = ApiImportFakers.api({
        id: firstApiId,
        metadata: [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'API Management',
          },
        ],
      });

      const secondApi = ApiImportFakers.api({
        id: secondApiId,
        metadata: [
          {
            key: 'team',
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'Access Management',
          },
        ],
      });

      it('should create a first API with some metadata having a key named "team"', () => {
        importCreateApi(ADMIN_USER, firstApi).ok();
      });

      it('should create a second API with metadata having a key named "team"', () => {
        importCreateApi(ADMIN_USER, secondApi).ok();
      });

      it('should get API metadata for the first API', () => {
        getApiMetadata(ADMIN_USER, firstExpectedApiId).ok().its('body').its(0).should('deep.equal', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'API Management',
          apiId: firstExpectedApiId,
        });
      });

      it('should get API metadata for the second API', () => {
        getApiMetadata(ADMIN_USER, secondExpectedApiId).ok().its('body').its(0).should('deep.equal', {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Access Management',
          apiId: secondExpectedApiId,
        });
      });

      it('should delete the APIs', () => {
        deleteApi(ADMIN_USER, firstExpectedApiId).noContent();
        deleteApi(ADMIN_USER, secondExpectedApiId).noContent();
      });
    });

    describe('Create API with metadata having an undefined key', () => {
      const apiId = '4d73b285-5b87-4186-928e-f6f6240708f3';
      const expectedApiId = '08ee5d81-a6b8-3562-aaf6-b2c1313398cd';

      const fakeApi = ApiImportFakers.api({
        id: apiId,
        metadata: [
          {
            name: 'team',
            format: ApiMetadataFormat.STRING,
            value: 'QA',
          },
        ],
      });

      it('should create an API with some metadata having an empty key', () => {
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should get the API metadata', () => {
        getApiMetadata(ADMIN_USER, expectedApiId).ok().its('body').its(0).should('deep.equal', {
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'QA',
          apiId: expectedApiId,
        });
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });
  });

  describe('Create API from import with groups', () => {
    describe('Create API with with group name that already exists', () => {
      const apiId = '7ffe12cc-15b9-48ff-b436-0c9bb18b2816';
      const expectedApiId = 'fbbb2c4f-2aeb-31ed-8943-f4d5b03fa892';

      const groupName = 'architecture';
      const fakeGroup = GroupFakers.group({ name: groupName });
      const fakeApi = ApiImportFakers.api({ id: apiId, groups: [groupName] });

      let groupId;

      it('should create a group with name "architecture"', () => {
        createGroup(ADMIN_USER, fakeGroup)
          .created()
          .its('body')
          .should((body) => {
            expect(body.name).to.eq('architecture');
          })
          .should('have.property', 'id')
          .then((id) => {
            groupId = id;
          });
      });

      it('should create an API associated to the "architecture" group', () => {
        importCreateApi(ADMIN_USER, fakeApi)
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

    describe('Create API with with group name that does not exists', () => {
      const apiId = '533efd8a-22e1-4483-a8af-0c24a2abd590';
      const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';

      const groupName = 'performances';
      const fakeApi = ApiImportFakers.api({ id: apiId, groups: [groupName] });

      let groupId;

      it('should create an API associated to the "performances" group', () => {
        importCreateApi(ADMIN_USER, fakeApi)
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
        getGroup(ADMIN_USER, groupId).ok().its('body').should('have.property', 'name').should('eq', 'performances');
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });

      it('should delete the group', () => {
        deleteGroup(ADMIN_USER, groupId).noContent();
      });
    });
  });

  describe('Create API form import with members', () => {
    describe('Create API with member (member role is specified by id)', () => {
      const apiId = '721badf2-3563-4a84-b2e8-f69752fe416c';
      const expectedApiId = '01daa02c-b4ec-370e-9d2b-8732f4bf06b7';

      let member;
      let primaryOwner;
      let roleName = 'MY_TEST_ROLE';
      let roleId;

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

      it('should create an API, and associate a member with role, by role id', () => {
        const fakeMember = ApiImportFakers.member({ sourceId: member.email, roles: [roleId] });
        const fakeApi = ApiImportFakers.api({
          id: apiId,
          members: [fakeMember],
          primaryOwner: { id: primaryOwner.id, type: ApiPrimaryOwnerType.USER, email: primaryOwner.email },
        });
        importCreateApi(ADMIN_USER, fakeApi).ok();
      });

      it('should get API members, with a primary owner, and an additional member with associated role', () => {
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

    describe('Create API with member (member role is specified by name)', () => {
      const apiId = '533efd8a-22e1-4483-a8af-0c24a2abd590';
      const expectedApiId = 'b9bf4c5e-07dc-313d-b42c-c3217fa0ed93';

      let member;
      let primaryOwner;
      let roleName = 'MY_TEST_ROLE';
      let roleId;

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

      it('should create an API, and associate a member with role, by role name', () => {
        const fakeMember = ApiImportFakers.member({ sourceId: member.email, role: 'MY_TEST_ROLE' });
        const fakeApi = ApiImportFakers.api({
          id: apiId,
          members: [fakeMember],
          primaryOwner: { id: primaryOwner.id, type: ApiPrimaryOwnerType.USER, email: primaryOwner.email },
        });
        importCreateApi(ADMIN_USER, fakeApi).ok().its('body').should('have.property', 'id').should('eq', expectedApiId);
      });

      it('should get API members, with a primary owner, and an additional member with associated role', () => {
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
  });

  describe('Create API form import with primary owner', () => {
    describe('Create API with primary owner of type "USER", already existing with same id', () => {
      const apiId = '92d900c3-7497-4739-bb98-a8f3615c2773';
      const expectedApiId = '32961b80-aec9-3e1e-b9d5-b1e7e596f407';

      const fakeApi = ApiImportFakers.api({ id: apiId });

      let userId;

      it('should get "user" user ID', () => {
        getCurrentUser(LOW_PERMISSION_USER)
          .ok()
          .its('body')
          .should('have.property', 'id')
          .then((id) => {
            userId = id;
          });
      });

      it('should create an API with user "user" as a primary owner, omitting to use the display name', () => {
        fakeApi.primaryOwner = {
          id: userId,
          type: ApiPrimaryOwnerType.USER,
          displayName: 'Not to be used',
        };

        importCreateApi(ADMIN_USER, fakeApi)
          .ok()
          .its('body')
          .should('have.property', 'owner')
          .should('have.property', 'displayName')
          .should('eq', 'user');
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Create API with primary owner of type "USER", not existing with same id', () => {
      const apiId = 'df41e94d-ddd5-47b5-a9d8-973fefc4118f';
      const expectedApiId = '55dd7d69-a7a4-31fb-b96a-8d4338c25d82';

      const userId = 'i-do-not-exist';

      const fakeApi = ApiImportFakers.api({ id: apiId });

      it('should create an API, falling back to authenticated user as a primary owner', () => {
        fakeApi.primaryOwner = {
          id: userId,
          type: ApiPrimaryOwnerType.USER,
          displayName: 'Not to be used',
        };

        importCreateApi(ADMIN_USER, fakeApi)
          .ok()
          .its('body')
          .should('have.property', 'owner')
          .should('have.property', 'displayName')
          .should('eq', 'admin');
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });

    describe('Create API with primary owner of type "GROUP", already existing with same id', () => {
      const groupName = 'R&D';
      const apiId = '5446abb0-0199-4303-8b1a-fff57b0a9eac';
      const expectedApiId = 'cc28bec3-598a-3728-ad19-b5c4b6c67fca';

      // We have to set members to null because of #6808
      const fakeApi = ApiImportFakers.api({ id: apiId, members: null });
      const fakeGroup = GroupFakers.group({ name: groupName });

      let groupId;

      it('should create a group with name "R&D"', () => {
        createGroup(ADMIN_USER, fakeGroup)
          .created()
          .its('body')
          .should((body) => {
            expect(body.name).to.eq('R&D');
          })
          .should('have.property', 'id')
          .then((id) => {
            groupId = id;
          });
      });

      it('should create an API with the "R&D" group as a primary owner, omitting to use the display name', () => {
        fakeApi.primaryOwner = {
          id: groupId,
          type: ApiPrimaryOwnerType.GROUP,
          displayName: 'Not to be used',
        };

        importCreateApi(ADMIN_USER, fakeApi)
          .ok()
          .its('body')
          .should('have.property', 'owner')
          .should('have.property', 'displayName')
          .should('eq', 'R&D');
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });

      it('should delete the group', () => {
        deleteGroup(ADMIN_USER, groupId).noContent();
      });
    });

    describe('Create API with primary owner of type "GROUP", not existing with same id', () => {
      const apiId = 'b12f4414-76e1-425c-98e9-6f0c89c5b52d';
      const expectedApiId = 'd3a8a8d5-76f4-3ee4-bbfc-ffa031af89bc';

      const fakeApi = ApiImportFakers.api({ id: apiId, members: null });
      const groupId = 'i-do-not-exist';

      it('should create an API, falling back to "admin" as an API owner', () => {
        fakeApi.primaryOwner = {
          id: groupId,
          type: ApiPrimaryOwnerType.GROUP,
        };

        importCreateApi(ADMIN_USER, fakeApi)
          .ok()
          .its('body')
          .should('have.property', 'owner')
          .should('have.property', 'displayName')
          .should('eq', 'admin');
      });

      it('should delete the API', () => {
        deleteApi(ADMIN_USER, expectedApiId).noContent();
      });
    });
  });

  describe('API import via Swagger definition', () => {
    let swaggerImport: any;
    let apiId: string;

    beforeEach(() => {
      cy.fixture('json/petstore_swaggerv2.json').then((fileContent) => {
        swaggerImport = JSON.stringify(fileContent);
      });
    });

    afterEach(() => {
      deleteApi(ADMIN_USER, apiId).noContent();
    });

    it('should import API without creating a documentation', () => {
      importSwaggerApi(API_PUBLISHER_USER, swaggerImport)
        .created()
        .its('body')
        .then((api) => {
          apiId = api.id;
          expect(api.id).to.be.a('string').and.not.to.be.empty;
          expect(api.visibility).to.equal('PRIVATE');
          expect(api.state).to.equal('STOPPED');
          getPages(API_PUBLISHER_USER, apiId).ok().its('body').should('have.length', 1).should('not.have.a.property', 'SWAGGER');
        });
    });

    it('should import API and create a swagger documentation', () => {
      importSwaggerApi(API_PUBLISHER_USER, swaggerImport, { with_documentation: true })
        .created()
        .its('body')
        .then((api) => {
          apiId = api.id;
          expect(api.id).to.be.a('string').and.not.to.be.empty;
          expect(api.visibility).to.equal('PRIVATE');
          expect(api.state).to.equal('STOPPED');
          getPages(API_PUBLISHER_USER, apiId)
            .ok()
            .its('body')
            .should('have.length', 2)
            .its(1)
            .should((swaggerEntry) => {
              expect(swaggerEntry).to.have.property('id').and.not.to.be.empty;
              expect(swaggerEntry).to.have.property('type', 'SWAGGER');
              expect(swaggerEntry).to.have.property('content').and.contain(api.name);
            });
        });
    });

    it('should fail to import the same Swagger API again', () => {
      importSwaggerApi(API_PUBLISHER_USER, swaggerImport)
        .created()
        .its('body')
        .then((api) => {
          apiId = api.id;
          importSwaggerApi(API_PUBLISHER_USER, swaggerImport)
            .badRequest()
            .its('body.message')
            .should('equal', `The path [${api.context_path}/] is already covered by an other API.`);
        });
    });

    it('should import API and create a path (to add policies) for every declared Swagger path', () => {
      importSwaggerApi(API_PUBLISHER_USER, swaggerImport, { with_policy_paths: true })
        .created()
        .its('body')
        .then((api) => {
          apiId = api.id;
          deployApi(API_PUBLISHER_USER, apiId).its('body.flows').should('have.length', 20);
        });
    });
  });

  describe('Test API endpoints on Swagger import', () => {
    let mockPolicyApi: ApiImport;
    let jsonValidationPolicyApi: ApiImport;
    let noExtrasApi: ApiImport;
    let xmlValidationPolicyApi: ApiImport;

    before(() => {
      {
        cy.log('-----  Import a swagger API without any extra options selected  -----');
        cy.fixture('json/petstore_swaggerv2.json')
          .then((swaggerFile) => cy.createAndStartApiFromSwagger(swaggerFile))
          .then((api) => (noExtrasApi = api));
      }

      {
        cy.log('-----  Import a swagger API with mock policies  -----');
        const swaggerImportAttributes = {
          with_policy_paths: true,
          with_policies: ['mock'],
        };
        cy.fixture('json/petstore_swaggerv2.json')
          .then((swaggerFile) => cy.createAndStartApiFromSwagger(swaggerFile, swaggerImportAttributes))
          .then((api) => (mockPolicyApi = api));
      }

      {
        cy.log('-----  Import a swagger API with JSON-Validation policies  -----');
        const swaggerImportAttributes = {
          with_policy_paths: true,
          with_policies: ['json-validation'],
        };
        cy.fixture('json/petstore_swaggerv2.json')
          .then((swaggerFile) => cy.createAndStartApiFromSwagger(swaggerFile, swaggerImportAttributes))
          .then((api) => (jsonValidationPolicyApi = api));
      }

      {
        cy.log('-----  Import a swagger API with XML-Validation policies  -----');
        const swaggerImportAttributes = {
          with_policy_paths: true,
          with_policies: ['xml-validation'],
        };
        cy.fixture('json/petstore_swaggerv2.json')
          .then((swaggerFile) => cy.createAndStartApiFromSwagger(swaggerFile, swaggerImportAttributes))
          .then((api) => (xmlValidationPolicyApi = api));
      }
    });

    describe('Test without any extra options selected', () => {
      after(() => cy.teardownApi(noExtrasApi));

      it('should successfully connect to API endpoint', () => {
        requestGateway({ url: `${Cypress.env('gatewayServer')}${noExtrasApi.context_path}/pet/findByStatus?status=available` })
          .its('body')
          .should('have.length', 7)
          .its('0.name')
          .should('equal', 'Cat 1');
      });
    });

    describe('Tests mock path policy', () => {
      after(() => cy.teardownApi(mockPolicyApi));

      it('should get a mocked response when trying to reach API endpoint', () => {
        requestGateway({ url: `${Cypress.env('gatewayServer')}${mockPolicyApi.context_path}/pet/findByStatus?status=available` })
          .its('body.category.name')
          .should('equal', 'Mocked string');
      });
    });

    describe('Tests JSON-Validation path policy', () => {
      after(() => cy.teardownApi(jsonValidationPolicyApi));

      it('should fail with BAD REQUEST (400) when sending data using an invalid JSON schema', () => {
        requestGateway(
          {
            url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
            method: 'PUT',
            body: {
              invalidProperty: 'invalid value',
            },
          },
          {
            validWhen: (response) => response.status === 400,
          },
        ).should((response) => {
          expect(response.body.message).to.equal('Bad Request');
        });
      });

      it('should successfully connect to API endpoint if JSON schema is valid', () => {
        const body = {
          id: 2,
          category: {
            id: 0,
            name: 'string',
          },
          name: 'doggie',
          photoUrls: ['string'],
          tags: [
            {
              id: 0,
              name: 'string',
            },
          ],
          status: 'available',
        };
        requestGateway(
          {
            url: `${Cypress.env('gatewayServer')}${jsonValidationPolicyApi.context_path}/pet`,
            method: 'PUT',
            body,
          },
          {
            validWhen: (response) => response.status === 200,
          },
        ).should((response) => {
          expect(response.body.name).to.equal('doggie');
        });
      });
    });

    describe('Tests XML-Validation path policy', () => {
      after(() => cy.teardownApi(xmlValidationPolicyApi));

      it('should fail with BAD REQUEST (400) when sending data using an invalid XML schema', () => {
        requestGateway(
          {
            url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
            method: 'PUT',
            headers: {
              'Content-Type': 'application/xml',
            },
            body: {
              invalidProperty: 'invalid value',
            },
          },
          {
            validWhen: (response) => response.status === 400,
          },
        ).should((response) => {
          expect(response.body.message).to.equal('Bad Request');
        });
      });

      // test not working yet, needs investigation to figure out if there's an issue with the gateway
      it.skip('should successfully connect to API endpoint if XML schema is valid', () => {
        const body = `<?xml version="1.0" encoding="UTF-8"?><Pet><id>2</id><Category><id>0</id><name>string</name></Category><name>Cat 9</name><photoUrls><photoUrl>string</photoUrl></photoUrls><tags><Tag><id>0</id><name>string</name></Tag></tags><status>available</status></Pet>`;
        requestGateway(
          {
            url: `${Cypress.env('gatewayServer')}${xmlValidationPolicyApi.context_path}/pet`,
            method: 'PUT',
            headers: {
              'Content-Type': 'application/xml',
            },
            body,
          },
          {
            validWhen: (response) => response.status === 200,
          },
        ).should((response) => {
          expect(response.body.name).to.equal('Cat 9');
        });
      });
    });
  });
});
