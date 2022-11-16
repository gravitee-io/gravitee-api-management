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

import { PlanService } from './plan.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { AjsRootScope } from '../ajs-upgraded-providers';
import { fakePlan } from '../entities/plan/plan.fixture';

describe('PlanService', () => {
  let httpTestingController: HttpTestingController;
  let planService: PlanService;
  const fakeRootScope = { $broadcast: jest.fn(), $on: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
      providers: [{ provide: AjsRootScope, useValue: fakeRootScope }],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    planService = TestBed.inject<PlanService>(PlanService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getApiPlans', () => {
    it('should get all api plans', (done) => {
      const apiId = 'fox';
      const fakePlans = [fakePlan({ id: '1', name: 'free plan 😭' }), fakePlan({ id: '2', name: 'premium plan 💸' })];

      planService.getApiPlans(apiId).subscribe((response) => {
        expect(response).toMatchObject(fakePlans);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/plans`,
      });

      req.flush(fakePlans);
    });

    it('should get only DEPRECATED api plans', (done) => {
      const apiId = 'fox';
      const fakePlans = [fakePlan({ id: '1', name: '✈️', status: 'DEPRECATED' })];

      planService.getApiPlans(apiId, 'deprecated').subscribe((response) => {
        expect(response).toMatchObject(fakePlans);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/plans?status=deprecated`,
      });

      req.flush(fakePlans);
    });

    it('should get only PUBLISHED KEY_LESS api plans', (done) => {
      const apiId = 'fox';
      const fakePlans = [fakePlan({ id: '1', name: '🙅🔑', security: 'KEY_LESS' })];

      planService.getApiPlans(apiId, 'published', 'KEY_LESS').subscribe((response) => {
        expect(response).toMatchObject(fakePlans);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/plans?status=published&security=KEY_LESS`,
      });

      req.flush(fakePlans);
    });
  });
});
