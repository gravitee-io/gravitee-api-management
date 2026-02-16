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

import { ApiExposedEntrypointV2Service } from './api-exposed-entrypoints-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeExposedEntrypoint } from '../entities/management-api-v2/api/exposedEntrypoint.fixture';

describe('ApiExposedEntrypointV2Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiExposedEntrypointV2Service;
  const API_ID = 'api-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiExposedEntrypointV2Service>(ApiExposedEntrypointV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get', () => {
    it('should call the API', done => {
      service.get(API_ID).subscribe(res => {
        expect(res).toEqual([fakeExposedEntrypoint()]);
        done();
      });

      expectGetExposedEntrypoints(httpTestingController, API_ID, [fakeExposedEntrypoint()]);
    });
  });
});

export function expectGetExposedEntrypoints(httpTestingController: HttpTestingController, apiId: string, exposedEntrypoints: any[]) {
  httpTestingController
    .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/exposedEntrypoints`, method: 'GET' })
    .flush(exposedEntrypoints);
}
