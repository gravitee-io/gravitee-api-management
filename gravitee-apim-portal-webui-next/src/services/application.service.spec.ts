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
import { Application, ApplicationsResponse, ApplicationType } from '../entities/application/application';
import { fakeApplication, fakeApplicationsResponse, fakeSimpleApplicationType } from '../entities/application/application.fixture';
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
    const applicationResponse: ApplicationsResponse = fakeApplicationsResponse();
    service.list().subscribe(response => {
      expect(response).toMatchObject(applicationResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications?page=1&size=10`);
    expect(req.request.method).toEqual('GET');

    req.flush(applicationResponse);
  });

  it('should return application', done => {
    const application: Application = fakeApplication();
    service.get(applicationId).subscribe(response => {
      expect(response).toMatchObject(application);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}`);
    expect(req.request.method).toEqual('GET');

    req.flush(application);
  });

  it('should return application type configuration', done => {
    const applicationType: ApplicationType = fakeSimpleApplicationType();
    service.getType(applicationId).subscribe(response => {
      expect(response).toMatchObject(applicationType);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}/configuration`);
    expect(req.request.method).toEqual('GET');

    req.flush(applicationType);
  });

  it('should save application', done => {
    const application: Application = fakeApplication({ id: applicationId });
    service.save(application).subscribe(response => {
      expect(response).toMatchObject(application);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}`);
    expect(req.request.method).toEqual('PUT');
    expect(req.request.body).toEqual(application);

    req.flush(application);
  });

  it('should delete application', done => {
    service.delete(applicationId).subscribe(done());

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${applicationId}`);
    expect(req.request.method).toEqual('DELETE');

    req.flush(null);
  });
});
