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

import { ApplicationService } from './application.service';
import { Application } from '../entities/application/application';
import { fakeApplication } from '../entities/application/application.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ApplicationService', () => {
  let service: ApplicationService;
  let httpTestingController: HttpTestingController;
  const applicationId = 'testId';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(ApplicationService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should return application list', done => {
    const applicationResponse: Application = fakeApplication();
    service.get(applicationId).subscribe(response => {
      expect(response).toMatchObject(applicationResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/testId`);
    expect(req.request.method).toEqual('GET');

    req.flush(applicationResponse);
  });
});
