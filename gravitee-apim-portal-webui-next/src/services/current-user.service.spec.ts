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

import { CurrentUserService } from './current-user.service';
import { fakeUser } from '../entities/user/user.fixtures';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('CurrentUserService', () => {
  let service: CurrentUserService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(CurrentUserService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it('should load user', done => {
    expect(service.user()).toEqual({});
    const user = fakeUser();
    service.loadUser().subscribe(() => {
      expect(service.user()).toEqual(user);
      done();
    });

    httpTestingController.expectOne(`${TESTING_BASE_URL}/user`).flush(user);
  });

  it('should reset user on error', done => {
    service.user.set(fakeUser());

    service.loadUser().subscribe(() => {
      expect(service.user()).toEqual({});
      done();
    });

    httpTestingController.expectOne(`${TESTING_BASE_URL}/user`).flush('', { status: 401, statusText: 'Unauthorized' });
  });

  it('should clear user', () => {
    service.user.set(fakeUser());
    service.clear();
    expect(service.user()).toEqual({});
  });
});
