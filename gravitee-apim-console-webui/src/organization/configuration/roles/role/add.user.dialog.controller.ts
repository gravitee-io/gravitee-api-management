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

import NotificationService from '../../../../services/notification.service';
import RoleService from '../../../../services/role.service';

function DialogAddUserRoleController(
  $mdDialog: angular.material.IDialogService,
  role,
  roleScope,
  $q: ng.IQService,
  NotificationService: NotificationService,
  RoleService: RoleService,
) {
  'ngInject';

  this.role = role;
  this.roleScope = roleScope;
  this.roleUsers = [];

  this.usersSelected = [];

  RoleService.listUsers(roleScope, role).then((users) => {
    this.roleUsers = users;
  });

  this.hide = function () {
    $mdDialog.cancel();
  };

  this.addUsers = function () {
    const promises: Array<any> = [];
    for (let i = 0; i < this.usersSelected.length; i++) {
      const member = this.usersSelected[i];
      const membership = {
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
