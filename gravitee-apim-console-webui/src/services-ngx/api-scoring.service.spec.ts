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

import { ApiScoringService } from './api-scoring.service';

import { fakeApiScoringTriggerResponse } from '../management/api/scoring/api-scoring.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('ApiScoringService', () => {
  const apiId = 'api-id';
  let httpTestingController: HttpTestingController;
  let service: ApiScoringService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [ApiScoringService],
    });
    service = TestBed.inject(ApiScoringService);

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiScoringService>(ApiScoringService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('evaluate', () => {
    it('should call API', (done) => {
      const responsePayload = fakeApiScoringTriggerResponse();

      service.evaluate(apiId).subscribe((data) => {
        expect(data).toEqual(responsePayload);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/scoring/_evaluate`);
      req.flush(responsePayload);
    });
  });
});
