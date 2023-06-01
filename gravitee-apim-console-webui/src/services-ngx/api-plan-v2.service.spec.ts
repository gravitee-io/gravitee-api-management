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

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { ApiPlansResponse, fakePlanV4, CreatePlanV4 } from '../entities/management-api-v2';

describe('ApiPlanV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiPlanV2Service: ApiPlanV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiPlanV2Service = TestBed.inject<ApiPlanV2Service>(ApiPlanV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', (done) => {
      const apiId = 'api-id';

      const fakeApiPlansResponse: ApiPlansResponse = {
        data: [
          fakePlanV4({
            id: 'plan-id',
          }),
        ],
      };

      apiPlanV2Service.list(apiId).subscribe((apiPlansResponse) => {
        expect(apiPlansResponse.data).toEqual([
          fakePlanV4({
            id: 'plan-id',
          }),
        ]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(fakeApiPlansResponse);
    });
  });

  describe('create', () => {
    it('should create api plans', (done) => {
      const apiId = 'fox';
      const plan: CreatePlanV4 = {
        description: '',
        definitionVersion: 'V4',
        flows: [],
        validation: 'AUTO',
        name: 'free',
        security: { type: 'API_KEY', configuration: '{}' },
      };

      apiPlanV2Service.create(apiId, plan).subscribe((response) => {
        expect(response).toMatchObject(plan);
        done();
      });

      const planReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans`,
      });
      expect(planReq.request.body).toEqual(plan);
      planReq.flush(plan);
    });
  });

  describe('publish', () => {
    it('should publish api plans', (done) => {
      const apiId = 'api-1';
      const planId = 'plan-1';

      apiPlanV2Service.publish(apiId, planId).subscribe(() => {
        done();
      });

      const publishPlanReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans/${planId}/_publish`,
      });
      publishPlanReq.flush({});
    });
  });
});
