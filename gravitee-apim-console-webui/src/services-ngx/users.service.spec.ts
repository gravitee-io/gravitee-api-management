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

import { UsersService } from './users.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeAdminUser, fakeUser } from '../entities/user/user.fixture';
import { fakePagedResult } from '../entities/pagedResult';
import { fakeNewPreregisterUser } from '../entities/user/newPreRegisterUser.fixture';
import { fakeGroup } from '../entities/group/group.fixture';
import { fakeUserMembership } from '../entities/user/userMembership.fixture';
import { fakeSearchableUser } from '../entities/user/searchableUser.fixture';

describe('UsersService', () => {
  let httpTestingController: HttpTestingController;
  let usersService: UsersService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    usersService = TestBed.inject<UsersService>(UsersService);
  });

  describe('list', () => {
    it('should return users', done => {
      const expectedUsersResult = fakePagedResult([fakeAdminUser()]);

      usersService.list().subscribe(users => {
        expect(users).toEqual(expectedUsersResult);
        done();
      });

      const page = 1;
      const size = 10;
      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users?page=${page}&size=${size}`);
      expect(req.request.method).toEqual('GET');

      req.flush(expectedUsersResult);
    });

    it('should return users with params', done => {
      const page = 2;
      const size = 25;
      const expectedUsersResult = fakePagedResult([fakeAdminUser()]);

      usersService.list('Joe', page, size).subscribe(users => {
        expect(users).toEqual(expectedUsersResult);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users?page=${page}&size=${size}&q=Joe`);
      expect(req.request.method).toEqual('GET');

      req.flush(expectedUsersResult);
    });
  });

  describe('get', () => {
    it('should call the API', done => {
      const userId = 'userId';
      const userToGet = fakeUser({ id: userId });

      usersService.get(userId).subscribe(user => {
        expect(user).toMatchObject(userToGet);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}`);
      expect(req.request.method).toEqual('GET');

      req.flush(userToGet);
    });
  });

  describe('getUserAvatar', () => {
    it('should return url', () => {
      const userId = 'userId';

      expect(usersService.getUserAvatar(userId)).toEqual(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/avatar`);
    });
  });

  describe('getUserGroups', () => {
    it('should call the API', done => {
      const userId = 'userId';
      const fakeGroups = [fakeGroup()];

      usersService.getUserGroups(userId).subscribe(groups => {
        expect(groups).toMatchObject(fakeGroups);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/groups`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeGroups);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const userToCreate = fakeNewPreregisterUser();
      const createdUser = fakeUser();

      usersService.create(userToCreate).subscribe(user => {
        expect(user).toMatchObject({
          firstname: userToCreate.firstname,
          lastname: userToCreate.lastname,
          email: userToCreate.email,
          source: userToCreate.source,
          picture: userToCreate.picture,
          sourceId: userToCreate.sourceId,
          customFields: userToCreate.customFields,
        });
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users`);
      expect(req.request.method).toEqual('POST');

      req.flush(createdUser);
    });
  });

  describe('updateUserRoles', () => {
    it('should call the API', done => {
      const userId = 'userId';
      const referenceType = 'referenceType';
      const referenceId = 'referenceId';
      const roles = ['role1', 'role2'];

      usersService.updateUserRoles(userId, referenceType, referenceId, roles).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/roles`);
      expect(req.request.method).toEqual('PUT');

      req.flush(null);
    });
  });

  describe('getMemberships', () => {
    it('should call the API', done => {
      const userId = 'userId';
      const type = 'application';
      const userMembership = fakeUserMembership('application');

      usersService.getMemberships(userId, type).subscribe(memberships => {
        expect(memberships).toEqual(userMembership);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/memberships?type=${type}`);
      expect(req.request.method).toEqual('GET');

      req.flush(userMembership);
    });

    it('should init memberships and metadata if empty response', done => {
      const userId = 'userId';
      const type = 'application';

      usersService.getMemberships(userId, type).subscribe(memberships => {
        expect(memberships.memberships).toEqual([]);
        expect(memberships.metadata).toEqual({});
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/memberships?type=${type}`);
      expect(req.request.method).toEqual('GET');

      req.flush({});
    });
  });

  describe('resetPassword', () => {
    it('should call the API', done => {
      const userId = 'userId';

      usersService.resetPassword(userId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.org.baseURL}/users/${userId}/resetPassword`,
      });
      req.flush(null);
    });
  });

  describe('processRegistration', () => {
    it('should call the API', done => {
      const userId = 'userId';
      const accepted = true;

      usersService.processRegistration(userId, accepted).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/${userId}/_process`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(accepted);

      req.flush(null);
    });
  });

  describe('search', () => {
    it('should return users matching params', done => {
      const expectedUsersResult = [fakeSearchableUser()];

      usersService.search('joh').subscribe(users => {
        expect(users).toEqual(expectedUsersResult);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/search/users?q=joh`,
        })
        .flush(expectedUsersResult);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
