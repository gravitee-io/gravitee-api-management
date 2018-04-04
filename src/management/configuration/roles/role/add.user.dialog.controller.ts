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
import UserService from "../../../../services/user.service";
import NotificationService from "../../../../services/notification.service";
import RoleService from "../../../../services/role.service";


function DialogAddUserRoleController($mdDialog: angular.material.IDialogService, role, roleScope, $q: ng.IQService,
                                     UserService: UserService, NotificationService: NotificationService, RoleService: RoleService) {
  'ngInject';

  this.role = role;
  this.roleScope = roleScope;
  this.roleUsers = [];

  this.usersSelected = [];
  this.searchText = "";

  const that = this;
  RoleService.listUsers(roleScope, role).then(function (users) {
    that.roleUsers = users;
  });

  this.hide = function () {
     $mdDialog.cancel();
  };

  this.searchUser = function (query) {
    if (query) {
      return UserService.search(query).then(function(response) {
        return response.data;
      });
    }
  };

  this.getUserAvatar = function(id?: string) {
    return (id) ? UserService.getUserAvatar(id) : 'assets/default_photo.png';
  };

  this.selectUser = function(user) {
    if (user && user.reference) {
      let selected = _.find(this.usersSelected, {reference: user.reference});
      if (!selected) {
        this.usersSelected.push(user);
      }
      this.searchText = "";
    }
  };

  this.addUsers = function () {
    let promises: Array<any> = [];
		for (let i = 0; i < this.usersSelected.length; i++) {
      let member = this.usersSelected[i];
      let membership = {
        id: member.id,
        reference: member.reference,
      };

      promises.push(RoleService.addRole(this.roleScope, this.role, membership));
		}

		$q.all(promises).then((response) => {
      NotificationService.show('Users ' + _.map(this.usersSelected, 'displayName').join(',') + ' has been added successfully to the role');
		  $mdDialog.hide(response);
    });
  };
}

export default DialogAddUserRoleController;
