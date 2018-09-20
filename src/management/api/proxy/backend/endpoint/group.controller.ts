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

import _ = require('lodash');

class ApiEndpointGroupController {
  private api: any;
  private group: any;
  private initialGroups: any;
  private initialGroup: any;

  private creation: boolean = false;

  constructor (
    private ApiService,
    private NotificationService,
    private $scope,
    private $rootScope,
    private $state,
    private $stateParams
  ) {
    'ngInject';

    this.api = this.$scope.$parent.apiCtrl.api;
    this.group = _.find(this.api.proxy.groups, { 'name': $stateParams.groupName});

    this.initialGroups = _.cloneDeep(this.api.proxy.groups);

    // Creation mode
    if (!this.group) {
      this.group = {};
      this.creation = true;
    }

    // Keep the initial state in case of form reset
    this.initialGroup = _.cloneDeep(this.group);

    this.$scope.lbs = [
      {
        name: 'Round-Robin',
        value: 'ROUND_ROBIN'
      }, {
        name: 'Random',
        value: 'RANDOM'
      }, {
        name: 'Weighted Round-Robin',
        value: 'WEIGHTED_ROUND_ROBIN'
      }, {
        name: 'Weighted Random',
        value: 'WEIGHTED_RANDOM'
      }];
  }

  update(api) {
    if (!_.includes(api.proxy.groups, this.group)) {
      if (!api.proxy.groups) {
        api.proxy.groups = [this.group];
      } else {
        api.proxy.groups.push(this.group);
      }
    }

    this.ApiService.update(api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.onApiUpdate();
      this.initialGroups = _.cloneDeep(api.proxy.groups);
    });
  }

  onApiUpdate() {
    this.$rootScope.$broadcast('apiChangeSuccess', {api: this.api});
    this.NotificationService.show('Endpoint saved');
    this.$state.go('management.apis.detail.proxy.endpoints');
  }

  reset() {
    this.$scope.formGroup.$setPristine();
    this.group = _.cloneDeep(this.initialGroup);
  }

  backToEndpointsConfiguration() {
    this.api.proxy.groups = _.cloneDeep(this.initialGroups);
    this.$state.go('management.apis.detail.proxy.endpoints');
  }
}

export default ApiEndpointGroupController;
