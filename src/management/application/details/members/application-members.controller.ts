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
import * as angular from 'angular';

import _ = require('lodash');
import ApplicationService from '../../../../services/application.service';
import NotificationService from '../../../../services/notification.service';
import RoleService from '../../../../services/role.service';
import GroupService from "../../../../services/group.service";
import UserService from "../../../../services/user.service";
import { StateService } from '@uirouter/core';

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
  private newPrimaryOwner: any;

  constructor(
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private $state: StateService,
    private RoleService: RoleService,
    private GroupService: GroupService,
    private UserService: UserService
  ) {
    'ngInject';

    const that = this;
    this.newPrimaryOwner = null;
    RoleService.list('APPLICATION').then(function (roles) {
      that.roles = roles;
      that.newPORoles = _.filter(roles, (role: any)=>{
        return role.name !== "PRIMARY_OWNER";});
      that.newPORole = _.find(roles, (role: any) => {
        return role.default;
      });
    });
  }

  $onInit() {
    this.groupById = _.keyBy(this.resolvedGroups, "id");
    this.displayGroups = {};
    _.forEach(this.resolvedGroups, (grp) => {
      this.displayGroups[grp.id] = false;
    });
    this.groupMembers = {};
    this.groupIdsWithMembers = [];
    if (this.application.groups) {
      let self = this;
      _.forEach(this.application.groups, (grp) => {
        this.GroupService.getMembers(grp).then((members) => {
          let filteredMembers = _.filter(members.data, (m: any) => {
            return m.roles["APPLICATION"]
          });
          if (filteredMembers.length > 0) {
            self.groupMembers[grp] = filteredMembers;
            self.groupIdsWithMembers.push(grp)
          }
        });
      });
    }
  }

  updateMember(member) {
    if (member.role) {
      this.ApplicationService.addOrUpdateMember(this.application.id, _.pick(member, ['id', 'reference', 'role']) as any).then(() => {
        this.NotificationService.show(`Member ${member.displayName} has been updated with role ${member.role}`);
      });
    }
  }

  deleteMember(member) {
    let index = this.members.indexOf(member);
    this.ApplicationService.deleteMember(this.application.id, member.id).then(() => {
      this.members.splice(index, 1);
      this.NotificationService.show(`${member.displayName} has been removed`);
    });
  }

  showDeleteMemberConfirm(ev, member) {
    ev.stopPropagation();
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirm.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Would you like to remove the member ?',
        msg: '',
        confirmButton: 'Remove'
      }
    }).then(function (response) {
      if (response) {
        that.deleteMember(member);
      }
    });
  }

  showAddMemberModal(ev) {
    let that = this;
    this.$mdDialog.show({
      controller: 'DialogAddMemberController',
      template: require('./addMember.dialog.html'),
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      locals: {
        application: that.application,
        members: that.members
      }
    }).then(function (application) {
      if (application) {
        that.$state.go('management.applications.application.members', {applicationId: that.application.id}, {reload: true});
      }
    }, function() {
       // You cancelled the dialog
    });
  }

  searchUser(query) {
    if (query) {
      return this.UserService.search(query).then((response) => {
        return _.filter(response.data, (user:any) => {
          return  user.id === undefined || _.findIndex(this.members,
              function(member: any) {
                return member.id === user.id && member.role === 'PRIMARY_OWNER';
              }) === -1;
        });
      });
    } else {
      return _.filter(this.members, (member: any) => { return member.role !== 'PRIMARY_OWNER'; });
    }
  }

  getMembershipDisplay(member): string {
    if (! member.displayName) {
      return member.username;
    }

    return (member.username)
      ? member.displayName + ' (' + member.username + ')'
      : member.displayName;
  }

  getMembershipAvatar(member): string {
    return (member.id) ? this.UserService.getUserAvatar(member.id) : 'assets/default_photo.png';
  }

  selectedUserChange(user) {
    if (user) {
      this.newPrimaryOwner = user;
    } else {
      if (this.newPrimaryOwner !== null) {
        this.newPrimaryOwner = null;
      }
    }
  }

  showTransferOwnershipConfirm(ev) {
    this.$mdDialog.show({
      controller: 'DialogTransferApplicationController',
      controllerAs: '$ctrl',
      template: require('./transferApplication.dialog.html'),
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose:true,
      locals: {
        newRole: this.newPORole
      }
    }).then((transferApplication) => {
      if (transferApplication) {
        this.transferOwnership(this.newPORole.name);
      }
    }, () => {
      // You cancelled the dialog
    });
  }

  private transferOwnership(newRole: string) {
    let ownership = {
      id: this.newPrimaryOwner.id,
      reference: this.newPrimaryOwner.reference,
      role: newRole
    };

    this.ApplicationService.transferOwnership(this.application.id, ownership).then(() => {
      this.NotificationService.show("API ownership changed !");
      this.$state.go('management.applications.list');
    });
  }

  isAllowedToTransferOwnership() {
    return this.UserService.currentUser.isAdmin() || this.isPrimaryOwner();
  }

  isPrimaryOwner() {
    return this.UserService.currentUser.id === this.application.owner.id;
  }
}

export default ApplicationMembersController;
