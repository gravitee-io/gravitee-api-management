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

import { StateService } from '@uirouter/core';
import * as _ from 'lodash';

import ApplicationService from '../../../../services/application.service';
import NotificationService from '../../../../services/notification.service';

interface IApplicationScope extends ng.IScope {
  formApplication: any;
}

class ApplicationGeneralController {
  private application: any;
  private initialApplication: any;

  private readonly = false;

  constructor(
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $state: StateService,
    private $scope: IApplicationScope,
    private $mdDialog: angular.material.IDialogService,
  ) {}

  $onInit() {
    if (!this.application.groups) {
      this.application.groups = [];
    }
    this.$scope.$on('applicationPictureChangeSuccess', (event, args) => {
      this.application.picture = args.image;
      this.$scope.formApplication.$setDirty();
    });
    this.$scope.$on('applicationBackgroundChangeSuccess', (event, args) => {
      this.application.background = args.image;
      this.$scope.formApplication.$setDirty();
    });

    this.initialApplication = _.cloneDeep(this.application);
  }

  update() {
    this.application.settings.oauth.application_type = this.application.type.toLowerCase();
    this.ApplicationService.update(this.application).then(() => {
      this.initialApplication = _.cloneDeep(this.application);
      this.$scope.formApplication.$setPristine();
      this.NotificationService.show(this.application.name + ' has been updated');
    });
  }

  delete() {
    this.ApplicationService.delete(this.application.id).then(() => {
      this.NotificationService.show(this.application.name + ' has been deleted');
      this.$state.go('management.applications.list', {}, { reload: true });
    });
  }

  reset() {
    _.assign(this.application, this.initialApplication);
    this.$scope.formApplication.$setPristine();
  }

  isOAuthClient() {
    return this.application.type.toLowerCase() !== 'simple';
  }

  showDeleteApplicationConfirm(ev) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogConfirmAndValidateController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmAndValidate.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to delete your application?',
          warning: 'This operation is irreversible.',
          msg: 'You will no longer be able to access this application.',
          validationMessage: 'Please, type in the name of the application <code>' + this.application.name + '</code> to confirm.',
          validationValue: this.application.name,
          confirmButton: 'Yes, delete this application',
        },
      })
      .then((response) => {
        if (response) {
          this.delete();
        }
      });
  }

  renewClientSecret(ev) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          msg: 'By renewing the client secret, you will no longer be able to generate new access tokens and call APIs.',
          title: 'Are you sure to renew the client secret?',
          confirmButton: 'Renew',
        },
      })
      .then((response) => {
        if (response) {
          this.ApplicationService.renewClientSecret(this.application.id).then((response) => {
            this.NotificationService.show('Client secret has been renew');
            this.application = response.data;
            this.initialApplication = _.cloneDeep(this.application);
            this.$scope.formApplication.$setPristine();
          });
        }
      });
  }

  onCopyClientIdSuccess(e) {
    this.NotificationService.show('ClientId has been copied to clipboard');
    e.clearSelection();
  }

  onCopyClientSecretSuccess(e) {
    this.NotificationService.show('ClientSecret has been copied to clipboard');
    e.clearSelection();
  }

  onUnauthorized() {
    this.readonly = true;
  }
}
ApplicationGeneralController.$inject = ['ApplicationService', 'NotificationService', '$state', '$scope', '$mdDialog'];

export default ApplicationGeneralController;
