/**
 * Created by david on 27/11/2015.
 */
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
class ApiMembersController {
  constructor (ApiService, resolvedApi, resolvedMembers, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;
    this.members = resolvedMembers.data;
    this.membershipTypes = [ 'primary_owner', 'owner', 'user' ];
  }

  updateMember(member) {
    this.ApiService.addOrUpdateMember(this.api.id, member).then(() => {
      this.NotificationService.show('Member updated');
    });
  }

  deleteMember(member) {
    var index = this.members.indexOf(member);
    this.ApiService.deleteMember(this.api.id, member.user).then(() => {
      this.members.splice(index, 1);
      this.NotificationService.show("Member " + member.user + " has been removed successfully");
    });
  }

  showAddMemberModal(ev) {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddMemberApiController',
      templateUrl: 'app/api/admin/members/addMember.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      api: _this.api,
			apiMembers: _this.members
    }).then(function (api) {
      if (api) {
        //that.getMembers(api.id);
      }
    }, function() {
      // You cancelled the dialog
    });
  }
}

export default ApiMembersController;
