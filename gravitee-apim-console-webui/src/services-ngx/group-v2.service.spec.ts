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

import { GroupV2Service } from './group-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeGroupsResponse, GroupsResponse, MembersResponse } from '../entities/management-api-v2';
import { fakeMember } from '../entities/management-api-v2/member/member.fixture';

describe('GroupV2Service', () => {
  let httpTestingController: HttpTestingController;
  let groupService: GroupV2Service;
  const GROUP_ID = 'my-group';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    groupService = TestBed.inject<GroupV2Service>(GroupV2Service);
  });

  describe('get members', () => {
    it('should call the API', done => {
      const fakeMembersResponse: MembersResponse = {
        data: [fakeMember()],
        metadata: {},
        pagination: {
          page: 1,
          pageCount: 1,
          pageItemsCount: 1,
          perPage: 10,
          totalCount: 1,
        },
      };

      groupService.getMembers(GROUP_ID).subscribe(groups => {
        expect(groups).toMatchObject(fakeMembersResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/${GROUP_ID}/members?page=1&perPage=10`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeMembersResponse);
    });

    it('should allow custom pagination', done => {
      const fakeMembersResponse: MembersResponse = {
        data: [fakeMember()],
        metadata: {
          groupName: 'group-display-name',
        },
        pagination: {
          page: 2,
          pageCount: 1,
          pageItemsCount: 1,
          perPage: 1,
          totalCount: 2,
        },
      };

      groupService.getMembers(GROUP_ID, 2, 1).subscribe(groups => {
        expect(groups).toMatchObject(fakeMembersResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/${GROUP_ID}/members?page=2&perPage=1`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeMembersResponse);
    });
  });

  describe('list groups', () => {
    it('should call API', done => {
      const groupsResponse: GroupsResponse = fakeGroupsResponse({ pagination: { page: 1, perPage: 10 } });

      groupService.list().subscribe(groups => {
        expect(groups).toMatchObject(groupsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=10`);
      expect(req.request.method).toEqual('GET');

      req.flush(groupsResponse);
    });

    it('should allow custom pagination', done => {
      const groupsResponse: GroupsResponse = fakeGroupsResponse({ pagination: { page: 2, perPage: 1 } });

      groupService.list(2, 1).subscribe(groups => {
        expect(groups).toMatchObject(groupsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=2&perPage=1`);
      expect(req.request.method).toEqual('GET');

      req.flush(groupsResponse);
    });
  });

  describe('listById', () => {
    it('should call API with a valid idList', done => {
      const validIdList = ['id1', 'id2'];
      const groupsResponse: GroupsResponse = fakeGroupsResponse();

      groupService.listById(validIdList).subscribe(groups => {
        expect(groups).toMatchObject(groupsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search?page=1&perPage=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ ids: validIdList });

      req.flush(groupsResponse);
    });

    it('should call API with an empty idList', done => {
      const groupsResponse: GroupsResponse = fakeGroupsResponse();

      groupService.listById([]).subscribe(groups => {
        expect(groups).toMatchObject(groupsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search?page=1&perPage=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ ids: [] });

      req.flush(groupsResponse);
    });

    it('should call API with default empty idList when no parameter is provided', done => {
      const groupsResponse: GroupsResponse = fakeGroupsResponse();

      groupService.listById().subscribe(groups => {
        expect(groups).toMatchObject(groupsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search?page=1&perPage=10`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ ids: [] });

      req.flush(groupsResponse);
    });

    it('should allow custom pagination', done => {
      const validIdList = ['id1', 'id2'];
      const groupsResponse: GroupsResponse = fakeGroupsResponse({ pagination: { page: 2, perPage: 5 } });

      groupService.listById(validIdList, 2, 5).subscribe(groups => {
        expect(groups).toMatchObject(groupsResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search?page=2&perPage=5`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ ids: validIdList });

      req.flush(groupsResponse);
    });

    it('should handle error response', done => {
      const validIdList = ['id1', 'id2'];
      const errorMessage = 'Error occurred while fetching groups by ID';
      const emptyGroupResponse: GroupsResponse = { data: [] };

      groupService.listById(validIdList).subscribe({
        next: groups => {
          expect(groups).toEqual(emptyGroupResponse);
          done();
        },
        error: () => done(),
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search?page=1&perPage=10`);
      req.flush({ message: errorMessage }, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
