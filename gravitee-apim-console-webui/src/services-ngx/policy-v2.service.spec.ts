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

import { PolicyV2Service } from './policy-v2.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakePolicySchema } from '../entities/policy';

describe('PolicyService', () => {
  let httpTestingController: HttpTestingController;
  let policyService: PolicyV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    policyService = TestBed.inject<PolicyV2Service>(PolicyV2Service);
  });

  describe('getSchema', () => {
    it('should call the API', (done) => {
      const policyId = 'policy#1';
      const policySchema = fakePolicySchema();

      policyService.getSchema(policyId).subscribe((response) => {
        expect(response).toStrictEqual(policySchema);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.v2BaseURL}/plugins/policies/policy#1/schema`);
      expect(req.request.method).toEqual('GET');

      req.flush(policySchema);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
