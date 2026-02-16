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
    it('should call the API', done => {
      const fakeGroups = [fakeGroup()];

      groupService.list().subscribe(groups => {
        expect(groups).toMatchObject(fakeGroups);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeGroups);
    });
  });

  describe('listByOrganization', () => {
    it('should call the API', done => {
      const fakeGroups = [fakeGroup()];

      groupService.listByOrganization().subscribe(groups => {
        expect(groups).toMatchObject(fakeGroups);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/groups`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeGroups);
    });
  });

  describe('addOrUpdateMemberships', () => {
    it('should call the API', done => {
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

    it('should filter membership with empty roles', done => {
      const groupId = 'GROUP_ID';
      const groupMemberships: GroupMembership[] = [fakeGroupMembership({ roles: [] })];

      groupService.addOrUpdateMemberships(groupId, groupMemberships).subscribe(() => {
        done();
      });

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${groupId}/members`);
    });
  });

  describe('deleteMember', () => {
    it('should call the API', done => {
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
