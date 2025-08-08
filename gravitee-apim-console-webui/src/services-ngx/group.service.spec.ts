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

import { GroupService } from './group.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeGroup } from '../entities/group/group.fixture';
import { GroupMembership } from '../entities/group/groupMember';
import { fakeGroupMembership } from '../entities/group/groupMember.fixture';

describe('GroupService', () => {
  let httpTestingController: HttpTestingController;
  let groupService: GroupService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    groupService = TestBed.inject<GroupService>(GroupService);
  });

  describe('list', () => {
    it('should call the API', (done) => {
      const fakeGroups = [fakeGroup()];

      groupService.list().subscribe((groups) => {
        expect(groups).toMatchObject(fakeGroups);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeGroups);
    });
  });

  describe('listByOrganization', () => {
    it('should call the API', (done) => {
      const fakeGroups = [fakeGroup()];

      groupService.listByOrganization().subscribe((groups) => {
        expect(groups).toMatchObject(fakeGroups);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/groups`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeGroups);
    });
  });

  describe('listPaginated', () => {
    it('should call the API with default parameters', (done) => {
      const fakePagedGroups = {
        data: [fakeGroup()],
        page: { current: 1, total_pages: 1, total_elements: 1, size: 20 },
      };

      groupService.listPaginated().subscribe((pagedGroups) => {
        expect(pagedGroups).toMatchObject(fakePagedGroups);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/_paged?page=1&size=20&query=`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakePagedGroups);
    });

    it('should call the API with custom parameters', (done) => {
      const page = 2;
      const size = 10;
      const query = 'test';
      const fakePagedGroups = {
        data: [fakeGroup()],
        page: { current: page, total_pages: 2, total_elements: 15, size: size },
      };

      groupService.listPaginated(page, size, query).subscribe((pagedGroups) => {
        expect(pagedGroups).toMatchObject(fakePagedGroups);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/_paged?page=${page}&size=${size}&query=${query}`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(fakePagedGroups);
    });
  });

  describe('searchPaginated', () => {
    it('should call the API with default parameters', (done) => {
      const fakePagedGroups = {
        data: [fakeGroup()],
        page: { current: 1, total_pages: 1, total_elements: 1, size: 20 },
      };

      groupService.searchPaginated().subscribe((pagedGroups) => {
        expect(pagedGroups).toMatchObject(fakePagedGroups);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/_paged?page=1&size=20&sortOrder=ASC`,
      );
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ query: '', ids: [] });

      req.flush(fakePagedGroups);
    });

    it('should call the API with custom parameters', (done) => {
      const page = 2;
      const size = 10;
      const sortOrder = 'DESC';
      const query = 'test';
      const ids = ['group1', 'group2'];
      const fakePagedGroups = {
        data: [fakeGroup()],
        page: { current: page, total_pages: 2, total_elements: 15, size: size },
      };

      groupService.searchPaginated(page, size, sortOrder, query, ids).subscribe((pagedGroups) => {
        expect(pagedGroups).toMatchObject(fakePagedGroups);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/_paged?page=${page}&size=${size}&sortOrder=${sortOrder}`,
      );
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ query: query, ids: ids });

      req.flush(fakePagedGroups);
    });
  });

  describe('addOrUpdateMemberships', () => {
    it('should call the API', (done) => {
      const groupId = 'GROUP_ID';
      const groupMemberships: GroupMembership[] = [fakeGroupMembership()];

      groupService.addOrUpdateMemberships(groupId, groupMemberships).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${groupId}/members`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(groupMemberships);

      req.flush(null);
    });

    it('should filter membership with empty roles', (done) => {
      const groupId = 'GROUP_ID';
      const groupMemberships: GroupMembership[] = [fakeGroupMembership({ roles: [] })];

      groupService.addOrUpdateMemberships(groupId, groupMemberships).subscribe(() => {
        done();
      });

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${groupId}/members`);
    });
  });

  describe('deleteMember', () => {
    it('should call the API', (done) => {
      const groupId = 'groupId';
      const memberId = 'memberId';

      groupService.deleteMember(groupId, memberId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${groupId}/members/${memberId}`,
      });
      req.flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
