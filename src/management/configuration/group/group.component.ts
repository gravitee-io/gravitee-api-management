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
import GroupService from '../../../services/group.service';
import NotificationService from '../../../services/notification.service';
import { StateService } from '@uirouter/core';
import _ = require('lodash');
import UserService from '../../../services/user.service';

interface IGroupDetailComponentScope extends ng.IScope {
  groupApis: any[];
  groupApplications: any[];
  selectedApiRole: string;
  selectedApplicationRole: string;
  currentTab: string;
  formGroup: any;
}
const GroupComponent: ng.IComponentOptions = {
  bindings: {
    group: '<',
    members: '<',
    apiRoles: '<',
    applicationRoles: '<',
    invitations: '<',
    tags: '<'
  },
  template: require('./group.html'),
  controller: function (
    GroupService: GroupService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
    $state: StateService,
    $scope: IGroupDetailComponentScope,
    UserService: UserService,
    $rootScope
  ) {
    'ngInject';
    this.$rootScope = $rootScope;

    this.$onInit = () => {
      $scope.groupApis = [];
      $scope.groupApplications = [];
      $scope.currentTab = 'users';

      if (this.group.roles) {
        $scope.selectedApiRole = this.group.roles.API;
        $scope.selectedApplicationRole = this.group.roles.APPLICATION;
      }
    };

    this.updateRole = (member: any) => {
      GroupService.addOrUpdateMember(this.group.id, [member]).then((response) => {
        NotificationService.show('Member successfully updated');
        $state.reload();
      });
    };

    this.update = () => {
      let roles: any = {};

      if ($scope.selectedApiRole) {
        roles.API = $scope.selectedApiRole;
      } else {
        delete roles.API;
      }

      if ($scope.selectedApplicationRole) {
        roles.APPLICATION = $scope.selectedApplicationRole;
      } else {
        delete roles.APPLICATION;
      }

      this.group.roles = roles;
      GroupService.update(this.group).then((response) => {
        this.group = response.data;
        this.$onInit();
        $scope.formGroup.$setPristine();
        NotificationService.show('Group \'' + this.group.name + '\' has been updated');
      });
    };

    this.removeUser = (ev, member: any) => {
      ev.stopPropagation();
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          msg: '',
          title: 'Are you sure you want to remove the user "' + member.displayName + '"?',
          confirmButton: 'Remove'
        }
      }).then( (response) => {
        if (response) {
          GroupService.deleteMember(this.group.id, member.id).then((response) => {
            NotificationService.show('Member ' + member.displayName + ' has been successfully removed');
            GroupService.getMembers(this.group.id).then(response =>
              this.members = response.data
            );
          });
        }
      });
    };

    this.showAddMemberModal = () => {
      $mdDialog.show({
        controller: 'DialogAddGroupMemberController',
        controllerAs: '$ctrl',
        template: require('./addMember.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          defaultApiRole: $scope.selectedApiRole,
          defaultApplicationRole: $scope.selectedApplicationRole,
          group: this.group,
          apiRoles: this.apiRoles,
          applicationRoles: this.applicationRoles,
          canChangeDefaultApiRole: this.canChangeDefaultApiRole,
          canChangeDefaultApplicationRole: this.canChangeDefaultApplicationRole
        }
      }).then( (members) => {
        if (members) {
          GroupService.addOrUpdateMember(this.group.id, members).then((response) => {
              NotificationService.show('Member(s) successfully added');
              $state.reload();
          });
        }
      }, () => {
        // you cancelled the dialog
      });
    };

    this.loadGroupApis = () => {
      GroupService.getMemberships(this.group.id, 'api').then( (response) => {
          $scope.groupApis = _.sortBy(response.data, 'name');
        }
      );
    };

    this.loadGroupApplications = () => {
      GroupService.getMemberships(this.group.id, 'application').then( (response) => {
          $scope.groupApplications = _.sortBy(response.data, 'name');
        }
      );
    };

    this.reset = () => {
      $state.reload();
    };

    this.canChangeDefaultApiRole = () => {
      return this.isSuperAdmin() || !this.group.lock_api_role;
    };

    this.canChangeDefaultApplicationRole = () => {
      return this.isSuperAdmin() || !this.group.lock_application_role;
    };

    this.canAddMembers = () => {
      if (this.isSuperAdmin()) {
        return true;
      } else if (this.group.manageable) {
        const numberOfMembers = this.members ? this.members.length : 0;
        const numberOfInvitations = this.invitations ? this.invitations.length : 0;
        const numberOfSlots = numberOfMembers + numberOfInvitations;
        return !this.group.max_invitation || (numberOfSlots < this.group.max_invitation);
      } else {
        return false;
      }
    };

    this.isSuperAdmin = () => {
      return UserService.isUserHasPermissions(['environment-group-u']);
    };

    this.canSave = () => {
      return this.group.manageable;
    };

    this.updateInvitation = (invitation: any) => {
      GroupService.updateInvitation(this.group.id, invitation).then(() => {
        NotificationService.show('Invitation successfully updated');
        $state.reload();
      });
    };

    this.showInviteMemberModal = () => {
      $mdDialog.show({
        controller: function($mdDialog, group, apiRoles, applicationRoles, defaultApiRole, defaultApplicationRole,
                             canChangeDefaultApiRole, canChangeDefaultApplicationRole) {
          'ngInject';
          this.group = group;
          this.group.api_role = group.api_role || defaultApiRole;
          this.group.application_role = group.application_role || defaultApplicationRole;
          this.apiRoles = apiRoles;
          this.applicationRoles = applicationRoles;
          this.canChangeDefaultApiRole = canChangeDefaultApiRole;
          this.canChangeDefaultApplicationRole = canChangeDefaultApplicationRole;
          this.hide = function () {$mdDialog.hide(); };
          this.save = function () {$mdDialog.hide(this.email); };
        },
        controllerAs: '$ctrl',
        template: require('./inviteMember.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          defaultApiRole: $scope.selectedApiRole,
          defaultApplicationRole: $scope.selectedApplicationRole,
          group: this.group,
          apiRoles: this.apiRoles,
          applicationRoles: this.applicationRoles,
          canChangeDefaultApiRole: this.canChangeDefaultApiRole,
          canChangeDefaultApplicationRole: this.canChangeDefaultApplicationRole
        }
      }).then((email) => {
        if (email) {
          GroupService.inviteMember(this.group, email).then((response) => {
            if (response.data.id) {
              NotificationService.show('Invitation successfully sent');
            } else {
              NotificationService.show('Member successfully added');
            }
            $state.reload();
          });
        }
      },
        // you cancelled the dialog
        () => {});
    };

    this.removeInvitation = (ev, invitation: any) => {
      ev.stopPropagation();
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          msg: '',
          title: 'Are you sure you want to remove the invitation for "' + invitation.email + '"?',
          confirmButton: 'Remove'
        }
      }).then((response) => {
          if (response) {
            GroupService.deleteInvitation(this.group.id, invitation.id).then(() => {
              NotificationService.show('Invitation for ' + invitation.email + ' has been successfully removed');
              $state.reload();
            });
          }
        },
        // you cancelled the dialog
        () => {});
    };

    this.hasGroupAdmin = () => {
      let hasGroupAdmin = false;
      _.forEach(this.members, (member) => {
        if (member.roles.GROUP && member.roles.GROUP === 'ADMIN') {
          hasGroupAdmin = true;
        }
      });
      return hasGroupAdmin;
    };
  }
};

export default GroupComponent;
