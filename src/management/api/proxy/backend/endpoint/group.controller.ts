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
  private discovery: any;
  private creation: boolean = false;

  private serviceDiscoveryJsonSchemaForm: string[];
  private types: any[];
  private serviceDiscoveryJsonSchema: any;
  private serviceDiscoveryConfigurationForm: any;

  constructor (
    private ApiService,
    private NotificationService,
    private ServiceDiscoveryService,
    private $scope,
    private $rootScope,
    private resolvedServicesDiscovery,
    private $state,
    private $stateParams,
    private $timeout
  ) {
    'ngInject';
  }

  $onInit() {
    this.api = this.$scope.$parent.apiCtrl.api;
    this.group = _.find(this.api.proxy.groups, { 'name': this.$stateParams.groupName});

    // Creation mode
    if (!this.group) {
      this.group = {};
      this.creation = true;
    }

    this.serviceDiscoveryJsonSchemaForm = ['*'];

    this.types = this.resolvedServicesDiscovery.data;

    this.discovery = this.group.services && this.group.services['discovery'];
    this.discovery = this.discovery || {enabled: false, configuration: {}};
    this.initialGroups = _.cloneDeep(this.api.proxy.groups);

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

    if (!this.group.load_balancing) {
      this.group.load_balancing = {
        type: this.$scope.lbs[0].value
      }
    }

    this.retrievePluginSchema();
  }

  onTypeChange() {
    this.discovery.configuration = {};

    this.retrievePluginSchema();
  }

  retrievePluginSchema() {
    if (this.discovery.provider !== undefined) {
      this.ServiceDiscoveryService.getSchema(this.discovery.provider).then(({data}) => {
          this.serviceDiscoveryJsonSchema = data;
        },
        (response) => {
          if (response.status === 404) {
            this.serviceDiscoveryJsonSchema = {};
            return {
              schema: {}
            };
          } else {
            //todo manage errors
            this.NotificationService.showError('Unexpected error while loading service discovery schema for ' + this.discovery.provider);
          }
        });
    }
  }

  update(api) {
    // include discovery service
    this.group.services = {
      discovery: this.discovery
    };

    if (!_.includes(api.proxy.groups, this.group)) {
      if (!api.proxy.groups) {
        api.proxy.groups = [this.group];
      } else {
        api.proxy.groups.push(this.group);
      }
    }
    if (this.group.ssl.trustAll) {
      delete this.group.ssl.trustStore;
    }
    if (this.group.ssl.trustStore && (!this.group.ssl.trustStore.type || this.group.ssl.trustStore.type === '')) {
      delete this.group.ssl.trustStore;
    }
    if (this.group.ssl.keyStore && (!this.group.ssl.keyStore.type || this.group.ssl.keyStore.type === '')) {
      delete this.group.ssl.keyStore;
    }
    if (this.group.headers.length > 0) {
      this.group.headers = _.mapValues(_.keyBy(this.group.headers, 'name'), 'value');
    } else {
      delete this.group.headers;
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
    this.NotificationService.show('Group configuration saved');
    this.$state.go('management.apis.detail.proxy.endpoints');
  }

  reset() {
    this.$state.reload();
  }

  backToEndpointsConfiguration() {
    this.api.proxy.groups = _.cloneDeep(this.initialGroups);
    this.$state.go('management.apis.detail.proxy.endpoints');
  }
}

export default ApiEndpointGroupController;
