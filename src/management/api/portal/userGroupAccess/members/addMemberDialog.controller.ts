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
import _ = require('lodash');
import RoleService from "../../../../../services/role.service";
import ApiService from "../../../../../services/api.service";

function DialogAddMemberApiController($scope, $mdDialog, api, members, ApiService: ApiService, NotificationService,
      RoleService: RoleService, UserService) {
  'ngInject';

  RoleService.list('API').then(function (roles) {
    $scope.roles = roles;
  });

	$scope.api = api;
	$scope.members = members;
	$scope.usersSelected = [];
	$scope.searchText = "";

  $scope.hide = function () {
     $mdDialog.cancel();
  };

	$scope.searchUser = function (query) {
		if (query) {
			return UserService.search(query).then(function(response) {
        return _.filter(response.data, (user: any) => { return _.findIndex($scope.members, {id: user.id}) === -1;});
			});
		}
	};

  $scope.getUserAvatar = function(id?: string) {
    return (id) ? UserService.getUserAvatar(id) : 'assets/default_photo.png';
  };

  $scope.selectUser = function(user) {
		if (user && user.reference) {
      let selected = _.find($scope.usersSelected, {reference: user.reference});
      if (!selected) {
        $scope.usersSelected.push(user);
      }
      $scope.searchText = "";
		}
  };

  $scope.addMembers = function () {
		for (let i = 0; i < $scope.usersSelected.length; i++) {
			let member = $scope.usersSelected[i];
      let membership = {
        id : member.id,
        reference : member.reference,
        role : $scope.role.name
			};
      ApiService.addOrUpdateMember($scope.api.id, membership).then(function() {
				NotificationService.show('User ' + member.displayName + ' has been added as a member.');
			}).catch(function (error) {
			  $scope.error = error;
			});
		}
		$mdDialog.hide($scope.api);
  };
}

export default DialogAddMemberApiController;
