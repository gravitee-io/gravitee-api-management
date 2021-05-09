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
class PolicyService {
  constructor(private $http, private Constants) {
    'ngInject';
  }

  list(expandSchema = false, expandIcon = false, withoutResource = false) {
    const expandParams = [];
    if (expandSchema) {
      expandParams.push('expand=schema');
    }
    if (expandIcon) {
      expandParams.push('expand=icon');
    }
    if (withoutResource) {
      expandParams.push('withResource=false');
    }
    let url = `${this.Constants.env.baseURL}/policies/`;
    if (expandParams.length > 0) {
      url += `?${expandParams.join('&')}`;
    }
    return this.$http.get(url);
  }

  listSwaggerPolicies() {
    return this.$http.get(`${this.Constants.env.baseURL}/policies/swagger`);
  }

  getSchema(policyId) {
    return this.$http.get(`${this.Constants.env.baseURL}/policies/${policyId}/schema`);
  }

  getDocumentation(policyId) {
    return this.$http.get(`${this.Constants.env.baseURL}/policies/${policyId}/documentation`);
  }
}

export default PolicyService;
