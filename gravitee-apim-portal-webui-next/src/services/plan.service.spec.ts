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

import { PlanService } from './plan.service';
import { fakePlansResponse } from '../entities/plan/plan.fixture';
import { PlansResponse } from '../entities/plan/plans-response';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('PlanService', () => {
  let service: PlanService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PlanService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should return plans response with default page and size', done => {
      const plansResponse: PlansResponse = fakePlansResponse();

      service.list('api-id').subscribe(response => {
        expect(response).toMatchObject(plansResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id/plans?size=-1`);
      expect(req.request.method).toEqual('GET');

      req.flush(plansResponse);
    });
  });
});
