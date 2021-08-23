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
import { IScope } from 'angular';

import { User } from '../entities/user';
import NotificationService from '../services/notification.service';
import TokenService from '../services/token.service';
import UserService from '../services/user.service';

interface IUserScope extends ng.IScope {
  formUser: any;
}

class UserController {
  private originalPicture: any;
  private user: User;
  private tokens: Array<any>;

  private fields: any[] = [];
  private groups = '';

  constructor(
    private UserService: UserService,
    private NotificationService: NotificationService,
    private $state: StateService,
    private $scope: IUserScope,
    private $rootScope: IScope,
    private TokenService: TokenService,
    private $mdDialog: angular.material.IDialogService,
    private Constants,
  ) {
    'ngInject';
  }

  $onInit() {
    this.UserService.customUserFieldsToRegister().then((resp) => (this.fields = resp.data));
    if (!this.user || (this.user && this.user.id === undefined)) {
      this.$state.go('login', {}, { reload: true, inherit: false });
    } else {
      this.originalPicture = this.getUserPicture();
      this.user.picture_url = this.getUserPicture();
      this.TokenService.list().then((response) => {
        this.tokens = response.data;
      });
      if (this.user.groupsByEnvironment) {
        const groupsByEnvironmentKeys = Object.keys(this.user.groupsByEnvironment);
        if (groupsByEnvironmentKeys.length === 1) {
          this.groups = Object.values(this.user.groupsByEnvironment)[0].join(' - ');
        } else {
          this.groups = groupsByEnvironmentKeys
            .map((envId) => {
              const env = this.Constants.org.environments.find((env) => env.id === envId);
              return `[${env.name}] ${this.user.groupsByEnvironment[envId].join('/')}`;
            })
            .join(' - ');
        }
      } else {
        this.groups = '-';
      }
    }
  }

  save() {
    this.UserService.save(this.user).then((response) => {
      this.user = response.data;
      this.user.picture_url = this.getUserPicture();
      this.$rootScope.$broadcast('graviteeUserRefresh', { user: this.user, refresh: true });
      this.$scope.formUser.$setPristine();
      this.NotificationService.show('User has been updated successfully');
    });
  }

  canDeleteMyAccount(): boolean {
    return !this.user.primaryOwner;
  }

  deleteMyAccount() {
    return this.$mdDialog
      .show({
        controller: 'DialogConfirmAndValidateController',
        controllerAs: 'ctrl',
        template: require('../components/dialog/confirmAndValidate.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete your account ?',
          warning: 'This operation is irreversible.',
          msg: 'After removing your account, you will be automatically logout.',
          validationMessage: 'Please, type in your username <code>' + this.user.displayName + '</code> to confirm.',
          validationValue: this.user.displayName,
          confirmButton: 'Yes, delete my account',
        },
      })
      .then((response) => {
        if (response) {
          return this.UserService.removeCurrentUser().then(() => {
            this.$state.go('logout');
            this.NotificationService.show('You have been successfully deleted');
          });
        }
      });
  }

  cancel() {
    delete this.user.picture;
    delete this.user.picture_url;
    this.$state.reload();
  }

  getUserPicture() {
    return this.UserService.currentUserPicture();
  }

  isInternalUser(): boolean {
    return this.user.source === 'gravitee' || this.user.source === 'memory';
  }

  generateToken() {
    this.$mdDialog
      .show({
        controller: 'DialogGenerateTokenController',
        controllerAs: 'ctrl',
        template: require('./token/generateToken.dialog.html'),
        clickOutsideToClose: false,
        escapeToClose: false,
        locals: {
          msg:
            'Any applications or scripts using this token will no longer be able to access the Gravitee.io management API. You cannot undo this action.',
          title: 'Generate Personal Access Token',
        },
      })
      .then((tokenGenerated) => {
        if (tokenGenerated) {
          this.$state.reload();
        }
      });
  }

  revoke(token) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          msg:
            'Any applications or scripts using this token will no longer be able to access the Gravitee.io management API. You cannot undo this action.',
          title: 'Are you sure you want to revoke the token "' + token.name + '"?',
          confirmButton: 'Revoke',
        },
      })
      .then((response) => {
        if (response) {
          this.TokenService.revoke(token).then(() => {
            this.NotificationService.show('Token "' + token.name + '" has been revoked.');
            this.$state.reload();
          });
        }
      });
  }
}

export default UserController;
