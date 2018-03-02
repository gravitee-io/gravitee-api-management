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
import {IScope} from 'angular';
import NotificationService from '../../../services/notification.service';
import _ = require('lodash');
import angular = require('angular');
import RoleService from '../../../services/role.service';

class RolesController {
  private roles: [any];
  private roleScopes: [string];
  private roleScopeSelectedIndex: number;

  constructor(
    private $state: ng.ui.IStateService,
    private $scope: IScope,
    private RoleService: RoleService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService) {
    'ngInject';

    this.RoleService.list(this.$state.params.roleScope).then(response => this.roles = response);
  }

  $onInit() {
    this.roleScopeSelectedIndex = this.roleScopes.indexOf(this.$state.params.roleScope);

    const that = this;
    this.$scope.$watch(function() { return that.roleScopeSelectedIndex}, function(newIndex) {
      that.$state.go(that.$state.current, {roleScope: that.roleScopes[newIndex]});
    });
  }

  editRole(role) {
    this.$state.go('management.settings.role.edit', {roleScope: this.$state.params.roleScope, role: role.name});
  }

  newRole() {
    this.$state.go('management.settings.role.new', {roleScope: this.$state.params.roleScope});
  }

  deleteRole(role) {
    const that = this;
    this.$mdDialog.show({
      controller: 'DeleteRoleDialogController',
      controllerAs: '$ctrl',
      template: require('./role/delete/delete.role.dialog.html'),
      locals: {
        role: role
      }
    }).then(function (deleteRole) {
      if (deleteRole) {
        that.RoleService.delete(role).then(function () {
          that.NotificationService.show("Role '" + role.name + "' deleted with success");
          _.remove(that.roles, role);
        });
      }
    });
  }

  addUserRole(role) {
    const that = this;
    this.$mdDialog.show({
      controller: 'DialogAddUserRoleController',
      controllerAs: '$ctrl',
      template: require('./user/add/add.user.dialog.html'),
      parent: angular.element(document.body),
      clickOutsideToClose: true,
      locals: {
        roleScope: this.$state.params.roleScope,
        role: role.name
      }
    }).then((usernames) => {
      if (usernames && usernames.length) {
        // load all role/users in case a user has been moved to a new role
        _.forEach(that.roles, function (role) {
          that.loadUsers(role);
        });
      }
    });
  }

  userManagementEnabled(role) {
    return this.RoleService.isUserRoleManagement(role.scope);
  }

  loadUsers(role) {
    return this.RoleService.listUsers(role.scope, role.name).then(function (users) {
      role.users = users;
    });
  }

  deleteUser(role, member) {
    const that = this;
    this.$mdDialog.show({
      controller: 'DeleteUserRoleDialogController',
      controllerAs: '$ctrl',
      template: require('./user/delete/delete.user.role.dialog.html'),
      locals: {
        role: role,
        username: member.username
      }
    }).then(function (deleteUserRole) {
      if (deleteUserRole) {
        that.RoleService.deleteUser(role, member.id).then(function () {
          that.NotificationService.show(`User ${member.username} no longer has the role ${role.name}`);
          that.loadUsers(role);
        });
      }
    });
  }
}

export default RolesController;
