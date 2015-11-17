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
  constructor(UserService, TeamService, $mdDialog, $stateParams) {
    'ngInject';
    this.UserService = UserService;
    this.TeamService = TeamService;
    this.$mdDialog = $mdDialog;
		if ($stateParams.teamName) {
      this.getTeam($stateParams.teamName);
			this.listTeamMembers($stateParams.teamName);
			this.listTeamApis($stateParams.teamName);
			this.listTeamApplications($stateParams.teamName);
    } else {
       this.listTeams();
    }
  }

  get(code) {
    this.UserService.get(code).then(response => {
      this.user = response.data;
    });
  }

	getTeam(name) {
		this.TeamService.get(name).then(response => {
			this.team = response.data;
		});
	}

	listTeamMembers(teamName) {
		this.TeamService.listMembers(teamName).then(response => {
			this.teamMembers = response.data;
		});
	}

  listTeamApis(teamName) {
		this.TeamService.listApis(teamName).then(response => {
			this.teamApis = response.data;		
		});
	}

	listTeamApplications(teamName) {
		this.TeamService.listApplications(teamName).then(response => {
			this.teamApplications = response.data;		
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

	updateTeam(team) {
		this.TeamService.update(team).then(response => {
			NotificationService.show('Team updated with success!');
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

	showAddMemberModal(team) {
		var that = this;
		this.$mdDialog.show({
      controller: DialogAddMemberController,
      templateUrl: 'app/user/teamAddMember.dialog.html',
      parent: angular.element(document.body),
      team: team,
    }).then(function () {
      that.list();
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

function DialogAddMemberController($scope, $mdDialog, TeamService, UserService, team) {
	'ngInject';
	$scope.team = team;
	$scope.member = {};
	$scope.query = "";

	$scope.searchUsers = function() {
		UserService.get($scope.query).then(function(response) {
			$scope.member = response.data;
			$scope.query = "";
		}).catch(function (error) {
			$scope.error = error;
			$scope.query = "";
		});
	};

	$scope.addMember = function(team) {
		TeamService.addMember(team, $scope.member.username).then(function(response) {
			NotificationService.show('Member added successfully');
			$mdDialog.hide();
		}).catch(function (error) {
			NotificationService.show('Error while adding member to the team');
		});
	}
	$scope.cancel = function () {
    $mdDialog.cancel();
  };
}

export default UserController;
