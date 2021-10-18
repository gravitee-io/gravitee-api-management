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
import UserService from '../../../services/user.service';
import NotificationService from '../../../services/notification.service';
import { User } from '../../../entities/user';
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';

const UsersComponent: ng.IComponentOptions = {
  bindings: {
    usersPage: '<',
  },
  template: require('./users.html'),
  controller: function (
    UserService: UserService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
    $state: StateService,
    $rootScope: IScope,
    $window,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$onInit = () => {
      this.onPaginate = this.onPaginate.bind(this);
      this.query = $state.params.q;
      $window.localStorage.usersTablePage = $state.params.page ? parseInt($state.params.page, 0) : 1;
    };

    this.remove = (ev: Event, user: User) => {
      ev.stopPropagation();
      $mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            msg:
              user.number_of_active_tokens > 0
                ? `The user has ${user.number_of_active_tokens} active token(s) that will be definitively removed.`
                : '',
            title: 'Are you sure you want to remove the user "' + user.displayName + '"?',
            confirmButton: 'Remove',
          },
        })
        .then((response) => {
          if (response) {
            UserService.remove(user.id).then((response) => {
              NotificationService.show('User ' + user.displayName + ' has been removed.');
              $state.reload();
            });
          }
        });
    };

    this.onPaginate = (page: number) => {
      $window.localStorage.usersTablePage = page;
      UserService.list(this.query, page).then((response) => {
        this.usersPage = response.data;
        $state.go('.', { page: page });
      });
    };

    this.getUserPicture = (user) => {
      return UserService.getUserAvatar(user.id);
    };

    this.search = () => {
      $window.localStorage.usersTableQuery = this.query;
      $window.localStorage.usersTablePage = 1;
      $state.go('.', { q: this.query, page: 1 });
    };

    this.newUser = () => {
      $state.go('organization.settings.newuser');
    };
  },
};

export default UsersComponent;
