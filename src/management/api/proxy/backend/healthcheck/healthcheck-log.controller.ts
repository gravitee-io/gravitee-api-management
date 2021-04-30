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

class ApiHealthCheckLogController {
  private api: any;
  private log: any;

  constructor(private $scope, private resolvedLog, private $state, private $window) {
    'ngInject';
    this.api = this.$scope.$parent.apiCtrl.api;
    this.log = resolvedLog.data;
  }

  getMimeType(log) {
    if (log.headers !== undefined && log.headers['Content-Type'] !== undefined) {
      const contentType = log.headers['Content-Type'][0];
      return contentType.split(';', 1)[0];
    }

    return null;
  }

  backToHealthcheck() {
    const query = JSON.parse(this.$window.localStorage.lastHealthCheckQuery);
    this.$state.go('management.apis.detail.proxy.healthcheck.visualize', {
      page: query.page,
      size: query.size,
      from: query.from,
      to: query.to,
    });
  }
}

export default ApiHealthCheckLogController;
