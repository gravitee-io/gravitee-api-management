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

import { ApiSpecGenRequestState, ApiSpecGenService, ApiSpecGenState } from './api-spec-gen.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('ApiSpecGenService', () => {
  const apiId = 'api-id';
  let httpTestingController: HttpTestingController;
  let service: ApiSpecGenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [ApiSpecGenService],
    });
    service = TestBed.inject(ApiSpecGenService);

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiSpecGenService>(ApiSpecGenService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('evaluate', () => {
    it('should call API /_state', (done) => {
      const response = { state: ApiSpecGenState.AVAILABLE } as ApiSpecGenRequestState;
      service.getState(apiId).subscribe((data) => {
        expect(data).toEqual(response);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/spec-gen/_state`);
      req.flush(response);
    });

    it('should call API /_start', (done) => {
      const response = { state: ApiSpecGenState.STARTED } as ApiSpecGenRequestState;

      service.postJob(apiId).subscribe((data) => {
        expect(data).toEqual(response);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/spec-gen/_start`);
      req.flush(response);
    });
  });
});
