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

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeAdminUser } from '../entities/user/user.fixture';
import { fakePagedResult } from '../entities/pagedResult';

describe('UsersService', () => {
  let httpTestingController: HttpTestingController;
  let usersService: UsersService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    usersService = TestBed.inject<UsersService>(UsersService);
  });

  describe('list', () => {
    it('should return users', (done) => {
      const expectedUsersResult = fakePagedResult([fakeAdminUser()]);

      usersService.list().subscribe((users) => {
        expect(users).toEqual(expectedUsersResult);
        done();
      });

      const page = 1;
      const size = 10;
      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users?page=${page}&size=${size}`);
      expect(req.request.method).toEqual('GET');

      req.flush(expectedUsersResult);
    });

    it('should return users with params', (done) => {
      const page = 2;
      const size = 25;
      const expectedUsersResult = fakePagedResult([fakeAdminUser()]);

      usersService.list('Joe', page, size).subscribe((users) => {
        expect(users).toEqual(expectedUsersResult);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users?page=${page}&size=${size}&q=Joe`);
      expect(req.request.method).toEqual('GET');

      req.flush(expectedUsersResult);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
