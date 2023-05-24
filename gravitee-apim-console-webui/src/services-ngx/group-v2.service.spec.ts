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

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { MembersResponse } from '../entities/management-api-v2';
import { fakeMember } from '../entities/management-api-v2/member/member.fixture';

describe('GroupV2Service', () => {
  let httpTestingController: HttpTestingController;
  let groupService: GroupV2Service;
  const GROUP_ID = 'my-group';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    groupService = TestBed.inject<GroupV2Service>(GroupV2Service);
  });

  describe('get members', () => {
    it('should call the API', (done) => {
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

      groupService.getMembers(GROUP_ID).subscribe((groups) => {
        expect(groups).toMatchObject(fakeMembersResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/${GROUP_ID}/members?page=1&perPage=10`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeMembersResponse);
    });

    it('should allow custom pagination', (done) => {
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

      groupService.getMembers(GROUP_ID, 2, 1).subscribe((groups) => {
        expect(groups).toMatchObject(fakeMembersResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/${GROUP_ID}/members?page=2&perPage=1`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeMembersResponse);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
