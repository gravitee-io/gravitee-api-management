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
import { find } from 'lodash';

class ApiV1ResourcesControllerAjs {
  resolvedApi: any;
  resolvedResources: any;

  private creation: boolean;
  private resourceJsonSchemaForm: string[];
  private types: any[];
  private resource: any;
  private resourceJsonSchema: any;

  constructor(
    private ApiService,
    private $mdSidenav,
    private $mdDialog,
    private ResourceService,
    private NotificationService,
    private $scope,
    private $rootScope,
    private $timeout,
  ) {
    this.creation = true;
    this.resourceJsonSchemaForm = ['*'];
  }

  $onInit() {
    this.types = this.resolvedResources;
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
      if (!this.resource.configuration) {
        this.resource.configuration = {};
      }

      this.ResourceService.getSchema(this.resource.type).then(
        ({ data }) => {
          this.resourceJsonSchema = data;
          return {
            schema: data,
          };
        },
        response => {
          if (response.status === 404) {
            return {
              schema: {},
            };
          } else {
            this.NotificationService.showError('Unexpected error while loading resource schema for ' + this.resource.type);
          }
        },
      );
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

    this.ResourceService.getSchema(this.resource.type).then(
      ({ data }) => {
        this.resourceJsonSchema = data;
        return {
          schema: data,
        };
      },
      response => {
        if (response.status === 404) {
          this.resourceJsonSchema = {};
          return {
            schema: {},
          };
        } else {
          // todo manage errors
          this.NotificationService.showError('Unexpected error while loading resource schema for ' + this.resource.type);
        }
      },
    );
  }

  deleteResource(resourceIdx) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('html-loader!../../../components/dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to remove this resource?',
          msg: '',
          confirmButton: 'Remove',
        },
      })
      .then(response => {
        if (response) {
          this.resolvedApi.resources.splice(resourceIdx, 1);
          this.updateApi();
        }
      });
  }

  saveResource() {
    // FIXME: Create a new object with only the wanted properties
    // eslint-disable-next-line angular/no-private-call
    delete this.resource.$$hashKey;

    if (this.creation) {
      this.resolvedApi.resources.push(this.resource);
    }

    this.updateApi();
  }

  updateApi() {
    let api = this.resolvedApi;
    return this.ApiService.update(api).then(({ data }) => {
      this.closeResourcePanel();
      this.$rootScope.$broadcast('apiChangeSuccess', { api: data });
      this.NotificationService.show("API '" + data.name + "' saved");

      this.$timeout(() => {
        api = data;
      });
    });
  }

  getResourceTypeName(resourceTypeId) {
    return find(this.types, { id: resourceTypeId }).name;
  }
}
ApiV1ResourcesControllerAjs.$inject = [
  'ApiService',
  '$mdSidenav',
  '$mdDialog',
  'ResourceService',
  'NotificationService',
  '$scope',
  '$rootScope',
  '$timeout',
];

export default ApiV1ResourcesControllerAjs;
