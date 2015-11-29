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
class ApiMonitoringController {
  constructor (ApiService, resolvedApi, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;
  }

  changeMonitoringState(id) {
    console.log('cool: ' + id);

  }

  update(api) {
    var _this = this;
    this.ApiService.update(api).then((updatedApi) => {
      _this.api = updatedApi.data;
    //  this.initState();
      _this.formApiMonitoring.$setPristine();
    });
  }
}

export default ApiMonitoringController;
