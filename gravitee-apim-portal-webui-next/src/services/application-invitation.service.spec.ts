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

import { ApplicationInvitationService } from './application-invitation.service';
import {
  fakeApplicationInvitationsCreateInput,
  fakeApplicationInvitationsResponse,
} from '../entities/application/application-invitation.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ApplicationInvitationService', () => {
  let service: ApplicationInvitationService;
  let httpTestingController: HttpTestingController;
  const applicationId = 'app-1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(ApplicationInvitationService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should search application invitations with default pagination', done => {
    const response = fakeApplicationInvitationsResponse();

    service.searchApplicationInvitations(applicationId).subscribe(res => {
      expect(res).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}/invitations/_search?page=1&size=10`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual({ filters: {} });
    req.flush(response);
  });

  it('should search application invitations with pagination and filters', done => {
    const response = fakeApplicationInvitationsResponse();

    service.searchApplicationInvitations(applicationId, 2, 25, { email: 'alice@example.com' }).subscribe(res => {
      expect(res).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}/invitations/_search?page=2&size=25`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual({ filters: { email: 'alice@example.com' } });
    req.flush(response);
  });

  it('should create application invitations', done => {
    const input = fakeApplicationInvitationsCreateInput({
      recipients: [{ email: 'alice@example.com' }, { email: 'bob@example.com' }],
    });
    const response = fakeApplicationInvitationsResponse();

    service.createApplicationInvitations(applicationId, input).subscribe(res => {
      expect(res).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}/invitations`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(input);
    req.flush(response);
  });

  it('should delete application invitation', done => {
    const invitationId = 'invitation-1';

    service.deleteApplicationInvitation(applicationId, invitationId).subscribe(() => done());

    const req = httpTestingController.expectOne(
      r => r.url === `${TESTING_BASE_URL}/applications/${applicationId}/invitations/${invitationId}` && r.method === 'DELETE',
    );
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
