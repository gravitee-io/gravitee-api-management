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
import angular = require('angular');
import _ = require('lodash');

import { ApiService } from '../../../../../services/api.service';
import UserService from '../../../../../services/user.service';

class ApiMembersController {
  private api: any;
  private members: any;
  private newPrimaryOwner: any;
  private groupMembers: any;
  private groupIdsWithMembers: any;
  private roles: any;
  private newPORoles: any[];
  private newPORole: any;
  private groupById: any;
  private displayGroups: any;
  constructor(
    private ApiService: ApiService,
    private resolvedMembers,
    private resolvedGroups,
    private resolvedApiGroups,
    private $mdDialog: ng.material.IDialogService,
    private NotificationService,
    private $scope,
    private UserService: UserService,
    private GroupService,
    private RoleService,
  ) {
    'ngInject';
    this.api = this.$scope.$parent.apiCtrl.api;
    this.members = resolvedMembers.data;
    this.newPrimaryOwner = null;
    this.$scope.searchText = '';
    this.groupById = _.keyBy(resolvedGroups, 'id');
    this.displayGroups = {};
    _.forEach(resolvedGroups, (grp) => {
      this.displayGroups[grp.id] = false;
    });
    this.groupMembers = {};
    this.groupIdsWithMembers = [];

    if (this.api.groups && this.UserService.isUserHasPermissions(['api-member-r'])) {
      ApiService.getGroupsWithMembers(this.api.id).then(({ data: groupsWithMembers }) => {
        this.groupMembers = groupsWithMembers;
        this.groupIdsWithMembers = Object.keys(groupsWithMembers);
      });
    }

    RoleService.list('API').then((roles) => {
      this.roles = roles;
      this.newPORoles = _.filter(roles, (role: any) => {
        return role.name !== 'PRIMARY_OWNER';
      });
      this.newPORole = _.find(roles, (role: any) => {
        return role.default;
      });
    });
  }

  updateMember(member) {
    if (member.role) {
      this.ApiService.addOrUpdateMember(this.api.id, _.pick(member, ['id', 'reference', 'role']) as any).then(() => {
        this.NotificationService.show('Member ' + member.displayName + ' has been updated with role ' + member.role);
      });
    }
  }

  deleteMember(member) {
    const index = this.members.indexOf(member);
    this.ApiService.deleteMember(this.api.id, member.id).then(() => {
      this.members.splice(index, 1);
      this.NotificationService.show('Member ' + member.displayName + ' has been removed');
    });
  }

  isPrimaryOwner() {
    return this.UserService.currentUser.id === this.api.owner.id;
  }

  showAddMemberModal(ev) {
    this.$mdDialog
      .show({
        controller: 'DialogAddMemberApiController',
        template: require('./addMember.dialog.html'),
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose: true,
        locals: {
          api: this.api,
          members: this.members,
        },
      })
      .then(
        (api) => {
          if (api) {
            this.ApiService.getMembers(api.id).then((response) => {
              this.members = response.data;
            });
          }
        },
        () => {
          // You cancelled the dialog
        },
      );
  }

  showDeleteMemberConfirm(ev, member) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../../components/dialog/confirm.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to remove the member?',
          confirmButton: 'Remove',
        },
      })
      .then((response) => {
        if (response) {
          this.deleteMember(member);
        }
      });
  }

  getMembershipDisplay(member): string {
    if (!member.displayName) {
      return member.username;
    }

    return member.username ? member.displayName + ' (' + member.username + ')' : member.displayName;
  }

  getMembershipAvatar(member): string {
    return member.id ? this.UserService.getUserAvatar(member.id) : 'assets/default_photo.png';
  }

  toggleDisableMembershipNotifications() {
    this.ApiService.update(this.api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.NotificationService.show('API ' + this.api.name + ' has been updated');
    });
  }
}

export default ApiMembersController;
