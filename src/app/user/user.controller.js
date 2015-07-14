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
  constructor(UserService, $mdDialog) {
    'ngInject';
    this.UserService = UserService;

    this.users = [
      {
        lastName: 'Géraud',
        firstName: 'Nicolas',
        roles: ['ROLE_ADMIN, ROLE_APIS']
      },
      {
        lastName: 'Brassely',
        firstName: 'David',
        roles: ['ROLE_APIS']
      },
      {
        lastName: 'Elamrani',
        firstName: 'Azize',
        roles: ['ROLE_USER']
      }
    ];
    this.list();
    this.$mdDialog = $mdDialog;
  }

  list() {
    this.UserService.list().then(response => {
      this.users = response.data;
    });
  }

  showAddUserModal(ev) {
    this.$mdDialog.show({
      controller: DialogController,
      templateUrl: 'app/user/user.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
    }).then(function (user) {
      if (user) {
        this.list();
      }
    });
  }
}

function DialogController($scope, $mdDialog, UserService) {
  'ngInject';
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
