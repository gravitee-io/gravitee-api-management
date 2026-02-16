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

import { CategoryV2Service } from './category-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakePagedResult } from '../entities/management-api-v2';
import { CategoryApi } from '../entities/management-api-v2/category/categoryApi';
import { fakeCategoryApi } from '../entities/management-api-v2/category/categoryApi.fixture';
import { UpdateCategoryApi } from '../entities/management-api-v2/category/updateCategoryApi';

describe('CategoryV2Service', () => {
  let httpTestingController: HttpTestingController;
  let categoryV2Service: CategoryV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    categoryV2Service = TestBed.inject<CategoryV2Service>(CategoryV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getApis', () => {
    it('should call the API', done => {
      const categoryId = 'categoryId';
      const response = fakePagedResult<CategoryApi[]>([fakeCategoryApi()]);

      categoryV2Service.getApis(categoryId).subscribe(result => {
        expect(result).toMatchObject(response);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/categories/${categoryId}/apis?perPage=9999`,
        })
        .flush(response);
    });
  });

  describe('updateCategoryApi', () => {
    it('should call the API', done => {
      const categoryId = 'categoryId';
      const apiId = 'apiId';
      const requestBody: UpdateCategoryApi = { order: 1 };
      const responseBody: CategoryApi = fakeCategoryApi();

      categoryV2Service.updateCategoryApi(categoryId, apiId, requestBody).subscribe(result => {
        expect(result).toMatchObject(responseBody);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/categories/${categoryId}/apis/${apiId}`,
      });
      expect(req.request.body).toEqual(requestBody);
      req.flush(responseBody);
    });
  });
});
