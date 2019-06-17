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
import UserService from "../../../services/user.service";
import NotificationService from "../../../services/notification.service";
import {User} from "../../../entities/user";
import { StateService } from '@uirouter/core';
import {IScope} from "angular";

const UsersComponent: ng.IComponentOptions = {
  bindings: {
    usersPage: '<'
  },
  template: require("./users.html"),
  controller: function (
    UserService: UserService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
    $state: StateService,
    $rootScope: IScope
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$onInit = () => {
      this.onPaginate = this.onPaginate.bind(this);
      this.query = $state.params.q;
    };

    this.remove = (ev: Event, user: User) => {
      ev.stopPropagation();
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          msg: '',
          title: 'Would you like to remove the user "' + user.displayName + '" ?',
          confirmButton: 'Remove'
        }
      }).then( (response) => {
        if (response) {
          UserService.remove(user.id).then((response) => {
            NotificationService.show('User ' + user.displayName + ' has been removed.');
            $state.reload();
          });
        }
      });
    };

    this.onPaginate = (page: number) => {
      UserService.list(this.query, page).then((response)=> {
        this.usersPage = response.data;
      });
    };

    this.getUserPicture = (user) => {
      return UserService.getUserAvatar(user.id);
    };

    this.search = () => {
      $state.go('.', {q: this.query});
    };

    this.newUser = () => {
      $state.go('management.settings.newuser');
    };
  }
};

export default UsersComponent;
