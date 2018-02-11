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
import * as _ from 'lodash';
import UserService from "../../../../services/user.service";

function DialogAddGroupMemberController($scope, $mdDialog, group, UserService: UserService) {
  'ngInject';

  $scope.groupItem = group;
  $scope.usersSelected = [];
  $scope.searchText = "";

  $scope.hide = function () {
    $mdDialog.cancel();
  };

  $scope.searchUser = function (query) {
    if (query) {
      return UserService.search(query).then(function(response) { return response.data });
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
      $scope.searchText = '';
    }
  };

  $scope.addMembers = function () {
    let members = [];
    for (let i = 0; i < $scope.usersSelected.length; i++) {
      let member = $scope.usersSelected[i];
      let membership = {
        id: member.id,
        reference: member.reference,
        displayName: (!member.firstname || !member.lastname) ? member.username : (member.firstname + ' ' + member.lastname),
        roles: {API: '', APPLICATION: ''}
      };

      members.push(membership);
    }
    $mdDialog.hide(members);
  };
}

export default DialogAddGroupMemberController;
