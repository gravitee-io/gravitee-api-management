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

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePlan } from '../entities/plan/plan.fixture';
import { fakeApi } from '../entities/api/Api.fixture';
import { NewPlan, PlanSecurityType } from '../entities/plan';

describe('PlanService', () => {
  let httpTestingController: HttpTestingController;
  let planService: PlanService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    planService = TestBed.inject<PlanService>(PlanService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getApiPlans', () => {
    it('should get all api plans', done => {
      const apiId = 'fox';
      const fakePlans = [fakePlan({ id: '1', name: 'free plan 😭' }), fakePlan({ id: '2', name: 'premium plan 💸' })];

      planService.getApiPlans(apiId).subscribe(response => {
        expect(response).toMatchObject(fakePlans);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/plans`,
      });

      req.flush(fakePlans);
    });

    it('should get only DEPRECATED api plans', done => {
      const apiId = 'fox';
      const fakePlans = [fakePlan({ id: '1', name: '✈️', status: 'DEPRECATED' })];

      planService.getApiPlans(apiId, 'DEPRECATED').subscribe(response => {
        expect(response).toMatchObject(fakePlans);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/plans?status=DEPRECATED`,
      });

      req.flush(fakePlans);
    });

    it('should get only PUBLISHED KEY_LESS api plans', done => {
      const apiId = 'fox';
      const fakePlans = [fakePlan({ id: '1', name: '🙅🔑', security: PlanSecurityType.KEY_LESS })];

      planService.getApiPlans(apiId, 'PUBLISHED', 'KEY_LESS').subscribe(response => {
        expect(response).toMatchObject(fakePlans);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/plans?status=PUBLISHED&security=KEY_LESS`,
      });

      req.flush(fakePlans);
    });
  });

  describe('update', () => {
    it('should update api plans', done => {
      const api = fakeApi();
      const plan = fakePlan();

      planService.update(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/plans/${plan.id}`,
      });
      expect(planReq.request.body).toEqual(plan);
      planReq.flush(plan);
    });

    it('should not publish apiChangeSuccess event', done => {
      const api = fakeApi({ gravitee: '1.0.0' });
      const plan = fakePlan();

      planService.update(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/plans/${plan.id}`,
      });
      expect(planReq.request.body).toEqual(plan);
      planReq.flush(plan);
    });
  });

  describe('create', () => {
    it('should create api plans', done => {
      const api = fakeApi();
      const plan: NewPlan = {
        name: 'free',
        security: PlanSecurityType.API_KEY,
      };

      planService.create(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/plans`,
      });
      expect(planReq.request.body).toEqual(plan);
      planReq.flush(plan);
    });
  });

  describe('get', () => {
    it('should get the api plan', done => {
      const plan = fakePlan();

      planService.get(plan.api, plan.id).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/apis/${plan.api}/plans/${plan.id}`,
        })
        .flush(plan);
    });
  });

  describe('publish', () => {
    it('should publish the api plan', done => {
      const api = fakeApi();
      const plan = fakePlan({ api: api.id });

      planService.publish(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${plan.api}/plans/${plan.id}/_publish`,
      });

      expect(req.request.body).toEqual(plan);
      req.flush(plan);
    });

    it('should not publish apiChangeSuccess event', done => {
      const api = fakeApi({ gravitee: '1.0.0' });
      const plan = fakePlan({ api: api.id });

      planService.publish(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${plan.api}/plans/${plan.id}/_publish`,
      });

      expect(req.request.body).toEqual(plan);
      req.flush(plan);
    });
  });

  describe('deprecate', () => {
    it('should deprecate the api plan', done => {
      const api = fakeApi();
      const plan = fakePlan({ api: api.id });

      planService.deprecate(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${plan.api}/plans/${plan.id}/_deprecate`,
      });

      expect(req.request.body).toEqual(plan);
      req.flush(plan);
    });

    it('should not publish apiChangeSuccess event', done => {
      const api = fakeApi({ gravitee: '1.0.0' });
      const plan = fakePlan({ api: api.id });

      planService.deprecate(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${plan.api}/plans/${plan.id}/_deprecate`,
      });

      expect(req.request.body).toEqual(plan);
      req.flush(plan);
    });
  });

  describe('close', () => {
    it('should close the api plan', done => {
      const api = fakeApi();
      const plan = fakePlan({ api: api.id });

      planService.close(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${plan.api}/plans/${plan.id}/_close`,
      });

      expect(req.request.body).toEqual({});
      req.flush(plan);
    });

    it('should not publish apiChangeSuccess event', done => {
      const api = fakeApi({ gravitee: '1.0.0' });
      const plan = fakePlan({ api: api.id });

      planService.close(api, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${plan.api}/plans/${plan.id}/_close`,
      });

      expect(req.request.body).toEqual({});
      req.flush(plan);
    });
  });
});
