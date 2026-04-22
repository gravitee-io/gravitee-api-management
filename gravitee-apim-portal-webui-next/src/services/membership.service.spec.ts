/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { MembershipService } from './membership.service';
import { Member, MembersResponse } from '../entities/member/member';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('MembershipService', () => {
  let service: MembershipService;
  let httpTestingController: HttpTestingController;
  const applicationId = 'app-1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(MembershipService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should search application members with default pagination', done => {
    const members: Member[] = [{ id: 'm1', role: 'USER' }];
    const response: MembersResponse = {
      data: members,
      metadata: { pagination: { current_page: 1, total: 1, size: 10 } },
    };

    service.searchApplicationMembers(applicationId).subscribe(res => {
      expect(res).toEqual(response);
      done();
    });

    const req = httpTestingController.expectOne(
      r =>
        r.url === `${TESTING_BASE_URL}/applications/${applicationId}/members/_search` &&
        r.method === 'POST' &&
        r.params.get('page') === '1' &&
        r.params.get('size') === '10',
    );
    expect(req.request.body).toEqual({ filters: {} });
    req.flush(response);
  });

  it('should search application members with page, size and filters', done => {
    const response: MembersResponse = { data: [], metadata: { pagination: { current_page: 2, total: 0, size: 5 } } };

    service.searchApplicationMembers(applicationId, 2, 5, { displayName: 'Ada' }).subscribe(res => {
      expect(res).toEqual(response);
      done();
    });

    const req = httpTestingController.expectOne(
      r =>
        r.url === `${TESTING_BASE_URL}/applications/${applicationId}/members/_search` &&
        r.method === 'POST' &&
        r.params.get('page') === '2' &&
        r.params.get('size') === '5',
    );
    expect(req.request.body).toEqual({ filters: { displayName: 'Ada' } });
    req.flush(response);
  });
});
