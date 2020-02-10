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
import RoleService from '../../../services/role.service';
import NotificationService from '../../../services/notification.service';
import { StateService } from '@uirouter/core';

const RolesComponent: ng.IComponentOptions = {
  bindings: {
    roleScopes: '<',
    organizationRoles: '<',
    environmentRoles: '<',
    apiRoles: '<',
    applicationRoles: '<'
  },
  template: require('./roles.html'),
  controller: function(
    RoleService: RoleService,
    $mdDialog: angular.material.IDialogService,
    NotificationService: NotificationService,
    $state: StateService
  ) {
    'ngInject';
    this.rolesByScope = {};

    this.$onInit = () => {
      this.rolesByScope.ORGANIZATION = this.organizationRoles;
      this.rolesByScope.ENVIRONMENT = this.environmentRoles;
      this.rolesByScope.API = this.apiRoles;
      this.rolesByScope.APPLICATION = this.applicationRoles;
    };

    this.newRole = (roleScope) => {
      $state.go('management.settings.rolenew', {roleScope: roleScope});
    };

    this.deleteRole = (role) => {
      let that = this;
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete the role "' + role.name + '" ?',
          confirmButton: 'Remove'
        }
      }).then( (response) => {
        if (response) {
          RoleService.delete(role).then(function () {
            NotificationService.show('Role \'' + role.name + '\' deleted with success');
            _.remove(that.rolesByScope[role.scope], role);
          });
        }
      });
    };

    this.idUserManagementEnabled = (role) => {
      return RoleService.isUserRoleManagement(role.scope);
    };

    this.manageMembers = (role) => {
      $state.go('management.settings.rolemembers', {roleScope: role.scope, role: role.name});
    };
  }
};

export default RolesComponent;
