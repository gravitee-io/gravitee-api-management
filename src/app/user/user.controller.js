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
/* global document:false */
class UserController {
  constructor(UserService, $mdDialog, $location) {
    'ngInject';
    this.UserService = UserService;
    this.UserService.listRoles().then(response => {
      this.roles = response.data;
    });
    this.list();
    this.$mdDialog = $mdDialog;
    this.$location = $location;
  }

  get(code) {
    this.UserService.get(code).then(response => {
      this.user = response.data;
    });
  }

  list() {
    this.UserService.list().then(response => {
      this.users = response.data;
    });
  }

  showAddUserModal(user) {
    this.$mdDialog.show({
      controller: DialogController,
      templateUrl: 'app/user/user.dialog.html',
      parent: angular.element(document.body),
      user: user,
      roles: this.roles,
    }).then(function (user) {
      if (user) {
        this.list();
      }
    });
  }
}

function DialogController($scope, $mdDialog, UserService, user, roles) {
  'ngInject';

  $scope.user = user;
  $scope.roles = roles;

  UserService.listTeams(user.code).then(response => {
    $scope.teams = response.data;
  });

  $scope.hide = function () {
    $mdDialog.hide();
  };

  $scope.create = function (user) {
    UserService.create(user).then(function () {
      $mdDialog.hide(user);
    }).catch(function (error) {
      $scope.error = error;
    });
  };
}

export default UserController;
