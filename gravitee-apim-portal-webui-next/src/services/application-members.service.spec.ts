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

import { ApplicationMembersService } from './application-members.service';
import { ApplicationRolesV2Response, MembersV2Response } from '../entities/application-members/application-members';
import { fakeApplicationRolesResponse, fakeMembersResponse } from '../entities/application-members/application-members.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ApplicationMembersService', () => {
  let service: ApplicationMembersService;
  let httpTestingController: HttpTestingController;
  const applicationId = 'app-123';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(ApplicationMembersService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should list members with default params', done => {
    const response: MembersV2Response = fakeMembersResponse();
    service.list(applicationId).subscribe(result => {
      expect(result).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}/membersV2`);
    expect(req.request.method).toEqual('GET');
    req.flush(response);
  });

  it('should list members with page and size params', done => {
    const response: MembersV2Response = fakeMembersResponse();
    service.list(applicationId, 2, 20).subscribe(result => {
      expect(result).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(
      `${TESTING_BASE_URL}/applications/${applicationId}/membersV2?page=2&size=20`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(response);
  });

  it('should list members with query param', done => {
    const response: MembersV2Response = fakeMembersResponse();
    service.list(applicationId, 1, 10, 'admin').subscribe(result => {
      expect(result).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(
      `${TESTING_BASE_URL}/applications/${applicationId}/membersV2?page=1&size=10&query=admin`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(response);
  });

  it('should list roles', done => {
    const response: ApplicationRolesV2Response = fakeApplicationRolesResponse();
    service.listRoles().subscribe(result => {
      expect(result).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/applications/rolesV2`);
    expect(req.request.method).toEqual('GET');
    req.flush(response);
  });

  it('should update member role', done => {
    const memberId = 'member-1';
    service.updateMemberRole(applicationId, memberId, 'VIEWER').subscribe(result => {
      expect(result.role).toBe('VIEWER');
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}/membersV2/${memberId}`);
    expect(req.request.method).toEqual('PUT');
    expect(req.request.body).toEqual({ role: 'VIEWER' });
    req.flush({ id: memberId, role: 'VIEWER', status: 'ACTIVE', user: { id: 'user-1', display_name: 'Admin master' } });
  });
});
