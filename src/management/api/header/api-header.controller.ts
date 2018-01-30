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
import NotificationService from '../../../services/notification.service';
import ApiService from '../../../services/api.service';
import angular = require('angular');

import _ = require('lodash');
class ApiHeaderController {
  public api: any;
  public apiEnabled: boolean;

  constructor(private ApiService: ApiService,
              private NotificationService: NotificationService,
              private $mdDialog: angular.material.IDialogService,
              private $rootScope) {
    'ngInject';
  }

  $onInit() {
    this.apiEnabled = this.api.state === 'started';
  }

  changeLifecycle() {
    let started = this.api.state === 'started';
    this.apiEnabled = !this.apiEnabled;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: `Are you sure you want to ${started ? 'stop' : 'start'} the API ?`,
        msg: '',
        confirmButton: (started ? 'stop' : 'start')
      }
    }).then((response: boolean) => {
      if (response) {
        if (started) {
          this.ApiService.stop(this.api).then((response) => {
            this.api.state = 'stopped';
            this.api.etag = response.headers('etag');
            this.apiEnabled = false;
            this.NotificationService.show(`API ${this.api.name} has been stopped!`);
            this.$rootScope.$broadcast('apiChangeSuccess', {api: _.cloneDeep(this.api)});
          });
        } else {
          this.ApiService.start(this.api).then((response) => {
            this.api.state = 'started';
            this.api.etag = response.headers('etag');
            this.apiEnabled = true;
            this.NotificationService.show(`API ${this.api.name} has been started!`);
            this.$rootScope.$broadcast('apiChangeSuccess', {api: _.cloneDeep(this.api)});
          });
        }
      }
    })
  }

}

export default ApiHeaderController;
