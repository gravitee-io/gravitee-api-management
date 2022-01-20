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
import '@gravitee/ui-components/wc/gv-schema-form';

class ApiEndpointController {
  private api: any;
  private endpoint: any;
  private initialEndpoints: any;
  private tenants: any;
  private connectors: any[];
  private creation = false;
  private supportedTypes: string[];
  private schema: any;

  constructor(
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private $scope,
    private $rootScope: ng.IRootScopeService,
    private $state: StateService,
    private $stateParams: StateParams,
    private resolvedTenants,
    private resolvedConnectors,
  ) {
    'ngInject';

    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.tenants = resolvedTenants.data;
    this.connectors = resolvedConnectors.data;
    this.$scope.groupName = $stateParams.groupName;
    this.$scope.duplicateEndpointNames = false;
  }

  $onInit() {
    const group = this.findGroupByName(this.$stateParams.groupName);
    this.$scope.endpoint = this.findEndpointByName(group, this.$stateParams.endpointName);
    this.initialEndpoints = group ? _.cloneDeep(group.endpoints) : [];
    this.supportedTypes = this.connectors.map((connector) => connector.supportedTypes).reduce((acc, val) => acc.concat(val), []);

    // Creation mode
    if (!this.$scope.endpoint) {
      this.$scope.endpoint = {
        weight: 1,
        inherit: true,
        type: 'http',
      };
      this.creation = true;
    } else if (this.$scope.endpoint.type) {
      this.$scope.endpoint.type = this.$scope.endpoint.type.toLowerCase();
    }

    this.updateSchema();
  }

  changeType() {
    this.updateSchema();
  }

  findGroupByName(groupName: string) {
    return this.api.proxy.groups.find((group) => group.name === groupName);
  }

  findEndpointByName(group: any, endpointName: string) {
    if (group && group.endpoints) {
      return group.endpoints.find((endpoint) => endpoint.name === endpointName);
    }
    return null;
  }

  update(api) {
    const group: any = this.findGroupByName(this.$stateParams.groupName);

    if (!_.includes(group.endpoints, this.$scope.endpoint)) {
      group.endpoints = group.endpoints || [];
      group.endpoints.push(this.$scope.endpoint);
    }

    this.ApiService.update(api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.onApiUpdate();
      this.initialEndpoints = _.cloneDeep(group.endpoints);
    });
  }

  onApiUpdate() {
    this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    this.NotificationService.show('Endpoint saved');
    this.$state.go('management.apis.detail.proxy.endpoints');
  }

  reset() {
    this.$scope.duplicateEndpointNames = false;
    this.$state.reload();
  }

  backToEndpointsConfiguration() {
    const group: any = _.find(this.api.proxy.groups, { name: this.$stateParams.groupName });
    group.endpoints = _.cloneDeep(this.initialEndpoints);
    this.$state.go('management.apis.detail.proxy.endpoints');
  }

  checkEndpointNameUniqueness() {
    this.$scope.duplicateEndpointNames = this.ApiService.isEndpointNameAlreadyUsed(this.api, this.$scope.endpoint.name);
  }

  updateEndpoint(event) {
    const isValid = !event.detail?.validation?.errors?.length;
    if (isValid) {
      // Abstract endpoint properties to keep
      const { backup, inherit, name, target, type, weight } = this.$scope.endpoint;
      // Delete all properties, important if a property is deleted by the user with the form
      for (const prop of Object.getOwnPropertyNames(this.$scope.endpoint)) {
        delete this.$scope.endpoint[prop];
      }
      // Reassign all desired properties and keep reference
      Object.assign(this.$scope.endpoint, { backup, inherit, name, target, type, weight }, event.detail.values);
    }
    this.$scope.formEndpoint.$setDirty();
    this.$scope.formEndpoint.$setValidity('endpoint', isValid);
  }

  findCurrentSchema() {
    return this.connectors.find((connector) => connector.supportedTypes.includes(this.$scope.endpoint?.type?.toLowerCase()))?.schema;
  }

  updateSchema() {
    const currentSchema = this.findCurrentSchema();
    if (currentSchema) {
      const schema = JSON.parse(currentSchema);
      if (this.$scope.endpoint?.type?.toLowerCase() === 'grpc') {
        schema.properties.http.disabled = ['version'];
        this.$scope.endpoint.http = this.$scope.endpoint.http || {};
        this.$scope.endpoint.http.version = 'HTTP_2';
      }
      this.schema = schema;
    }
  }

  // FIXME: https://github.com/gravitee-io/issues/issues/6437
  canHealthCheck() {
    return (
      this.creation === false && (this.$scope.endpoint.type.toLowerCase() === 'http' || this.$scope.endpoint.type.toLowerCase() === 'grpc')
    );
  }
}

export default ApiEndpointController;
