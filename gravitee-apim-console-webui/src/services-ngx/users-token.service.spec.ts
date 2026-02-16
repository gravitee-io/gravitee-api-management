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

import { UsersTokenService } from './users-token.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeNewUserToken, fakeUserToken } from '../entities/user/userToken.fixture';

describe('UsersTokenService', () => {
  let httpTestingController: HttpTestingController;
  let usersTokenService: UsersTokenService;
  const userId = 'user-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    usersTokenService = TestBed.inject<UsersTokenService>(UsersTokenService);
  });

  describe('getTokens', () => {
    it('should return tokens', done => {
      const expectedUsersResult = [fakeUserToken(), fakeUserToken({ name: 'token2' })];

      usersTokenService.getTokens(userId).subscribe(tokens => {
        expect(tokens).toEqual(expectedUsersResult);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/users/${userId}/tokens`,
        })
        .flush(expectedUsersResult);
    });
  });

  describe('createToken', () => {
    it('should call the API', done => {
      const tokenToCreate = fakeNewUserToken();
      const createdToken = fakeUserToken();

      usersTokenService.createToken(userId, tokenToCreate).subscribe(token => {
        expect(token).toMatchObject({
          name: tokenToCreate.name,
        });
        done();
      });

      httpTestingController
        .expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.org.baseURL}/users/${userId}/tokens`,
        })
        .flush(createdToken);
    });
  });

  describe('revokeToken', () => {
    it('should call the API', done => {
      const tokenToDelete = fakeUserToken();

      usersTokenService.revokeToken(userId, tokenToDelete.id).subscribe(() => {
        done();
      });

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/users/${userId}/tokens/${tokenToDelete.id}`,
        })
        .flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
