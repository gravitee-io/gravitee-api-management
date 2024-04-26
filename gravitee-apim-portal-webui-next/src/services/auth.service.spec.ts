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

import { AuthService } from './auth.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it('should call log in endpoint', done => {
    const token = { token: 'token', token_type: 'BEARER' };
    service.login('username', 'password').subscribe(resp => {
      expect(resp).toEqual(token);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/login`);

    expect(req.request.headers.get('Authorization')).toEqual('Basic dXNlcm5hbWU6cGFzc3dvcmQ=');
    req.flush(token);
  });
});
