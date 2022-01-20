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
import { StateService } from '@uirouter/core';
import * as angular from 'angular';
import * as _ from 'lodash';

import ApplicationService from '../../../../services/application.service';
import GroupService from '../../../../services/group.service';
import NotificationService from '../../../../services/notification.service';
import RoleService from '../../../../services/role.service';
import UserService from '../../../../services/user.service';

class ApplicationMembersController {
  private application: any;
  private members: any;
  private roles: any;
  private newPORoles: any[];
  private newPORole: any;
  private groupById: any;
  private displayGroups: any;
  private groupMembers: any;
  private groupIdsWithMembers: any;
  private resolvedGroups: any;
  private usersSelected = [];
  private userFilterFn;
  private defaultUsersList: string[];

  constructor(
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private $state: StateService,
    private RoleService: RoleService,
    private GroupService: GroupService,
    private UserService: UserService,
  ) {
    'ngInject';

    RoleService.list('APPLICATION').then((roles) => {
      this.roles = roles;
      this.newPORoles = _.filter(roles, (role: any) => {
        return role.name !== 'PRIMARY_OWNER';
      });
      this.newPORole = _.find(roles, (role: any) => {
        return role.default;
      });
    });
  }

  $onInit() {
    this.groupById = _.keyBy(this.resolvedGroups, 'id');
    this.displayGroups = {};
    _.forEach(this.resolvedGroups, (grp) => {
      this.displayGroups[grp.id] = false;
    });
    this.groupMembers = {};
    this.groupIdsWithMembers = [];
    if (this.application.groups) {
      _.forEach(this.application.groups, (grp) => {
        this.GroupService.getMembers(grp).then((members) => {
          const filteredMembers = _.filter(members.data, (m: any) => {
            return m.roles.APPLICATION;
          });
          if (filteredMembers.length > 0) {
            this.groupMembers[grp] = filteredMembers;
            this.groupIdsWithMembers.push(grp);
          }
        });
      });
    }

    this.userFilterFn = (user: any) => {
      return (
        user.id === undefined ||
        _.findIndex(this.members, (member: any) => {
          return member.id === user.id && member.role === 'PRIMARY_OWNER';
        }) === -1
      );
    };

    this.defaultUsersList = _.filter(this.members, (member: any) => {
      return member.role !== 'PRIMARY_OWNER';
    });
  }

  updateMember(member) {
    if (member.role) {
      this.ApplicationService.addOrUpdateMember(this.application.id, _.pick(member, ['id', 'reference', 'role']) as any).then(() => {
        this.NotificationService.show(`Member ${member.displayName} has been updated with role ${member.role}`);
      });
    }
  }

  deleteMember(member) {
    const index = this.members.indexOf(member);
    this.ApplicationService.deleteMember(this.application.id, member.id).then(() => {
      this.members.splice(index, 1);
      this.NotificationService.show(`${member.displayName} has been removed`);
    });
  }

  showDeleteMemberConfirm(ev, member) {
    ev.stopPropagation();
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirm.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to remove the member?',
          msg: '',
          confirmButton: 'Remove',
        },
      })
      .then((response) => {
        if (response) {
          this.deleteMember(member);
        }
      });
  }

  showAddMemberModal(ev) {
    this.$mdDialog
      .show({
        controller: 'DialogAddMemberController',
        template: require('./addMember.dialog.html'),
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose: true,
        locals: {
          application: this.application,
          members: this.members,
        },
      })
      .then(
        (application) => {
          if (application) {
            this.$state.go('management.applications.application.members', { applicationId: this.application.id }, { reload: true });
          }
        },
        () => {
          // You cancelled the dialog
        },
      );
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

  showTransferOwnershipConfirm(ev) {
    this.$mdDialog
      .show({
        controller: 'DialogTransferApplicationController',
        controllerAs: '$ctrl',
        template: require('./transferApplication.dialog.html'),
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose: true,
        locals: {
          newRole: this.newPORole,
        },
      })
      .then(
        (transferApplication) => {
          if (transferApplication) {
            this.transferOwnership(this.newPORole.name);
          }
        },
        () => {
          // You cancelled the dialog
        },
      );
  }

  isAllowedToTransferOwnership() {
    return this.UserService.currentUser.isAdmin() || this.isPrimaryOwner();
  }

  isPrimaryOwner() {
    return this.UserService.currentUser.id === this.application.owner.id;
  }

  toggleDisableMembershipNotifications() {
    this.ApplicationService.update(this.application).then((updatedApplication) => {
      this.application = updatedApplication.data;
      this.NotificationService.show('Application ' + this.application.name + ' has been updated');
    });
  }

  private transferOwnership(newRole: string) {
    const ownership = {
      id: this.usersSelected[0].id,
      reference: this.usersSelected[0].reference,
      role: newRole,
    };

    this.ApplicationService.transferOwnership(this.application.id, ownership).then(() => {
      this.NotificationService.show('API ownership changed !');
      this.$state.go('management.applications.list');
    });
  }
}

export default ApplicationMembersController;
