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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { RoleService } from './role.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeRole } from '../entities/role/role.fixture';
import { fakeMembershipListItem } from '../entities/role/membershipListItem.fixture';
import { fakePermissionsByScopes } from '../entities/role/permission.fixtures';

describe('RoleService', () => {
  let httpTestingController: HttpTestingController;
  let roleService: RoleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    roleService = TestBed.inject<RoleService>(RoleService);
  });

  describe('list', () => {
    it('should call the API', done => {
      const fakeRoles = [fakeRole()];
      const scope = 'SCOPE';

      roleService.list(scope).subscribe(roles => {
        expect(roles).toMatchObject(fakeRoles);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${scope}/roles`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeRoles);
    });
  });

  describe('listMemberships', () => {
    it('should call the API', done => {
      const membershipList = [fakeMembershipListItem()];
      const scope = 'ORGANIZATION';
      const roleName = 'ADMIN';

      roleService.listMemberships(scope, roleName).subscribe(memberships => {
        expect(memberships).toStrictEqual(membershipList);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${scope}/roles/${roleName}/users`,
        })
        .flush(membershipList);
    });
  });

  describe('deleteMembership', () => {
    it('should call the API', done => {
      const roleScope = 'ORGANIZATION';
      const roleName = 'ADMIN';
      const username = 'johndoe';

      roleService.deleteMembership(roleScope, roleName, username).subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleScope}/roles/${roleName}/users/${username}`,
        })
        .flush(null);
    });
  });

  describe('createMembership', () => {
    it('should call the API', done => {
      const roleScope = 'ORGANIZATION';
      const roleName = 'ADMIN';
      const membership = {
        id: 'user#1',
        reference: '08620e92ee2112001ade4000',
      };

      roleService.createMembership(roleScope, roleName, membership).subscribe(() => done());

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleScope}/roles/${roleName}/users`,
      });
      expect(req.request.body).toStrictEqual(membership);

      req.flush(null);
    });
  });

  describe('getPermissionsByScopes', () => {
    it('should call the API', done => {
      const scopes = fakePermissionsByScopes();

      roleService.getPermissionsByScopes().subscribe(response => {
        expect(response).toStrictEqual(scopes);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes`,
        })
        .flush(scopes);
    });
  });

  describe('getPermissionsByScope', () => {
    it('should call the API', done => {
      const scopes = fakePermissionsByScopes();

      roleService.getPermissionsByScope('ENVIRONMENT').subscribe(response => {
        expect(response).toStrictEqual(scopes.ENVIRONMENT);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes`,
        })
        .flush(scopes);
    });

    it('should throw if scope is not valid', async () => {
      const stringInvalidScope = 'BAD_SCOPE';

      expect(() => roleService.getPermissionsByScope(stringInvalidScope)).toThrow(
        'Invalid scope. The accepted scopes are API | APPLICATION | ENVIRONMENT | ORGANIZATION',
      );
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      const roleToDelete = fakeRole({
        scope: 'ORGANIZATION',
        name: 'admin',
      });

      roleService.delete(roleToDelete.scope, roleToDelete.name).subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/ORGANIZATION/roles/admin`,
        })
        .flush(null);
    });
  });

  describe('get', () => {
    it('should call the API', done => {
      const roleToGet = fakeRole({
        scope: 'ORGANIZATION',
        name: 'admin',
      });

      roleService.get(roleToGet.scope, roleToGet.name).subscribe(role => {
        expect(role).toMatchObject(roleToGet);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleToGet.scope}/roles/${roleToGet.name}`,
          method: 'GET',
        })
        .flush(roleToGet);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const roleToUpdate = fakeRole({
        scope: 'ORGANIZATION',
        name: 'admin',
      });

      roleService.update(roleToUpdate).subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleToUpdate.scope}/roles/${roleToUpdate.name}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual(roleToUpdate);

      req.flush(null);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const roleToCreate = fakeRole({
        scope: 'ORGANIZATION',
        name: 'admin',
      });

      roleService.create(roleToCreate).subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${roleToCreate.scope}/roles`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(roleToCreate);

      req.flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
