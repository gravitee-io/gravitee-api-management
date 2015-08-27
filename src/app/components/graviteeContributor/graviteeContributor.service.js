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
class GraviteeContributorService {
  constructor ($log, $http) {
    'ngInject';

    this.$http = $http;
    this.$log = $log;
    this.apiHost = 'https://api.github.com/repos/gravitee-io/gravitee-management-webui';
  }

  getContributors(limit) {
    if (!limit) {
      limit = 30;
    }

    return this.$http.get(this.apiHost + '/contributors?per_page=' + limit)
      .then((response) => {
        return response.data;
      })
      .catch((error) => {
        this.$log.error('XHR Failed for getContributors.\n' + angular.toJson(error.data, true));
      });
  }
}

export default GraviteeContributorService;
