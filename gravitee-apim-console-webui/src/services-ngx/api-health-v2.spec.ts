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

import { ApiHealthV2Service } from './api-health-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeApiHealthResponseTimeOvertime } from '../entities/management-api-v2/api/v4/healthCheck.fixture';

describe('ApiHealthV2', () => {
  let httpTestingController: HttpTestingController;
  let service: ApiHealthV2Service;
  const apiId = 'testId';

  const { timeRange } = fakeApiHealthResponseTimeOvertime();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiHealthV2Service>(ApiHealthV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('getApiHealthResponseTimeOvertime should call the API', (done) => {
    service.getApiHealthResponseTimeOvertime(apiId, timeRange.from, timeRange.to).subscribe((res) => {
      expect(res).toEqual(fakeApiHealthResponseTimeOvertime());
      done();
    });

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/health/average-response-time-overtime?from=${timeRange.from}&to=${timeRange.to}`,
      })
      .flush(fakeApiHealthResponseTimeOvertime());
  });
});
