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
class ApplicationMembersController {
  constructor(resolvedApplication, resolvedMembers, ApplicationService, NotificationService, $mdDialog) {
    'ngInject';
    this.application = resolvedApplication.data;
    this.members = resolvedMembers.data;
    this.membershipTypes = [ 'owner', 'user' ];
    this.ApplicationService = ApplicationService;
    this.NotificationService = NotificationService;
    this.$mdDialog = $mdDialog;
  }

  getMembers(applicationId) {
    this.ApplicationService.getMembers(applicationId).then(response => {
      this.members = response.data;
    });
  }

  updateMember(member) {
    this.ApplicationService.addOrUpdateMember(this.application.id, member).then(() => {
      this.NotificationService.show('Member ' + member.username + " has been updated with role " + member.type);
    });
  }

  deleteMember(member) {
    var index = this.members.indexOf(member);
    this.ApplicationService.deleteMember(this.application.id, member.username).then(() => {
      this.members.splice(index, 1);
      this.NotificationService.show("Member " + member.username + " has been removed");
    });
  }

  showDeleteMemberConfirm(ev, member) {
    var confirm = this.$mdDialog.confirm()
      .title('Would you like to remove the member?')
      .ariaLabel('delete-member')
      .ok('OK')
      .cancel('Cancel')
      .targetEvent(ev);
    var self = this;
    this.$mdDialog.show(confirm).then(function() {
      self.deleteMember(member);
    }, function() {
      self.$mdDialog.cancel();
    });
  }

  showAddMemberModal(ev) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogAddMemberController',
      templateUrl: 'app/application/dialog/addMember.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      application: that.application,
      applicationMembers : that.members
    }).then(function (application) {
      if (application) {
        that.getMembers(application.id);
      }
    }, function() {
       // You cancelled the dialog
    });
  }

  showPermissionsInformation(ev) {
    this.$mdDialog.show({
      controller: 'DialogApplicationPermissionsHelpController',
      controllerAs: 'ctrl',
      templateUrl: 'app/application/details/members/permissions.dialog.html',
      parent: angular.element(document.body),
      clickOutsideToClose:true
    });
  }
}

export default ApplicationMembersController;
