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
  constructor(UserService, TeamService, $mdDialog) {
    'ngInject';
    this.UserService = UserService;
    this.TeamService = TeamService;
    this.listTeams();
    this.$mdDialog = $mdDialog;
  }

  get(code) {
    this.UserService.get(code).then(response => {
      this.user = response.data;
    });
  }

  listTeams() {
    this.TeamService.list().then(response => {
      this.teams = response.data;
    });
  }

  list() {
    this.UserService.list().then(response => {
      this.users = response.data;
    });
  }

  showAddUserModal(user) {
    var that = this;
    this.$mdDialog.show({
      controller: DialogUserController,
      templateUrl: 'app/user/user.dialog.html',
      parent: angular.element(document.body),
      user: user,
      roles: this.roles,
    }).then(function () {
      that.list();
    });
  }

  showSaveTeamModal(team) {
    var that = this;
    this.$mdDialog.show({
      controller: DialogTeamController,
      templateUrl: 'app/user/team.dialog.html',
      parent: angular.element(document.body),
      team: team
    }).then(function () {
      that.listTeams();
    });
  }
}

function DialogTeamController($scope, $mdDialog, TeamService, team, NotificationService) {
  'ngInject';

  $scope.team = team;
  $scope.creationMode = !team;

  $scope.cancel = function () {
    $mdDialog.cancel();
  };

  $scope.save = function (team) {
    var save = $scope.creationMode ? TeamService.create(team) : TeamService.update(team);
    save.then(function () {
      NotificationService.show($scope.creationMode ? 'Team created with success!' : 'Team updated with success!');

      $mdDialog.hide();
    }).catch(function (error) {
      NotificationService.show(error.data.message);
    });
  };
}

function DialogUserController($scope, $mdDialog, UserService, user, roles) {
  'ngInject';

  $scope.user = user;
  $scope.roles = roles;

  UserService.listTeams(user.code).then(response => {
    $scope.teams = response.data;
  });

  $scope.cancel = function () {
    $mdDialog.cancel();
  };

  $scope.create = function (user) {
    UserService.create(user).then(function () {
      $mdDialog.hide();
    }).catch(function (error) {
      $scope.error = error;
    });
  };
}

export default UserController;
