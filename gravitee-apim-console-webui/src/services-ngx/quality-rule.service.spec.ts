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

import { QualityRuleService } from './quality-rule.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { QualityRule } from '../entities/qualityRule';

describe('QualityRuleService', () => {
  let httpTestingController: HttpTestingController;
  let qualityRuleService: QualityRuleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    qualityRuleService = TestBed.inject<QualityRuleService>(QualityRuleService);
  });

  describe('list', () => {
    it('should call the API', done => {
      const qualityRules: QualityRule[] = [{ name: 'name', description: 'description', weight: 42 }];

      qualityRuleService.list().subscribe(portalSettings => {
        expect(portalSettings).toMatchObject(qualityRules);
        done();
      });

      httpTestingController
        .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules` })
        .flush(qualityRules);
    });
  });

  describe('add quality rule', () => {
    it('should call the API', done => {
      const qualityRule: QualityRule = { name: 'name', description: 'description', weight: 42 };

      qualityRuleService.add(qualityRule).subscribe(newQualityRule => {
        expect(newQualityRule).toMatchObject(qualityRule);
        done();
      });

      httpTestingController
        .expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules` })
        .flush(qualityRule);
    });
  });

  describe('update quality rule', () => {
    it('should call the API', done => {
      const updatedQualityRule: QualityRule = { name: 'name2', description: 'description2', weight: 42 };

      qualityRuleService.update('1', updatedQualityRule).subscribe(selectedQualityRule => {
        expect(selectedQualityRule).toMatchObject(updatedQualityRule);
        done();
      });

      httpTestingController
        .expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules/1` })
        .flush(updatedQualityRule);
    });
  });

  describe('delete quality rule', () => {
    it('should call the API', done => {
      const qualityRuleId = '2';

      qualityRuleService.delete(qualityRuleId).subscribe(() => done());

      httpTestingController
        .expectOne({ method: 'DELETE', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules/2` })
        .flush(qualityRuleId);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
