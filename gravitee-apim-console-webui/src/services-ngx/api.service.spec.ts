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

import { ApiService } from './api.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeApi } from '../entities/api/Api.fixture';
import { fakeFlowSchema } from '../entities/flow/flowSchema.fixture';
import { fakeUpdateApi } from '../entities/api/UpdateApi.fixture';

describe('ApiService', () => {
  let httpTestingController: HttpTestingController;
  let apiService: ApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiService = TestBed.inject<ApiService>(ApiService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get', () => {
    it('should call the API', (done) => {
      const apiName = 'fox';
      const mockApi = fakeApi();

      apiService.get(apiName).subscribe((response) => {
        expect(response).toMatchObject(mockApi);
        done();
      });

      const req = httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiName}` });

      req.flush(mockApi);
    });
  });

  describe('getFlowSchemaForm', () => {
    it('should call the API', (done) => {
      const mockApiFlow = fakeFlowSchema();

      apiService.getFlowSchemaForm().subscribe((response) => {
        expect(response).toEqual(mockApiFlow);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/schema`);
      expect(req.request.method).toEqual('GET');

      req.flush(mockApiFlow);
    });
  });

  describe('update', () => {
    it('should call the API', (done) => {
      const apiId = 'apiId';
      const apiToUpdate = fakeUpdateApi();

      apiService.update({ id: apiId, ...apiToUpdate }).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}` });
      expect(req.request.body).toEqual(apiToUpdate);
      req.flush({});
    });
  });
});
