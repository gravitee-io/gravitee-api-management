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

class ApiResponseTemplatesController {
  private api: any;
  private templates: any;

  constructor(
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private $rootScope: ng.IRootScopeService,
    private $scope,
    private $stateParams: StateParams,
    private $state: StateService,
    private $mdDialog: angular.material.IDialogService,
  ) {
    'ngInject';

    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.templates = this.api.response_templates;
  }

  hasTemplates() {
    return this.api.response_templates && Object.keys(this.api.response_templates).length > 0;
  }

  countTypes(templates) {
    return Object.keys(templates).length;
  }

  remove(key) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete response templates?',
          confirmButton: 'Delete',
        },
      })
      .then((response) => {
        if (response) {
          delete this.api.response_templates[key];

          this.ApiService.update(this.api).then((updatedApi) => {
            this.api = updatedApi.data;
            this.api.etag = updatedApi.headers('etag');
            this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
            this.NotificationService.show('Response templates for key ' + key + ' have been deleted !');
            this.$state.go('management.apis.detail.proxy.responsetemplates.list');
          });
        }
      });
  }
}

export default ApiResponseTemplatesController;
