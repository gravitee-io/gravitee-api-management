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

import { ApiScoreService } from './api-score.service';
import { fakeApiScoringTriggerResponse } from './api-score.fixture';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';

describe('ApiScoreService', () => {
  const apiId = 'api-id';
  let httpTestingController: HttpTestingController;
  let service: ApiScoreService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [ApiScoreService],
    });
    service = TestBed.inject(ApiScoreService);

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ApiScoreService>(ApiScoreService);
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
