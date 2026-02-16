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

import { ApiQualityRuleService } from './api-quality-rule.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('ApiQualityRuleService', () => {
  let httpTestingController: HttpTestingController;
  let apiQualityRuleService: ApiQualityRuleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiQualityRuleService = TestBed.inject<ApiQualityRuleService>(ApiQualityRuleService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getQualityRules', () => {
    it('should call the API', done => {
      const apiId = 'api#1';

      apiQualityRuleService.getQualityRules(apiId).subscribe(response => {
        expect(response).toEqual([
          {
            id: 'api#1',
          },
        ]);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/quality-rules`);
      expect(req.request.method).toEqual('GET');

      req.flush([
        {
          id: 'api#1',
        },
      ]);
    });
  });

  describe('createQualityRule', () => {
    it('should call the API', done => {
      const apiId = 'api#1';
      const qualityRuleId = 'qualityRule#1';

      apiQualityRuleService.createQualityRule(apiId, qualityRuleId, true).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/quality-rules`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        api: apiId,
        quality_rule: qualityRuleId,
        checked: true,
      });

      req.flush({});
    });
  });

  describe('updateQualityRule', () => {
    it('should call the API', done => {
      const apiId = 'api#1';
      const qualityRuleId = 'qualityRule#1';

      apiQualityRuleService.updateQualityRule(apiId, qualityRuleId, false).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/quality-rules/${qualityRuleId}`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual({
        checked: false,
      });

      req.flush({});
    });
  });
});
