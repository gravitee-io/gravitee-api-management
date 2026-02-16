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

import { ApiPlanV2Service } from './api-plan-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  ApiPlansResponse,
  CreatePlanV4,
  fakePlanV2,
  fakePlanV4,
  fakeUpdatePlanV2,
  fakeUpdatePlanV4,
  PlanMode,
  PlanStatus,
} from '../entities/management-api-v2';

describe('ApiPlanV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiPlanV2Service: ApiPlanV2Service;
  const API_ID = 'api-id';
  const APP_ID = 'app-id';
  const PLAN_ID = 'plan-id';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiPlanV2Service = TestBed.inject<ApiPlanV2Service>(ApiPlanV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', done => {
      const fakeApiPlansResponse: ApiPlansResponse = {
        data: [
          fakePlanV4({
            id: PLAN_ID,
          }),
        ],
      };

      apiPlanV2Service.list(API_ID, undefined, undefined, undefined, undefined).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual([
          fakePlanV4({
            id: PLAN_ID,
          }),
        ]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(fakeApiPlansResponse);
    });
    it('should list with statuses, security and mode', done => {
      const security = ['API_KEY'];
      const statuses: PlanStatus[] = ['STAGING', 'PUBLISHED'];
      const mode: PlanMode = 'STANDARD';

      const fakeApiPlansResponse: ApiPlansResponse = {
        data: [
          fakePlanV4({
            id: PLAN_ID,
          }),
        ],
      };

      apiPlanV2Service.list(API_ID, security, statuses, mode, undefined).subscribe(apiPlansResponse => {
        expect(apiPlansResponse.data).toEqual([
          fakePlanV4({
            id: PLAN_ID,
          }),
        ]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=10&securities=API_KEY&statuses=STAGING,PUBLISHED&mode=STANDARD`,
        method: 'GET',
      });

      req.flush(fakeApiPlansResponse);
    });
  });

  describe('create', () => {
    it('should create api plans', done => {
      const plan: CreatePlanV4 = {
        description: '',
        definitionVersion: 'V4',
        flows: [],
        validation: 'AUTO',
        name: 'free',
        mode: 'STANDARD',
        security: { type: 'API_KEY', configuration: '{}' },
      };

      apiPlanV2Service.create(API_ID, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans`,
      });
      expect(planReq.request.body).toEqual(plan);
      planReq.flush(plan);
    });

    it('should create PUSH plans using SUBSCRIPTION type', done => {
      const plan: CreatePlanV4 = {
        description: '',
        definitionVersion: 'V4',
        flows: [],
        validation: 'AUTO',
        name: 'free',
        mode: 'PUSH',
      };

      apiPlanV2Service.create(API_ID, plan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans`,
      });
      expect(planReq.request.body.security).toBeUndefined();
      expect(planReq.request.body.mode).toEqual('PUSH');
      planReq.flush(plan);
    });
  });

  describe('get', () => {
    it('should get api plans', done => {
      const apiId = 'api-1';
      const planId = 'plan-1';

      apiPlanV2Service.get(apiId, planId).subscribe(response => {
        expect(response).toMatchObject({ id: planId });
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans/${planId}`,
      });
      planReq.flush({ id: planId });
    });
  });

  describe('publish', () => {
    it('should publish api plans', done => {
      const plan = fakePlanV4({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.publish(API_ID, PLAN_ID).subscribe(() => {
        done();
      });

      const publishPlanReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}/_publish`,
      });
      publishPlanReq.flush(plan);
    });
  });

  describe('update', () => {
    it('should update api plans V2', done => {
      const updatePlan = fakeUpdatePlanV2();
      const plan = fakePlanV2({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.update(API_ID, PLAN_ID, updatePlan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}`,
      });
      expect(planReq.request.body).toEqual(updatePlan);
      planReq.flush(plan);
    });

    it('should update api plans V4', done => {
      const updatePlan = fakeUpdatePlanV4();
      const plan = fakePlanV4({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.update(API_ID, PLAN_ID, updatePlan).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}`,
      });
      expect(planReq.request.body).toEqual(updatePlan);
      planReq.flush(plan);
    });
  });

  describe('get', () => {
    it('should get the api plan', done => {
      const plan = fakePlanV2({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.get(API_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}`,
        })
        .flush(plan);
    });
  });

  describe('deprecate', () => {
    it('should deprecate the api plan V2', done => {
      const plan = fakePlanV2({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.deprecate(API_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}/_deprecate`,
      });

      expect(req.request.body).toEqual({});
      req.flush(plan);
    });

    it('should deprecate the api plan V4', done => {
      const plan = fakePlanV4({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.deprecate(API_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}/_deprecate`,
      });

      expect(req.request.body).toEqual({});
      req.flush(plan);
    });
  });

  describe('close', () => {
    it('should close the api plan V2', done => {
      const plan = fakePlanV2({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.close(API_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}/_close`,
      });

      expect(req.request.body).toEqual({});
      req.flush(plan);
    });

    it('should close the api plan V4', done => {
      const plan = fakePlanV4({ id: PLAN_ID, apiId: API_ID });

      apiPlanV2Service.close(API_ID, PLAN_ID).subscribe(response => {
        expect(response).toMatchObject(plan);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}/_close`,
      });

      expect(req.request.body).toEqual({});
      req.flush(plan);
    });
  });

  describe('listSubscribablePlans', () => {
    it('should list subscribable plan for the api and application', done => {
      const fakeApiPlansResponse: ApiPlansResponse = { data: [fakePlanV4({ id: PLAN_ID, apiId: API_ID })] };

      apiPlanV2Service.listSubscribablePlans(API_ID, APP_ID).subscribe(response => {
        expect(response).toMatchObject(fakeApiPlansResponse);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999&subscribableBy=${APP_ID}`,
        })
        .flush(fakeApiPlansResponse);
    });
  });
});
