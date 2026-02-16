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

import { PolicyService } from './policy.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePolicyListItem, fakePolicySchema, fakePolicyDocumentation } from '../entities/policy';

describe('PolicyService', () => {
  let httpTestingController: HttpTestingController;
  let policyService: PolicyService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    policyService = TestBed.inject<PolicyService>(PolicyService);
  });

  describe('list', () => {
    it('should call the API with params to true', done => {
      const policies = [fakePolicyListItem()];

      policyService
        .list({
          expandSchema: true,
          expandIcon: true,
          withoutResource: true,
        })
        .subscribe(response => {
          expect(response).toStrictEqual(policies);
          done();
        });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=schema&expand=icon&withResource=false`);
      expect(req.request.method).toEqual('GET');

      req.flush(policies);
    });

    it('should call the API with different params', done => {
      const policies = [fakePolicyListItem()];

      policyService
        .list({
          expandSchema: false,
          expandIcon: true,
        })
        .subscribe(response => {
          expect(response).toStrictEqual(policies);
          done();
        });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=icon`);
      expect(req.request.method).toEqual('GET');

      req.flush(policies);
    });
  });

  describe('listSwaggerPolicies', () => {
    it('should call the API', done => {
      const policies = [fakePolicyListItem()];

      policyService.listSwaggerPolicies().subscribe(response => {
        expect(response).toStrictEqual(policies);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies/swagger`);
      expect(req.request.method).toEqual('GET');

      req.flush(policies);
    });
  });

  describe('getSchema', () => {
    it('should call the API', done => {
      const policyId = 'policy#1';
      const policySchema = fakePolicySchema();

      policyService.getSchema(policyId).subscribe(response => {
        expect(response).toStrictEqual(policySchema);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies/policy#1/schema`);
      expect(req.request.method).toEqual('GET');

      req.flush(policySchema);
    });
  });

  describe('getDocumentation', () => {
    it('should call the API', done => {
      const policyId = 'policy#1';
      const policySchema = fakePolicyDocumentation().content;

      policyService.getDocumentation(policyId).subscribe(response => {
        expect(response).toStrictEqual(policySchema);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies/policy#1/documentation`);
      expect(req.request.method).toEqual('GET');

      req.flush(policySchema);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
