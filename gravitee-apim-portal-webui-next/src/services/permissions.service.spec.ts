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

import { PermissionsService } from './permissions.service';
import { fakeUserApiPermissions, fakeUserApplicationPermissions } from '../entities/permission/permission.fixtures';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('PermissionsService', () => {
  let service: PermissionsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PermissionsService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should get API permissions', done => {
    const apiId = 'api-id';
    const userApiPermissions = fakeUserApiPermissions({
      DEFINITION: ['R'],
      DOCUMENTATION: ['R', 'U'],
      PLAN: ['R'],
    });

    service.getApiPermissions(apiId).subscribe(response => {
      expect(response).toMatchObject(userApiPermissions);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/permissions?apiId=${apiId}`);
    expect(req.request.method).toEqual('GET');
    req.flush(userApiPermissions);
  });

  it('should get Application permissions', done => {
    const applicationId = 'application-id';
    const userApplicationPermissions = fakeUserApplicationPermissions({
      DEFINITION: ['C', 'R', 'U', 'D'],
      LOG: ['R'],
      SUBSCRIPTION: ['C', 'R', 'U'],
    });

    service.getApplicationPermissions(applicationId).subscribe(response => {
      expect(response).toMatchObject(userApplicationPermissions);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/permissions?applicationId=${applicationId}`);
    expect(req.request.method).toEqual('GET');
    req.flush(userApplicationPermissions);
  });
});
