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

class CategoryService {
  constructor(private $http, private Constants) {
    'ngInject';
  }

  list(include?: string[]) {
    const queryParam = include ? `?include=${include.join(',')}` : '';
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/categories/` + queryParam);
  }

  get(categoryId) {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/categories/` + categoryId);
  }

  create(category) {
    return this.$http.post(`${this.Constants.env.baseURL}/configuration/categories/`, category);
  }

  update(category) {
    return this.$http.put(`${this.Constants.env.baseURL}/configuration/categories/` + category.id, category);
  }

  updateCategories(categories) {
    if (categories && categories.length) {
      return this.$http.put(`${this.Constants.env.baseURL}/configuration/categories/`, categories);
    }
  }

  delete(category) {
    return this.$http.delete(`${this.Constants.env.baseURL}/configuration/categories/` + category.id);
  }
}

export default CategoryService;
