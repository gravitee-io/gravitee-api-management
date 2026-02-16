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

import { ApplicationMembersService } from './application-members.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeApplication } from '../entities/application/Application.fixture';
import { fakeMembers } from '../entities/members/Members.fixture';

describe('ApplicationMembersService', () => {
  let httpTestingController: HttpTestingController;
  let applicationMembersService: ApplicationMembersService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    applicationMembersService = TestBed.inject<ApplicationMembersService>(ApplicationMembersService);
  });

  describe('list Application members', () => {
    it('should call the list endpoint', done => {
      const applicationId = 'test-id';
      const applicationDetails = fakeApplication({ type: 'NATIVE' });

      applicationMembersService.get(applicationId).subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/applications/test-id/members`,
        })
        .flush(applicationDetails);
    });
  });

  describe('update Application member', () => {
    it('should call the update endpoint', done => {
      const applicationId = 'test-id';
      const member = [fakeMembers()][0];

      applicationMembersService.update(applicationId, member).subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/test-id/members`,
        method: 'POST',
      });

      req.flush(member);
    });
  });

  describe('delete Application member', () => {
    it('should call the delete endpoint', done => {
      const applicationId = 'test-id';
      const member = [fakeMembers()][0];

      applicationMembersService.delete(applicationId, member.id).subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/test-id/members?user=test-id`,
        method: 'DELETE',
      });

      req.flush(member);
    });
  });

  describe('transfer ownership Application member', () => {
    it('should call the API', done => {
      const fakeNewOwnership = {
        id: 'test-id',
        reference: 'test-reference',
        role: 'test-role',
      };

      applicationMembersService.transferOwnership('123', fakeNewOwnership).subscribe(() => done());

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/applications/123/members/transfer_ownership`,
      );
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(fakeNewOwnership);

      req.flush(fakeNewOwnership);
    });
  });
});
