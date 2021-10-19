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
class EntrypointService {
  constructor(private $http, private Constants) {
    'ngInject';
  }

  findById(entrypoint) {
    return this.$http.get(`${this.Constants.org.baseURL}/configuration/entrypoints/` + entrypoint);
  }

  list() {
    return this.$http.get(`${this.Constants.org.baseURL}/configuration/entrypoints/`);
  }

  create(entrypoint) {
    if (entrypoint) {
      return this.$http.post(`${this.Constants.org.baseURL}/configuration/entrypoints/`, entrypoint);
    }
  }

  update(entrypoint) {
    if (entrypoint) {
      return this.$http.put(`${this.Constants.org.baseURL}/configuration/entrypoints/`, entrypoint);
    }
  }

  delete(entrypoint) {
    if (entrypoint) {
      return this.$http.delete(`${this.Constants.org.baseURL}/configuration/entrypoints/` + entrypoint.id);
    }
  }
}

export default EntrypointService;
