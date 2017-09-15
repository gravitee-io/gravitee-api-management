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
import * as _ from 'lodash';

class ApiResourcesController {
  private creation: boolean;
  private resourceJsonSchemaForm: string[];
  private types: any[];
  private resource: any;
  private resourceJsonSchema: any;

  constructor (
    private ApiService,
    private $mdSidenav,
    private $mdDialog,
    private ResourceService,
    private NotificationService,
    private $scope,
    private $rootScope,
    private resolvedResources,
    private $timeout
  ) {
    'ngInject';
    this.creation = true;
    this.resourceJsonSchemaForm = ["*"];

    this.types = resolvedResources.data;
  }

  initState() {
    if (this.resource !== undefined) {
      this.$scope.resourceEnabled = this.resource.enabled;
    } else {
      this.$scope.resourceEnabled = false;
    }
  }

  switchEnabled() {
    if (this.resource === undefined) {
      this.resource = {};
    }
    this.resource.enabled = this.$scope.resourceEnabled;
    this.updateApi();
  }

  showResourcePanel(resource) {
    this.$mdSidenav('resource-config').toggle();

    if (resource) {
      // Update resource
      this.creation = false;
      this.resource = resource;
      if (! this.resource.configuration) {
        this.resource.configuration = {};
      }

      this.ResourceService.getSchema(this.resource.type).then( ({data}) => {
        this.resourceJsonSchema = data;
          return {
            schema: data
          };
        },
        (response) => {
          if ( response.status === 404) {
            return {
              schema: {}
            };
          } else {
            this.NotificationService.showError('Unexpected error while loading resource schema for ' + this.resource.type);
          }
        });

    } else {
      // Create new resource
      this.resourceJsonSchema = {};
      this.creation = true;
      this.resource = {};
      this.resource.configuration = {};
      this.resource.enabled = true;
    }

    this.initState();
  }

  closeResourcePanel() {
    this.$mdSidenav('resource-config').close();
  }

  onTypeChange() {
    this.resource.configuration = {};

    this.ResourceService.getSchema(this.resource.type).then( ({data}) => {
        this.resourceJsonSchema = data;
        return {
          schema: data
        };
      },
      (response) => {
        if ( response.status === 404) {
          this.resourceJsonSchema = {};
          return {
            schema: {}
          };
        } else {
          //todo manage errors
          this.NotificationService.showError('Unexpected error while loading resource schema for ' + this.resource.type);
        }
      });
  }

  deleteResource(resourceIdx) {
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to remove this resource ?',
        msg: '',
        confirmButton: 'Remove'
      }
    }).then(function (response) {
      if (response) {
        that.$scope.$parent.apiCtrl.api.resources.splice(resourceIdx, 1);
        that.updateApi();
      }
    });
  }

  saveResource() {
    delete this.resource.$$hashKey;

    if (this.creation) {
      this.$scope.$parent.apiCtrl.api.resources.push(this.resource);
    }

    this.updateApi();
  }

  updateApi() {
    const that = this;

    let api = this.$scope.$parent.apiCtrl.api;
    return this.ApiService.update(api).then( ( {data} ) => {
      that.closeResourcePanel();
      that.$rootScope.$broadcast('apiChangeSuccess', {api: data});
      that.NotificationService.show('API \'' + data.name + '\' saved');

      that.$timeout(function () {
        api = data;
      });
    });
  }

  getResourceTypeName(resourceTypeId) {
    return _.find(this.types, { 'id': resourceTypeId }).name;
  }
}

export default ApiResourcesController;
