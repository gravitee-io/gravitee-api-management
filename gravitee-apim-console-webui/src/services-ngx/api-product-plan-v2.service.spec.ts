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

import { ApiProductPlanV2Service } from './api-product-plan-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { ApiPlansResponse, CreatePlanV4, fakePlanV4, fakeUpdatePlanV4, PlanMode, PlanStatus } from '../entities/management-api-v2';

describe('ApiProductPlanV2Service', () => {
  const API_PRODUCT_ID = 'product-svc-test';
  const PLAN_ID = 'plan-svc-test';

  let service: ApiProductPlanV2Service;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    service = TestBed.inject(ApiProductPlanV2Service);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('returns plan list with default page 1 and perPage 10', done => {
      const response: ApiPlansResponse = { data: [fakePlanV4({ id: PLAN_ID })] };

      service.list(API_PRODUCT_ID, undefined, undefined, undefined, undefined).subscribe(result => {
        expect(result).toEqual(response);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=10`,
        })
        .flush(response);
    });

    it('includes statuses securities and mode in list URL when provided', done => {
      const statuses: PlanStatus[] = ['STAGING', 'PUBLISHED'];
      const securities = ['API_KEY', 'JWT'];
      const mode: PlanMode = 'STANDARD';
      const response: ApiPlansResponse = { data: [] };

      service.list(API_PRODUCT_ID, securities, statuses, mode, undefined).subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=10&securities=API_KEY,JWT&statuses=STAGING,PUBLISHED&mode=STANDARD`,
        })
        .flush(response);
    });

    it('includes fields in list URL when provided', done => {
      const response: ApiPlansResponse = { data: [] };

      service.list(API_PRODUCT_ID, undefined, undefined, undefined, ['-flow']).subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans?page=1&perPage=10&fields=-flow`,
        })
        .flush(response);
    });
  });

  describe('create', () => {
    it('POSTs plan to api-products plans URL and returns created plan', done => {
      const plan: CreatePlanV4 = {
        name: 'API Key Plan',
        definitionVersion: 'V4',
        mode: 'STANDARD',
        flows: [],
        validation: 'AUTO',
        security: { type: 'API_KEY', configuration: {} },
      };

      service.create(API_PRODUCT_ID, plan).subscribe(result => {
        expect(result).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans`,
      });
      expect(req.request.body).toEqual(plan);
      req.flush(plan);
    });
  });

  describe('get', () => {
    it('GETs plan by id from api-products plan URL', done => {
      const plan = fakePlanV4({ id: PLAN_ID });

      service.get(API_PRODUCT_ID, PLAN_ID).subscribe(result => {
        expect(result).toMatchObject({ id: PLAN_ID });
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

  describe('update', () => {
    it('PUTs plan to api-products plan URL and returns updated plan', done => {
      const updatePlan = fakeUpdatePlanV4();
      const plan = fakePlanV4({ id: PLAN_ID });

      service.update(API_PRODUCT_ID, PLAN_ID, updatePlan).subscribe(result => {
        expect(result).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`,
      });
      expect(req.request.body).toEqual(updatePlan);
      req.flush(plan);
    });
  });

  describe('publish', () => {
    it('POSTs to _publish endpoint with empty body', done => {
      const plan = fakePlanV4({ id: PLAN_ID, status: 'PUBLISHED' });

      service.publish(API_PRODUCT_ID, PLAN_ID).subscribe(result => {
        expect(result).toMatchObject({ id: PLAN_ID });
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
    it('POSTs to _deprecate endpoint with empty body', done => {
      const plan = fakePlanV4({ id: PLAN_ID, status: 'DEPRECATED' });

      service.deprecate(API_PRODUCT_ID, PLAN_ID).subscribe(result => {
        expect(result).toMatchObject({ id: PLAN_ID });
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
    it('POSTs to _close endpoint with empty body', done => {
      const plan = fakePlanV4({ id: PLAN_ID, status: 'CLOSED' });

      service.close(API_PRODUCT_ID, PLAN_ID).subscribe(result => {
        expect(result).toMatchObject({ id: PLAN_ID });
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
