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

import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ConfigService } from './config.service';
import { ResetPasswordService } from './reset-password.service';

describe('ResetPasswordService', () => {
  let service: ResetPasswordService;
  let httpClientMock: { post: jest.Mock };
  const TEST_BASE_URL = 'https://api.test.com';

  beforeEach(() => {
    httpClientMock = {
      post: jest.fn().mockReturnValue(of({})),
    };

    TestBed.configureTestingModule({
      providers: [
        ResetPasswordService,
        {
          provide: ConfigService,
          useValue: { baseURL: TEST_BASE_URL },
        },
        {
          provide: HttpClient,
          useValue: httpClientMock,
        },
      ],
    });

    service = TestBed.inject(ResetPasswordService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('resetPassword', () => {
    it('should send POST request to correct endpoint with username and reset URL', () => {
      const mockUsername = 'testuser';
      const mockResetUrl = 'https://portal/reset';

      service.resetPassword(mockUsername, mockResetUrl).subscribe();

      expect(httpClientMock.post).toHaveBeenCalledWith(`${TEST_BASE_URL}/users/_reset_password`, {
        username: mockUsername,
        reset_page_url: mockResetUrl,
      });
    });
  });

  describe('confirmResetPassword', () => {
    it('should send POST request to correct endpoint with user details and token', () => {
      const mockDetails = {
        firstname: 'John',
        lastname: 'Doe',
        password: 'newPassword123',
        token: 'valid-token',
      };

      service.confirmResetPassword(mockDetails.firstname, mockDetails.lastname, mockDetails.password, mockDetails.token).subscribe();

      expect(httpClientMock.post).toHaveBeenCalledWith(`${TEST_BASE_URL}/users/_change_password`, mockDetails);
    });

    it('should handle null token gracefully', () => {
      service.confirmResetPassword('John', 'Doe', 'newPassword123', null).subscribe();

      expect(httpClientMock.post).toHaveBeenCalledWith(`${TEST_BASE_URL}/users/_change_password`, {
        firstname: 'John',
        lastname: 'Doe',
        password: 'newPassword123',
        token: null,
      });
    });
  });
});
