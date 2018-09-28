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
import ApiService from "../../../../services/api.service";
import NotificationService from "../../../../services/notification.service";
import DialogConfigureLoggingEditorController from "./configure-logging-editor.dialog.controller";

class ApiLoggingConfigurationController {

  private initialApi: any;
  private api: any;
  private formLogging: any;

  constructor(
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private $stateParams,
    private $rootScope,
    private $scope
  ) {
  'ngInject';

    this.initialApi = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);

    this.$scope.loggingModes = [
      {
        name: 'None',
        value: 'NONE'
      }, {
        name: 'Client only',
        value: 'CLIENT'
      }, {
        name: 'Proxy only',
        value: 'PROXY'
      }, {
        name: 'Client and proxy',
        value: 'CLIENT_PROXY'
      }];
  }

  update() {
    this.ApiService.update(this.api).then((updatedApi) => {
      this.NotificationService.show('Logging configuration has been updated');
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.initialApi = _.cloneDeep(updatedApi.data);
      this.$scope.formLogging.$setPristine();
      this.$rootScope.$broadcast('apiChangeSuccess', {api: this.api});
    });
  }

  reset() {
    this.api = _.cloneDeep(this.initialApi);

    if (this.$scope.formLogging) {
      this.$scope.formLogging.$setPristine();
      this.$scope.formLogging.$setUntouched();
    }
  }

  showConditionEditor(index) {
    this.$mdDialog.show({
      controller: DialogConfigureLoggingEditorController,
      controllerAs: '$ctrl',
      template: require('./configure-logging-editor.dialog.html'),
      clickOutsideToClose: true,
      resolve: {
        subscribers: ($stateParams, ApiService: ApiService) =>
          ApiService.getSubscribers($stateParams.apiId).then(response => response.data),

        plans: ($stateParams, ApiService: ApiService) =>
          ApiService.getApiPlans($stateParams.apiId).then(response => response.data)
      }
    }).then((condition) => {
      if (condition) {
        this.api.proxy.logging.condition = condition;
        this.$scope.formLogging.$setDirty();
      }
    }, function () {
      // Cancel of the dialog
    });
  }
}

export default ApiLoggingConfigurationController;
