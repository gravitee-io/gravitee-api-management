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

import { StateParams, StateService } from '@uirouter/core';
import * as _ from 'lodash';

import { ApiService } from '../../../../../services/api.service';
import NotificationService from '../../../../../services/notification.service';

class ApiEndpointGroupController {
  private api: any;
  private group: any;
  private initialGroups: any;
  private discovery: any;
  private creation = false;

  private serviceDiscoveryJsonSchemaForm: string[];
  private types: any[];
  private serviceDiscoveryJsonSchema: any;
  private schema: any;

  constructor(
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private ServiceDiscoveryService,
    private $scope,
    private $rootScope: ng.IRootScopeService,
    private resolvedServicesDiscovery,
    private resolvedConnectors,
    private $state: StateService,
    private $stateParams: StateParams,
  ) {
    'ngInject';
  }

  $onInit() {
    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.group = _.find(this.api.proxy.groups, { name: this.$stateParams.groupName });
    this.$scope.duplicateEndpointNames = false;
    this.schema = this.resolvedConnectors.data.find((connector) => connector.supportedTypes.includes('http'))?.schema;
    // Creation mode
    if (!this.group) {
      this.group = {
        type: 'http',
      };
      this.api.proxy.groups.push(this.group);
    }

    this.serviceDiscoveryJsonSchemaForm = ['*'];

    this.types = this.resolvedServicesDiscovery.data;

    this.discovery = this.group.services && this.group.services.discovery;
    this.discovery = this.discovery || { enabled: false, configuration: {} };
    this.initialGroups = _.cloneDeep(this.api.proxy.groups);

    this.$scope.lbs = [
      {
        name: 'Round-Robin',
        value: 'ROUND_ROBIN',
      },
      {
        name: 'Random',
        value: 'RANDOM',
      },
      {
        name: 'Weighted Round-Robin',
        value: 'WEIGHTED_ROUND_ROBIN',
      },
      {
        name: 'Weighted Random',
        value: 'WEIGHTED_RANDOM',
      },
    ];

    if (!this.group.load_balancing) {
      this.group.load_balancing = {
        type: this.$scope.lbs[0].value,
      };
    }

    this.retrievePluginSchema();
  }

  onTypeChange() {
    this.discovery.configuration = {};
    this.retrievePluginSchema();
  }

  retrievePluginSchema() {
    if (this.discovery.provider !== undefined) {
      this.ServiceDiscoveryService.getSchema(this.discovery.provider).then(
        ({ data }) => {
          this.serviceDiscoveryJsonSchema = data;
        },
        (response) => {
          if (response.status === 404) {
            this.serviceDiscoveryJsonSchema = {};
            return {
              schema: {},
            };
          } else {
            // todo manage errors
            this.NotificationService.showError('Unexpected error while loading service discovery schema for ' + this.discovery.provider);
          }
        },
      );
    }
  }

  updateConfiguration(event) {
    const isValid = !event.detail?.validation?.errors?.length;
    if (isValid) {
      // Abstract endpoint properties to keep
      const { name, load_balancing, discovery, endpoints } = this.group;
      // Delete all properties, important if a property is deleted by the user with the form
      for (const prop of Object.getOwnPropertyNames(this.group)) {
        delete this.group[prop];
      }
      // Reassign all desired properties and keep reference
      Object.assign(this.group, { name, load_balancing, discovery, endpoints }, event.detail.values);
    }
    this.$scope.formGroup.$setDirty();
    this.$scope.formGroup.$setValidity('group', isValid);
  }

  update(api) {
    // include discovery service
    this.group.services = {
      discovery: this.discovery,
    };

    this.ApiService.update(api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.onApiUpdate();
      this.initialGroups = _.cloneDeep(api.proxy.groups);
    });
  }

  onApiUpdate() {
    this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    this.NotificationService.show('Group configuration saved');
    this.$state.go('management.apis.detail.proxy.endpoints');
  }

  reset() {
    this.$scope.duplicateEndpointNames = false;
    this.$state.reload();
  }

  backToEndpointsConfiguration() {
    this.reset();
    this.api.proxy.groups = _.cloneDeep(this.initialGroups);
    this.$state.go('management.apis.detail.proxy.endpoints');
  }

  checkEndpointNameUniqueness() {
    this.$scope.duplicateEndpointNames = this.ApiService.isEndpointNameAlreadyUsed(this.api, this.group.name);
  }
}

export default ApiEndpointGroupController;
