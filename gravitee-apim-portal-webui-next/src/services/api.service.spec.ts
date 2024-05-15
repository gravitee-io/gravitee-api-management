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

import { ApiService } from './api.service';
import { fakeApisResponse } from '../entities/api/api.fixtures';
import { ApisResponse } from '../entities/api/apis-response';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ApiService', () => {
  let service: ApiService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ApiService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should return apis response with default page and size', done => {
      const apisResponse: ApisResponse = fakeApisResponse();

      service.search(1, 'ALL', '').subscribe(response => {
        expect(response).toMatchObject(apisResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/_search?page=1&category=ALL&size=9&q=`);
      expect(req.request.method).toEqual('POST');

      req.flush(apisResponse);
    });

    it('should return apis response with specified page and size', done => {
      const apisResponse: ApisResponse = fakeApisResponse();

      service.search(2, 'ALL', '', 99).subscribe(response => {
        expect(response).toMatchObject(apisResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/_search?page=2&category=ALL&size=99&q=`);
      expect(req.request.method).toEqual('POST');

      req.flush(apisResponse);
    });
  });
});
