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
import angular = require('angular');
import _ = require('lodash');

class ApiMembersController {
  private api: any;
  private members: any;
  private membershipTypes: any;
  private newPrimaryOwner: any;
  private groupMembers: any;
  constructor (
    private ApiService,
    private resolvedApi,
    private resolvedMembers,
    private $state,
    private $mdDialog: ng.material.IDialogService,
    private NotificationService,
    private $scope,
    private UserService,
    private GroupService
  ) {
    'ngInject';
    this.api = resolvedApi.data;
    this.members = resolvedMembers.data;
    this.membershipTypes = [ 'owner', 'user' ];
    this.newPrimaryOwner = null;
    this.$scope.searchText = "";

    if (this.api.group) {
      GroupService.getMembers(this.api.group.id).then((members) => {
        this.groupMembers = members.data;
      });
    }
  }

  updateMember(member) {
    this.ApiService.addOrUpdateMember(this.api.id, member).then(() => {
      this.NotificationService.show('Member ' + member.username + " has been updated with role " + member.type);
    });
  }

  deleteMember(member) {
    var index = this.members.indexOf(member);
    this.ApiService.deleteMember(this.api.id, member.username).then(() => {
      this.members.splice(index, 1);
      this.NotificationService.show("Member " + member.username + " has been removed");
    });
  }

	isOwner() {
    return this.api.permission && (this.api.permission === 'owner' || this.api.permission === 'primary_owner');
  }

  isPrimaryOwner() {
    return this.api.permission && (this.api.permission === 'primary_owner');
  }

  showAddMemberModal(ev) {
    this.$mdDialog.show({
      controller: 'DialogAddMemberApiController',
      templateUrl: 'api/admin/members/addMember.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      locals: {
        api: this.api,
        apiMembers: this.members
      }
    }).then((api) => {
      if (api) {
				this.ApiService.getMembers(api.id).then((response) => {
					this.members = response.data;
				});
      }
    }, function() {
      // You cancelled the dialog
    });
  }

  showPermissionsInformation() {
    this.$mdDialog.show({
      controller: 'DialogApiPermissionsHelpController',
      controllerAs: 'ctrl',
      templateUrl: 'api/admin/members/permissions.dialog.html',
      parent: angular.element(document.body),
      clickOutsideToClose:true
    });
  }

	showDeleteMemberConfirm(ev, member) {
    ev.stopPropagation();
    let self = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      templateUrl: 'components/dialog/confirm.dialog.html',
      clickOutsideToClose: true,
      locals: {
        title: 'Would you like to remove the member ?',
        confirmButton: 'Remove'
      }
    }).then(function (response) {
      if (response) {
        self.deleteMember(member);
      }
    });
  }

  searchUser(query) {
    if (query) {
      return this.UserService.search(query).then((response) => {
        var usersFound = response.data;
        var filterUsers = _.filter(usersFound, (user:any) => {
          return _.findIndex(this.members,
              function(apiMember: any) {
                return apiMember.username === user.id && apiMember.type === 'primary_owner';
              }) === -1;
        });
        return filterUsers;
      });
    } else {
      var filterMembers = _.filter(this.members, function(member: any) { return member.type !== 'primary_owner'; });
      var members = _.flatMap(filterMembers, function(member: any) { return { 'id' : member.username}; });
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
      controller: 'DialogTransferApiController',
      templateUrl: 'api/admin/members/transferAPI.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose:true
    }).then((transferAPI) => {
      if (transferAPI) {
        this.transferOwnership();
      }
    }, () => {
      // You cancelled the dialog
    });
  }

  transferOwnership() {
      this.ApiService.transferOwnership(this.api.id, this.newPrimaryOwner.id).then(() => {
        this.NotificationService.show("API ownership changed !");
        this.$state.go(this.$state.current, {}, {reload: true});
    });
  }
}

export default ApiMembersController;
