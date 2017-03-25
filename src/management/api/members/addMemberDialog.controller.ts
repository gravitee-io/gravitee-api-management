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

function DialogAddMemberApiController($scope, $mdDialog, api, apiMembers, ApiService, UserService, NotificationService) {
  'ngInject';

	$scope.api = api;
	$scope.apiMembers = apiMembers;
	$scope.user = {};
	$scope.usersFound = [];
	$scope.usersSelected = [];
	$scope.searchText = "";

  $scope.hide = function () {
     $mdDialog.cancel();
  };

	$scope.searchUser = function (query) {
		if (query) {
			return UserService.search(query).then(function(response) {
				var membersFound = response.data;
        var filterMembers = _.filter(membersFound, function(member:any) {
          return _.findIndex($scope.apiMembers,
                              function(apiMember:any) { return apiMember.username === member.id;}) === -1;
        });
				return filterMembers;
			});
		}
	};

  $scope.selectedItemChange = function(item) {
		if (item) {
			if (!$scope.isUserSelected(item)) {
				$scope.usersFound.push(item);
				$scope.selectMember(item);
			}
			$scope.searchText = "";
		}
  };

	$scope.selectMember = function(user) {
		var idx = $scope.usersSelected.indexOf(user.id);
    if (idx > -1) {
      $scope.usersSelected.splice(idx, 1);
    }
    else {
      $scope.usersSelected.push(user.id);
    }
	};

	$scope.isUserSelected = function(user) {
		var idx = $scope.usersSelected.indexOf(user.id);
    return idx > -1;
	};

  $scope.addMembers = function () {
		for (var i = 0; i < $scope.usersSelected.length; i++) {
			var username = $scope.usersSelected[i];
			var member = {
				"username" : username,
				"type" : "USER"
			};
      ApiService.addOrUpdateMember($scope.api.id, member).then(function() {
				NotificationService.show('Member ' + username + ' has been added to the API');
			}).catch(function (error) {
				NotificationService.show('Error while adding member ' + username);
			  $scope.error = error;
			});
		}
		$mdDialog.hide($scope.api);
  };
}

export default DialogAddMemberApiController;
