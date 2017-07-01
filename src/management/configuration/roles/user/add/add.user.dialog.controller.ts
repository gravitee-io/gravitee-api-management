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
import UserService from '../../../../../services/user.service';
import NotificationService from '../../../../../services/notification.service';
import RoleService from '../../../../../services/role.service';

function DialogAddUserRoleController($mdDialog: angular.material.IDialogService, role, roleScope, $q: ng.IQService,
                                     UserService: UserService, NotificationService: NotificationService, RoleService: RoleService) {
  'ngInject';

  this.role = role;
  this.roleScope = roleScope;
  this.roleUsers = [];

  this.usersFound = [];
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
			return UserService.search(query).then(response => {
        let usersFound = response.data;
        let filterMembers = _.filter(_.values(usersFound), function(user:any) {
          return _.findIndex(that.roleUsers,
              function(roleUser:any) { return roleUser.username === user.id;}) === -1;
        });
        return filterMembers;
      });
		}
	};

  this.selectedItemChange = function(item) {
		if (item) {
			if (!this.isUserSelected(item)) {
				this.usersFound.push(item);
				this.selectUser(item);
			}
			this.searchText = "";
		}
  };

	this.selectUser = function(user) {
		let idx = this.usersSelected.indexOf(user.id);
    if (idx > -1) {
      this.usersSelected.splice(idx, 1);
    }
    else {
      this.usersSelected.push(user.id);
    }
	};

	this.isUserSelected = function(user) {
		let idx = this.usersSelected.indexOf(user.id);
    return idx > -1;
	};

  this.addUsers = function () {
    let promises: Array<any> = [];
		for (let i = 0; i < this.usersSelected.length; i++) {
      let username = this.usersSelected[i];
      promises.push(RoleService.addRole(username, this.roleScope, this.role));
		}

		$q.all(promises).then((response) => {
      NotificationService.show('Users ' + this.usersSelected + ' has been added successfully to the role');
		  $mdDialog.hide(response);
    });
  };
}

export default DialogAddUserRoleController;
