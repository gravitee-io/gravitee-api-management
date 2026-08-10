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

import { PortalCategoriesService } from './portal-categories.service';
import { fakePortalCategory } from '../entities/categories/portal-category.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('PortalCategoriesService', () => {
  let service: PortalCategoriesService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PortalCategoriesService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should return the list of visible portal categories', done => {
    const categories = [fakePortalCategory(), fakePortalCategory({ id: 'category-2', title: 'Category 2' })];

    service.getCategories().subscribe(response => {
      expect(response).toEqual(categories);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-categories`);
    expect(req.request.method).toEqual('GET');

    req.flush({ data: categories });
  });

  it('should return an empty array when data is missing', done => {
    service.getCategories().subscribe(response => {
      expect(response).toEqual([]);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-categories`);
    req.flush({});
  });
});
