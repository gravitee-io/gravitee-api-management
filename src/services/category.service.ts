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
  private categoriesURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.categoriesURL = `${Constants.envBaseURL}/configuration/categories/`;
  }

  list() {
    return this.$http.get(this.categoriesURL);
  }

  get(categoryId) {
    return this.$http.get(this.categoriesURL + categoryId);
  }

  create(category) {
    return this.$http.post(this.categoriesURL, category);
  }

  update(category) {
    return this.$http.put(this.categoriesURL + category.id, category);
  }

  updateCategories(categories) {
    if (categories && categories.length) {
      return this.$http.put(this.categoriesURL, categories);
    }
  }

  delete(category) {
    return this.$http.delete(this.categoriesURL + category.id);
  }
}

export default CategoryService;
