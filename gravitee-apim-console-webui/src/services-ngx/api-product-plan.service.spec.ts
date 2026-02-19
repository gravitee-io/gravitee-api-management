/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { ApiProductPlanService } from './api-product-plan.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  ApiPlansResponse,
  CreatePlanV4,
  fakePlanV4,
  fakeUpdatePlanV4,
  PlanStatus,
} from '../entities/management-api-v2';

describe('ApiProductPlanService', () => {
  let httpTestingController: HttpTestingController;
  let apiProductPlanService: ApiProductPlanService;
  const API_PRODUCT_ID = 'api-product-id';
  const PLAN_ID = 'plan-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiProductPlanService = TestBed.inject(ApiProductPlanService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', done => {
      const fakeApiPlansResponse: ApiPlansResponse = {
        data: [fakePlanV4({ id: PLAN_ID })],
      };

      apiProductPlanService.list(API_PRODUCT_ID).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual([fakePlanV4({ id: PLAN_ID })]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(fakeApiPlansResponse);
    });

    it('should list with statuses', done => {
      const statuses: PlanStatus[] = ['STAGING', 'PUBLISHED'];
      const fakeApiPlansResponse: ApiPlansResponse = {
        data: [fakePlanV4({ id: PLAN_ID })],
      };

      apiProductPlanService.list(API_PRODUCT_ID, statuses).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual([fakePlanV4({ id: PLAN_ID })]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=10&statuses=STAGING,PUBLISHED`,
        method: 'GET',
      });

      req.flush(fakeApiPlansResponse);
    });
  });

  describe('get', () => {
    it('should get the api product plan', done => {
      const plan = fakePlanV4({ id: PLAN_ID });

      apiProductPlanService.get(API_PRODUCT_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`,
        })
        .flush(plan);
    });
  });

  describe('create', () => {
    it('should create api product plan', done => {
      const plan: CreatePlanV4 = {
        description: '',
        definitionVersion: 'V4',
        flows: [],
        validation: 'AUTO',
        name: 'free',
        mode: 'STANDARD',
        security: { type: 'API_KEY', configuration: {} },
      };

      apiProductPlanService.create(API_PRODUCT_ID, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans`,
      });
      expect(planReq.request.body).toEqual(plan);
      planReq.flush(plan);
    });
  });

  describe('update', () => {
    it('should update api product plan', done => {
      const updatePlan = fakeUpdatePlanV4();
      const plan = fakePlanV4({ id: PLAN_ID });

      apiProductPlanService.update(API_PRODUCT_ID, PLAN_ID, updatePlan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`,
      });
      expect(planReq.request.body).toEqual(updatePlan);
      planReq.flush(plan);
    });
  });

  describe('publish', () => {
    it('should publish api product plan', done => {
      const plan = fakePlanV4({ id: PLAN_ID });

      apiProductPlanService.publish(API_PRODUCT_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}/_publish`,
      });
      expect(req.request.body).toEqual({});
      req.flush(plan);
    });
  });

  describe('deprecate', () => {
    it('should deprecate the api product plan', done => {
      const plan = fakePlanV4({ id: PLAN_ID });

      apiProductPlanService.deprecate(API_PRODUCT_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}/_deprecate`,
      });
      expect(req.request.body).toEqual({});
      req.flush(plan);
    });
  });

  describe('close', () => {
    it('should close the api product plan', done => {
      const plan = fakePlanV4({ id: PLAN_ID });

      apiProductPlanService.close(API_PRODUCT_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}/_close`,
      });
      expect(req.request.body).toEqual({});
      req.flush(plan);
    });
  });
});
