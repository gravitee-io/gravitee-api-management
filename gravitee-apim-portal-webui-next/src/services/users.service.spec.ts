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

import { FinalizeRegistrationInput, RegisterUserInput, UsersService } from './users.service';
import { CustomUserField } from '../entities/user/custom-user-field';
import { User } from '../entities/user/user';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('UsersService', () => {
  let service: UsersService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
      providers: [UsersService],
    });

    service = TestBed.inject(UsersService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should list custom user fields', () => {
    const apiResponse: CustomUserField[] = [
      { key: 'company', label: 'Company', required: true, values: [] },
      { key: 'country', label: 'Country', required: false, values: ['PL', 'DE'] },
    ];

    service.listCustomUserFields().subscribe(res => {
      expect(res).toEqual(apiResponse);
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/users/custom-fields`);
    expect(req.request.method).toBe('GET');

    req.flush(apiResponse);
  });

  it('should register new user', () => {
    const input: RegisterUserInput = {
      email: 'john@doe.com',
      firstname: 'John',
      lastname: 'Doe',
      confirmation_page_url: 'http://example.local/confirm',
      customFields: { company: 'Acme Inc' },
    };

    const apiResponse = { id: 'user-1', email: 'john@doe.com' } as User;

    service.registerNewUser(input).subscribe(res => {
      expect(res).toEqual(apiResponse);
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/registration`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(input);

    req.flush(apiResponse);
  });

  it('should finalize registration', () => {
    const input: FinalizeRegistrationInput = {
      token: 'token-123',
      password: 'Secret123!',
      firstname: 'John',
      lastname: 'Doe',
    };

    const apiResponse = { id: 'user-1', email: 'john@doe.com' } as User;

    service.finalizeRegistration(input).subscribe(res => {
      expect(res).toEqual(apiResponse);
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/registration/_finalize`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(input);

    req.flush(apiResponse);
  });
});
