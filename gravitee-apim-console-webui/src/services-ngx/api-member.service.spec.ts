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

import { ApiMemberService } from './api-member.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { ApiMember, ApiMembership } from '../entities/api';

describe('ApiMemberService', () => {
  let httpTestingController: HttpTestingController;
  let apiMemberService: ApiMemberService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiMemberService = TestBed.inject<ApiMemberService>(ApiMemberService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get members', () => {
    it('should call the API', done => {
      const apiId = 'fox';
      const mockApiMembers: ApiMember[] = [
        { id: '1', role: 'king', displayName: 'Mufasa' },
        { id: '1', role: 'prince', displayName: 'Simba' },
      ];

      apiMemberService.getMembers(apiId).subscribe(response => {
        expect(response).toMatchObject(mockApiMembers);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members`,
      });

      req.flush(mockApiMembers);
    });
  });

  describe('update member', () => {
    it('should call the API', done => {
      const apiId = 'fox';
      const membership: ApiMembership = { id: '1', role: 'king', reference: 'ref' };
      apiMemberService.addOrUpdateMember(apiId, membership).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members`,
      });
      req.flush({});
    });
  });

  describe('delete member', () => {
    it('should call the API', done => {
      const apiId = 'fox';
      const memberId = 'id';
      apiMemberService.deleteMember(apiId, memberId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/members?user=${memberId}`,
      });
      req.flush({});
    });
  });
});
