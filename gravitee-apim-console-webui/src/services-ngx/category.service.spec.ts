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

import { CategoryService } from './category.service';

import { GioTestingModule } from '../shared/testing';
import { Category } from '../entities/category/Category';

describe('CategoryService', () => {
  let httpTestingController: HttpTestingController;
  let categoryService: CategoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    categoryService = TestBed.inject<CategoryService>(CategoryService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', done => {
      const response: Category[] = [{ id: 'Id', name: 'Fox', key: 'fox' }];

      categoryService.list().subscribe(result => {
        expect(result).toMatchObject(response);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/configuration/categories`,
        })
        .flush(response);
    });
  });
  describe('update', () => {
    it('should call the API', done => {
      const requestBody: Category = { id: 'Id', name: 'Fox', key: 'fox' };
      const responseBody: Category = { id: 'Id', name: 'Fox 2', key: 'fox 2' };

      categoryService.update(requestBody).subscribe(result => {
        expect(result).toMatchObject(responseBody);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/configuration/categories/${requestBody.id}`,
      });
      expect(req.request.body).toEqual(requestBody);
      req.flush(responseBody);
    });
  });
  describe('updateList', () => {
    it('should call the API', done => {
      const requestBody: Category[] = [{ id: 'Id', name: 'Fox', key: 'fox' }];
      const responseBody: Category[] = [{ id: 'Id', name: 'Fox 2', key: 'fox 2' }];

      categoryService.updateList(requestBody).subscribe(result => {
        expect(result).toMatchObject(responseBody);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/configuration/categories`,
      });
      expect(req.request.body).toEqual(requestBody);
      req.flush(responseBody);
    });
  });
  describe('delete', () => {
    it('should call the API', done => {
      const categoryId = 'cat-id';
      categoryService.delete(categoryId).subscribe(result => {
        expect(result).toMatchObject({});
        done();
      });

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/configuration/categories/${categoryId}`,
        })
        .flush({});
    });
  });
});
