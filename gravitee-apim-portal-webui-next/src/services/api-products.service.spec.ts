/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { ApiProductsService } from './api-products.service';
import { fakeApiProduct } from '../entities/api-product/api-product.fixture';
import { fakePlan } from '../entities/plan/plan.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ApiProductsService', () => {
  let service: ApiProductsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });

    service = TestBed.inject(ApiProductsService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getById', () => {
    it('should get API Product details', done => {
      const apiProduct = fakeApiProduct();

      service.getById(apiProduct.id).subscribe(response => {
        expect(response).toEqual(apiProduct);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/api-products/${apiProduct.id}`);
      expect(req.request.method).toEqual('GET');
      expect(req.request.params.keys()).toHaveLength(0);
      req.flush(apiProduct);
    });

    it('should get classic API Product details with a null kind', done => {
      const apiProduct = fakeApiProduct({ kind: null });

      service.getById(apiProduct.id).subscribe(response => {
        expect(response.kind).toBeNull();
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/api-products/${apiProduct.id}`);
      expect(req.request.method).toEqual('GET');
      req.flush(apiProduct);
    });

    it('should propagate an API Product not found error', done => {
      const apiProductId = '4f6597ca-74b8-4e68-a597-ca74b83e6824';

      service.getById(apiProductId).subscribe({
        next: () => fail(),
        error: error => {
          expect(error.status).toEqual(404);
          done();
        },
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/api-products/${apiProductId}`);
      req.flush({ message: 'API Product not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('listPlans', () => {
    it('should list all available API Product plans', done => {
      const apiProductId = '4f6597ca-74b8-4e68-a597-ca74b83e6824';
      const plansResponse = { data: [fakePlan()] };

      service.listPlans(apiProductId).subscribe(response => {
        expect(response).toEqual(plansResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/api-products/${apiProductId}/plans?size=-1`);
      expect(req.request.method).toEqual('GET');
      req.flush(plansResponse);
    });
  });
});
