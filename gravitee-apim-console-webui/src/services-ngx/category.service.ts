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

import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Category } from '../entities/category/Category';
import { Constants } from '../entities/Constants';
import { NewCategory } from '../entities/category/NewCategory';
import { UpdateCategory } from '../entities/category/UpdateCategory';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public list(includeTotalApis = false): Observable<Category[]> {
    return this.http.get<Category[]>(
      `${this.constants.env.baseURL}/configuration/categories${includeTotalApis ? '?include=total-apis' : ''}`,
    );
  }

  public update(category: UpdateCategory): Observable<Category> {
    return this.http.put<Category>(`${this.constants.env.baseURL}/configuration/categories/${category.id}`, category);
  }

  public updateList(categoryList: Category[]): Observable<Category[]> {
    return this.http.put<Category[]>(`${this.constants.env.baseURL}/configuration/categories`, categoryList);
  }

  public delete(categoryId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/configuration/categories/${categoryId}`);
  }

  public get(categoryId: string): Observable<Category> {
    return this.http.get<Category>(`${this.constants.env.baseURL}/configuration/categories/${categoryId}`);
  }

  public create(newCategory: NewCategory): Observable<Category> {
    return this.http.post<Category>(`${this.constants.env.baseURL}/configuration/categories`, newCategory);
  }
}
