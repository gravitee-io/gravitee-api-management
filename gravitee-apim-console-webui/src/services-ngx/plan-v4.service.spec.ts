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

import { PlanV4Service } from './plan-v4.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { NewPlan, PlanSecurityType, PlanType, PlanValidation } from '../entities/plan-v4';

describe('PlanV4Service', () => {
  let httpTestingController: HttpTestingController;
  let planService: PlanV4Service;
  const fakeRootScope = { $broadcast: jest.fn(), $on: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    planService = TestBed.inject<PlanV4Service>(PlanV4Service);
  });

  afterEach(() => {
    fakeRootScope.$broadcast.mockClear();
    httpTestingController.verify();
  });

  describe('create', () => {
    it('should create api plans', (done) => {
      const plan: NewPlan = {
        apiId: 'api-1',
        description: '',
        flows: [],
        status: 'published',
        type: PlanType.API,
        validation: PlanValidation.AUTO,
        name: 'free',
        security: { type: PlanSecurityType.API_KEY, configuration: {} },
      };

      planService.create(plan).subscribe((response) => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/v4/apis/${plan.apiId}/plans`,
      });
      expect(planReq.request.body).toEqual(plan);
      planReq.flush(plan);
    });
  });
});
