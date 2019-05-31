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
import GroupService from "../../../services/group.service";
import NotificationService from "../../../services/notification.service";
import UserService from "../../../services/user.service";
import RoleService from "../../../services/role.service";
import _ = require('lodash');
import {IScope} from "angular";

interface IUserDetailComponentScope extends ng.IScope {
  selectedMgmtRole: string,
  selectedPortalRole: string,
  userApis: any[],
  userApplications: any[];
}
const UserDetailComponent: ng.IComponentOptions = {
  bindings: {
    selectedUser: '<',
    groups: '<',
    managementRoles: '<',
    portalRoles: '<',
    apiRoles: '<',
    applicationRoles: '<'
  },
  template: require("./user.html"),
  controller: function (
    $mdDialog: angular.material.IDialogService,
    NotificationService: NotificationService,
    GroupService: GroupService,
    UserService: UserService,
    RoleService: RoleService,
    $scope: IUserDetailComponentScope,
    $rootScope: IScope
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$onInit = () => {
      let idxPortalRole = this.selectedUser.roles[0].scope === 'portal' ? 0:1;
      let idxMgmtRole =   this.selectedUser.roles[0].scope === 'portal' ? 1:0;
      $scope.selectedMgmtRole = this.selectedUser.roles[idxMgmtRole].name;
      $scope.selectedPortalRole = this.selectedUser.roles[idxPortalRole].name;
      $scope.userApis = [];
      $scope.userApplications = [];
    };

    this.getUserPicture = () => {
      return UserService.getUserAvatar(this.selectedUser.id);
    };

    this.remove = (ev: Event, group: any) => {
      ev.stopPropagation();
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          msg: '',
          title: 'Would you like to remove the user from the group "' + group.name + '" ?',
          confirmButton: 'Remove'
        }
      }).then( (response) => {
        if (response) {
          GroupService.deleteMember(group.id, this.selectedUser.id).then( () => {
            NotificationService.show(this.selectedUser.displayName + ' has been removed from the group "' + group.name + '"');
            UserService.getUserGroups(this.selectedUser.id).then((response) =>
              this.groups = response.data
            );
          });
        }
      });
    };

    this.updateGroupRole = (group) => {
      let member = {
        id: this.selectedUser.id,
        roles: group.roles
      };

      let promise = GroupService.addOrUpdateMember(group.id, [member]);
      if (promise) {
        promise.then(() => {
          NotificationService.show('Role has been updated');
          UserService.getUserGroups(this.selectedUser.id).then((response) =>
            this.groups = response.data
          );
        });
      }
    };

    this.updateGlobalRole = (rolescope, rolename) => {
      RoleService.addRole(rolescope, rolename, {id: this.selectedUser.id}).then( (response) =>
        NotificationService.show("Role updated")
      );
    };

    this.addGroupDialog = () => {
      let that = this;
      GroupService.list().then( (groups) => {
        $mdDialog.show({
          controller: 'DialogAddUserGroupController',
          controllerAs: 'dialogCtrl',
          template: require('./dialog/addusergroup.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            groups: groups.data,
            apiRoles: this.apiRoles,
            applicationRoles: this.applicationRoles
          }
        }).then((groupWithRole) => {
          that.updateGroupRole(groupWithRole);
        });
      });
    };

    this.resetPasswordDialog = () => {
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to reset password of user "' + this.selectedUser.displayName + '"?',
          msg: 'An email with a link to change it will be sent to him',
          confirmButton: 'Reset'
        }
      }).then( (response) => {
        if (response) {
          UserService.resetPassword(this.selectedUser.id).then( () => {
            NotificationService.show('The password of user "' + this.selectedUser.displayName + '" has been successfully reset');
          });
        }
      });
    };

    this.loadUserApis = () => {
      UserService.getMemberships(this.selectedUser.id, "api").then( (response) => {
          let newApiList = [];
          _.forEach(response.data.metadata, (apiMetadata: any, apiId: string) => {
            newApiList.push( {
              id: apiId,
              name: apiMetadata.name,
              version: apiMetadata.version,
              visibility: apiMetadata.visibility
            });
          });
          $scope.userApis = _.sortBy(newApiList, "name");
        }
      );
    };

    this.loadUserApplications = () => {
      UserService.getMemberships(this.selectedUser.id, "application").then( (response) => {
          let newAppList = [];
          _.forEach(response.data.metadata, (appMetadata: any, appId: string) => {
            newAppList.push( {
              id: appId,
              name: appMetadata.name,
              type: appMetadata.type
            });
          });
          $scope.userApplications = _.sortBy(newAppList, "name");
        }
      );
    };
  }
};

export default UserDetailComponent;
