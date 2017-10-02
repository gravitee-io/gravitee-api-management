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

  constructor (
    private $scope,
    private resolvedLog,
  ) {
    'ngInject';
    this.api = this.$scope.$parent.apiCtrl.api;
    this.log = resolvedLog.data;
  }

  getMimeType(log) {
    if (log.headers !== undefined && log.headers['Content-Type'] !== undefined) {
      let contentType = log.headers['Content-Type'][0];
      return contentType.split(';', 1)[0];
    }

    return null;
  };
}

export default ApiHealthCheckLogController;
