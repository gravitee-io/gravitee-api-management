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

import { PortalCategoryService } from './portal-category.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  CreatePortalCategory,
  fakeCreatePortalCategory,
  fakePortalCategory,
  fakeUpdatePortalCategory,
  PortalCategory,
  UpdatePortalCategory,
} from '../entities/management-api-v2';

describe('PortalCategoryService', () => {
  let httpTestingController: HttpTestingController;
  let service: PortalCategoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<PortalCategoryService>(PortalCategoryService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', done => {
      service.list().subscribe(categories => {
        expect(categories.length).toEqual(1);
        done();
      });

      expectListPortalCategoriesRequest(httpTestingController, [fakePortalCategory()]);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const createPortalCategory = fakeCreatePortalCategory();
      service.create(createPortalCategory).subscribe(category => {
        expect(category).toBeTruthy();
        done();
      });

      expectCreatePortalCategoryRequest(httpTestingController, createPortalCategory);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const updatePortalCategory = fakeUpdatePortalCategory();
      service.update('categoryId', updatePortalCategory).subscribe(category => {
        expect(category).toBeTruthy();
        done();
      });

      expectUpdatePortalCategoryRequest(httpTestingController, 'categoryId', updatePortalCategory);
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      service.delete('categoryId').subscribe(() => {
        done();
      });
      expectDeletePortalCategoryRequest(httpTestingController, 'categoryId');
    });
  });
});

export const expectListPortalCategoriesRequest = (
  httpTestingController: HttpTestingController,
  portalCategories: PortalCategory[] = [fakePortalCategory()],
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/portal-categories`);
  expect(req.request.method).toEqual('GET');
  req.flush(portalCategories);
};

export const expectCreatePortalCategoryRequest = (
  httpTestingController: HttpTestingController,
  expectedCreatePortalCategory: CreatePortalCategory,
  portalCategoryCreated: PortalCategory = fakePortalCategory(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/portal-categories`);
  expect(req.request.method).toEqual('POST');
  expect(req.request.body).toStrictEqual(expectedCreatePortalCategory);
  req.flush(portalCategoryCreated);
};

export const expectUpdatePortalCategoryRequest = (
  httpTestingController: HttpTestingController,
  portalCategoryId: string,
  expectedUpdatePortalCategory: UpdatePortalCategory,
  portalCategoryUpdated: PortalCategory = fakePortalCategory(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/portal-categories/${portalCategoryId}`);
  expect(req.request.method).toEqual('PUT');
  expect(req.request.body).toStrictEqual(expectedUpdatePortalCategory);
  req.flush(portalCategoryUpdated);
};

export const expectDeletePortalCategoryRequest = (httpTestingController: HttpTestingController, portalCategoryId: string) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/portal-categories/${portalCategoryId}`);
  expect(req.request.method).toEqual('DELETE');
  req.flush({});
};
