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
class ApiController {
  constructor (ApiService, $stateParams, PolicyService) {
    'ngInject';
    this.ApiService = ApiService;
    this.PolicyService = PolicyService;

    this.apis = [];
    if ($stateParams.apiName) {
      this.get($stateParams.apiName);
      this.listPolicies($stateParams.apiName);
    } else {
      this.list();
    }

    this.selectedPolicy = null;
  }

  get(apiName) {
    this.ApiService.get(apiName).then(response => {
      this.api = response.data;
      this.api.policy = {'Request': this.api.policy};
    });
  }

  list() {
    this.ApiService.list().then(response => {
      this.apis = response.data;
    });
  }

  start(name) {
    this.ApiService.start(name).then(() => {
      this.ApiService.reload(name).then(() => {
        this.list();
      });
    });
  }

  stop(name) {
    this.ApiService.stop(name).then(() => {
      this.ApiService.reload(name).then(() => {
        this.list();
      });
    });
  }

  listPolicies(apiName) {
    this.PolicyService.list(apiName).then(response => {
      // TODO filter request, response and request/response policies
      this.policies = {
        'Request': response.data,
        'Response': [],
        'Request/Response': []
      };
    });
  }
}

export default ApiController;
