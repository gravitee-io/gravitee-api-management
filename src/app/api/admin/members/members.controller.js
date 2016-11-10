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
  constructor (ApiService, resolvedApi, resolvedMembers, $state, $mdDialog, NotificationService, $scope, UserService, GroupService) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.UserService = UserService;
    this.$scope = $scope;
    this.$state = $state;
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
				_this.ApiService.getMembers(api.id).then(function(response) {
					_this.members = response.data;
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
      templateUrl: 'app/api/admin/members/permissions.dialog.html',
      parent: angular.element(document.body),
      clickOutsideToClose:true
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

  searchUser(query) {
    if (query) {
      var _this = this;
      return this.UserService.search(query).then(function(response) {
        var usersFound = response.data;
        var filterUsers = _.filter(usersFound, function(user) {
          return _.findIndex(_this.members,
              function(apiMember) {
                return apiMember.username === user.id && apiMember.type === 'primary_owner';
              }) === -1;
        });
        return filterUsers;
      });
    } else {
      var filterMembers = _.filter(this.members, function(member) { return member.type !== 'primary_owner'; });
      var members = _.flatMap(filterMembers, function(member) { return { 'id' : member.username}; });
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
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogTransferApiController',
      templateUrl: 'app/api/admin/members/transferAPI.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose:true
    }).then(function (transferAPI) {
      if (transferAPI) {
        _this.transferOwnership();
      }
    }, function() {
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
