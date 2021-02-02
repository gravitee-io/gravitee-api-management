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
import { IHttpBackendService } from 'angular';

import QualityRuleService from './qualityRule.service';
import { QualityRule } from '../entities/qualityRule';

describe('QualityRuleService', () => {
  let QualityRuleService: QualityRuleService;
  let $httpBackend: IHttpBackendService;

  beforeEach(inject((_QualityRuleService_, _$httpBackend_) => {
    QualityRuleService = _QualityRuleService_;
    $httpBackend = _$httpBackend_;
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('list', () => {
    it('returns the data', (done) => {
      const qualityRules: QualityRule[] = [{ name: 'name', description: 'description', weight: 42 }];
      $httpBackend.expectGET('http://url.test/configuration/quality-rules/').respond(qualityRules);

      QualityRuleService.list()
        .then((result) => {
          expect(result.data).toEqual(qualityRules);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });
  });
});
