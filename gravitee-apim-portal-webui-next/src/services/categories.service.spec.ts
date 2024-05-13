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

import { CategoriesService } from './categories.service';
import { Categories } from '../entities/categories/categories';
import { fakeCategoriesResponse } from '../entities/categories/categories.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('CategoriesService', () => {
  let service: CategoriesService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(CategoriesService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should return categories list', done => {
    const categoriesResponse: Categories = fakeCategoriesResponse();

    service.categories().subscribe(response => {
      expect(response).toMatchObject(categoriesResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/categories?size=-1`);
    expect(req.request.method).toEqual('GET');

    req.flush(categoriesResponse);
  });
});
