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

import ApplicationService from '../../../../services/applications.service';
import NotificationService from '../../../../services/notification.service';
import RoleService from '../../../../services/role.service';

class ApplicationMembersController {
  private application: any;
  private members: any;
  private roles: any;

  constructor(
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private $state: ng.ui.IStateService,
    private RoleService: RoleService
  ) {
    'ngInject';

    const that = this;
    RoleService.list('APPLICATION').then(function (roles) {
      that.roles = roles;
    });
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
}

export default ApplicationMembersController;
