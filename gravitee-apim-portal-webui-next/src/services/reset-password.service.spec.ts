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

import { ResetPasswordService } from './reset-password.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ResetPasswordService', () => {
  let service: ResetPasswordService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ResetPasswordService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('resetPassword', () => {
    it('should send POST request to correct endpoint with username and reset URL', done => {
      const mockUsername = 'testuser';
      const mockResetUrl = `${TESTING_BASE_URL}/reset-password/confirm/token`;

      service.resetPassword(mockUsername, mockResetUrl).subscribe(() => done());

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/_reset_password`);
      expect(req.request.method).toEqual('POST');

      req.flush({});
    });
  });

  describe('confirmResetPassword', () => {
    it('should send POST request to correct endpoint with user details and token', done => {
      const mockDetails = {
        firstname: 'John',
        lastname: 'Doe',
        password: 'newPassword123',
        token: 'valid-token',
      };

      service
        .confirmResetPassword(mockDetails.firstname, mockDetails.lastname, mockDetails.password, mockDetails.token)
        .subscribe(() => done());

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/_change_password`);
      expect(req.request.method).toEqual('POST');
      req.flush({});
    });
  });
});
