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
class ResourceService {
  private resourcesURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.resourcesURL = `${Constants.env.baseURL}/resources/`;
  }

  list(expandSchema = false, expandIcon = false) {
    const expandParams = [];
    if (expandSchema) {
      expandParams.push('expand=schema');
    }
    if (expandIcon) {
      expandParams.push('expand=icon');
    }
    let url = this.resourcesURL;
    if (expandParams.length > 0) {
      url += `?${expandParams.join('&')}`;
    }
    return this.$http.get(url);
  }

  getSchema(resourceId) {
    return this.$http.get(`${this.resourcesURL + resourceId}/schema`);
  }

  getDocumentation(resourceId) {
    return this.$http.get(this.resourcesURL + resourceId + '/documentation');
  }
}

export default ResourceService;
