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
import RoleService from '../../../../services/role.service';
import NotificationService from '../../../../services/notification.service';
import { StateService } from '@uirouter/core';
import * as _ from 'lodash';
import {IScope} from 'angular';

const RoleMembersComponent: ng.IComponentOptions = {
  bindings: {
    members: '<'
  },
  template: require('./role.members.html'),
  controller: function ( RoleService: RoleService,
                         $mdDialog: angular.material.IDialogService,
                         NotificationService: NotificationService,
                         $state: StateService,
                         $stateParams,
                         $rootScope: IScope) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.loadedMembers = [];
    this.roleScope = $stateParams.roleScope;
    this.role = $stateParams.role;
    this.$onInit = () => {
      if (this.roleScope !== 'ORGANIZATION' && this.roleScope !== 'ENVIRONMENT' ) {
        $state.go('management.settings');
      }
    };

    this.loadMoreMembers = () => {
      let loadedMembersLength = this.loadedMembers ? this.loadedMembers.length : 0;
      this.loadedMembers = _.take(this.members, 20 + loadedMembersLength);
    };

    this.deleteUser = (member, index) => {
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete the user "' + member.displayName + '" from role "' + this.role + '" ?',
          confirmButton: 'Remove'
        }
      }).then( (response) => {
        if (response) {
          RoleService.deleteUser({scope: this.roleScope, name: this.role}, member.id).then( () => {
            NotificationService.show(`User ${member.displayName} no longer has the role ${this.role}`);
            this.members.splice(index, 1);
            this.loadedMembers.splice(index, 1);
          });
        }
      });
    };

    this.reloadMembers = () => {
      RoleService.listUsers(this.roleScope, this.role).then( (members) => {
        this.members = members;
        this.loadedMembers = [];
        this.loadMoreMembers();
      });
    };

    this.addUserRole = () => {
      const that = this;
      $mdDialog.show({
        controller: 'DialogAddUserRoleController',
        controllerAs: '$ctrl',
        template: require('./add.user.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          roleScope: this.roleScope,
          role: this.role
        }
      }).then((usernames) => {
        if (usernames && usernames.length > 0) {
            that.reloadMembers();
        }
      });
    };
  }
};

export default RoleMembersComponent;
