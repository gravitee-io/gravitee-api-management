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

import { ApiMemberV2Service } from './api-member-v2.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { CreateApiMember, Member, MembersResponse, UpdateApiMember } from '../entities/management-api-v2';

describe('ApiMemberV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiMemberService: ApiMemberV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiMemberService = TestBed.inject<ApiMemberV2Service>(ApiMemberV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get members', () => {
    it('should call the API', (done) => {
      const apiId = 'fox';
      const mockApiMembers: Member[] = [
        { id: '1', roles: [{ name: 'king', scope: 'API' }] },
        { id: '1', roles: [{ name: 'prince', scope: 'API' }] },
      ];
      const mockResponse: MembersResponse = {
        data: mockApiMembers,
      };

      apiMemberService.getMembers(apiId).subscribe((response) => {
        expect(response).toMatchObject(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members`,
      });

      req.flush(mockResponse);
    });
  });

  describe('add member', () => {
    it('should call the API', (done) => {
      const apiId = 'fox';
      const membership: CreateApiMember = { userId: '1', roleName: 'king', externalReference: 'ref' };
      apiMemberService.addMember(apiId, membership).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members`,
      });
      req.flush({});
    });
  });

  describe('update member', () => {
    it('should call the API', (done) => {
      const apiId = 'fox';
      const membership: UpdateApiMember = { memberId: '1', roleName: 'king' };
      apiMemberService.updateMember(apiId, membership).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members/${membership.memberId}`,
      });
      req.flush({});
    });
  });

  describe('delete member', () => {
    it('should call the API', (done) => {
      const apiId = 'fox';
      const memberId = 'id';
      apiMemberService.deleteMember(apiId, memberId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/members/${memberId}`,
      });
      req.flush({});
    });
  });
});
