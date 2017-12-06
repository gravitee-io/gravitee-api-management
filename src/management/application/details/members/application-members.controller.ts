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
import ApplicationService from '../../../../services/applications.service';
import NotificationService from '../../../../services/notification.service';
import RoleService from '../../../../services/role.service';
import GroupService from "../../../../services/group.service";
import UserService from "../../../../services/user.service";

class ApplicationMembersController {
  private application: any;
  private members: any;
  private roles: any;
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
    private $state: ng.ui.IStateService,
    private RoleService: RoleService,
    private GroupService: GroupService,
    private UserService: UserService
  ) {
    'ngInject';

    const that = this;
    this.newPrimaryOwner = null;
    RoleService.list('APPLICATION').then(function (roles) {
      that.roles = roles;
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
      this.ApplicationService.addOrUpdateMember(this.application.id, member).then(() => {
        this.NotificationService.show(`Member ${member.username} has been updated with role ${member.role}`);
      });
    }
  }

  deleteMember(member) {
    let index = this.members.indexOf(member);
    this.ApplicationService.deleteMember(this.application.id, member.username).then(() => {
      this.members.splice(index, 1);
      this.NotificationService.show(`${member.username} has been removed`);
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
        applicationMembers: that.members
      }
    }).then(function (application) {
      if (application) {
        that.$state.go('management.applications.portal.members', {applicationId: that.application.id}, {reload: true});
      }
    }, function() {
       // You cancelled the dialog
    });
  }

  showPermissionsInformation() {
    this.$mdDialog.show({
      controller: 'DialogApplicationPermissionsHelpController',
      controllerAs: 'ctrl',
      template: require('./permissions.dialog.html'),
      parent: angular.element(document.body),
      clickOutsideToClose:true
    });
  }

  searchUser(query) {
    if (query) {
      return this.UserService.search(query).then((response) => {
        const usersFound = response.data;
        let filterUsers = _.filter(usersFound, (user:any) => {
          return _.findIndex(this.members,
            function(applicationMember: any) {
              return applicationMember.username === user.id && applicationMember.role === 'PRIMARY_OWNER';
            }) === -1;
        });
        return filterUsers;
      });
    } else {
      let filterMembers = _.filter(this.members, function(member: any) { return member.role !== 'PRIMARY_OWNER'; });
      let members = _.flatMap(filterMembers, function(member: any) { return { 'id' : member.username, 'label' : member.firstname? member.firstname + ' ' + member.lastname : member.username}; });
      return members;
    }
  }

  selectedItemChange(item) {
    if (item) {
      this.newPrimaryOwner = item;
    } else {
      if (this.newPrimaryOwner !== null) {
        this.newPrimaryOwner = null;
      }
    }
  }

  showTransferOwnershipConfirm(ev) {
    this.$mdDialog.show({
      controller: 'DialogTransferApplicationController',
      template: require('./transferApplication.dialog.html'),
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose:true
    }).then((transferApplication) => {
      if (transferApplication) {
        this.transferOwnership();
      }
    }, () => {
      // You cancelled the dialog
    });
  }

  transferOwnership() {
    this.ApplicationService.transferOwnership(this.application.id, this.newPrimaryOwner.id).then(() => {
      this.NotificationService.show("API ownership changed !");
      this.$state.go('management.applications.list');
    });
  }

  isAllowedToTransferOwnership() {
    return this.UserService.currentUser.isAdmin() || this.isPrimaryOwner();
  }

  isPrimaryOwner() {
    return this.UserService.currentUser.username === this.application.owner.username;
  }
}

export default ApplicationMembersController;
