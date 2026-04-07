/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { UsersV2Service } from './users-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('UsersV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: UsersV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(UsersV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getUserApis', () => {
    it('should call API with default pagination', done => {
      const fakeResponse = {
        data: [{ id: 'api1', name: 'API 1', version: '1.0', visibility: 'PUBLIC', environmentId: 'env1', environmentName: 'Env 1' }],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      service.getUserApis('userId', 'envId').subscribe(response => {
        expect(response).toEqual(fakeResponse);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/users/userId/apis?environmentId=envId&page=1&perPage=10`,
      );
      expect(req.request.method).toEqual('GET');
      req.flush(fakeResponse);
    });

    it('should allow custom pagination', done => {
      service.getUserApis('userId', 'envId', 2, 20).subscribe(() => done());

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/users/userId/apis?environmentId=envId&page=2&perPage=20`,
      );
      req.flush({ data: [], pagination: { page: 2, perPage: 20, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
    });
  });

  describe('getUserApplications', () => {
    it('should call API with default pagination', done => {
      const fakeResponse = {
        data: [{ id: 'app1', name: 'App 1', environmentId: 'env1', environmentName: 'Env 1' }],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      service.getUserApplications('userId', 'envId').subscribe(response => {
        expect(response).toEqual(fakeResponse);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/users/userId/applications?environmentId=envId&page=1&perPage=10`,
      );
      expect(req.request.method).toEqual('GET');
      req.flush(fakeResponse);
    });

    it('should allow custom pagination', done => {
      service.getUserApplications('userId', 'envId', 3, 5).subscribe(() => done());

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/users/userId/applications?environmentId=envId&page=3&perPage=5`,
      );
      req.flush({ data: [], pagination: { page: 3, perPage: 5, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
    });
  });

  describe('getUserGroups', () => {
    it('should call API with default pagination', done => {
      const fakeResponse = {
        data: [{ id: 'group1', name: 'Group 1', environmentId: 'env1', environmentName: 'Env 1', roles: { API: 'ADMIN' } }],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      service.getUserGroups('userId', 'envId').subscribe(response => {
        expect(response).toEqual(fakeResponse);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/users/userId/groups?environmentId=envId&page=1&perPage=10`,
      );
      expect(req.request.method).toEqual('GET');
      req.flush(fakeResponse);
    });

    it('should allow custom pagination', done => {
      service.getUserGroups('userId', 'envId', 2, 50).subscribe(() => done());

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.v2BaseURL}/users/userId/groups?environmentId=envId&page=2&perPage=50`,
      );
      req.flush({ data: [], pagination: { page: 2, perPage: 50, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
    });
  });
});
