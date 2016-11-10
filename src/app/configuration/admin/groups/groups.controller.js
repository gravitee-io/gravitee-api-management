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
class GroupsController {
  constructor($scope, GroupService, ApplicationService, ApiService, NotificationService, $q, $mdDialog, $mdSidenav) {
    'ngInject';
    this.$scope = $scope;
    this.$q = $q;
    this.$mdSidenav = $mdSidenav;
    this.GroupService = GroupService;
    this.ApplicationService = ApplicationService;
    this.NotificationService = NotificationService;
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.groupType = "APPLICATION";
    this.applicationGroups = [];
    this.apiGroups = [];
    this.listGroups();
  }

  listGroups() {
    this.GroupService.list().then( listResponse => {
      const promises = _.map(listResponse.data, group => {
        return this.GroupService.getMembers(group.id).then( getMembersResponse  => {
          return {
            group,
            members: getMembersResponse.data
          };
        }).then(groupWithMembers => {
          if(groupWithMembers.group.type === "application") {
            return this.ApplicationService.listByGroup(groupWithMembers.group.id).then (applicationsResponse => {
              return {
                group: groupWithMembers.group,
                members: groupWithMembers.members,
                applications: applicationsResponse.data
              };
            });
          } else {
            return this.ApiService.listByGroup(groupWithMembers.group.id).then (apiResponse => {
              return {
                group: groupWithMembers.group,
                members: groupWithMembers.members,
                apis: apiResponse.data
              };
            });
          }
        });
      });

      this.$q.all(promises).then(responses => {
        var partition = _.partition(responses, item => {
          return item.group.type === "application";
        });
        this.applicationGroups = partition[0];
        this.apiGroups = partition[1];
      });
    });
  }

  showMembers(item) {
    this.selectedGroup = item;
    this.$mdSidenav('group-members').toggle();
  }

  showApis(item) {
    this.selectedGroup = item;
    this.$mdSidenav('group-apis').toggle();
  }

  showApplications(item) {
    this.selectedGroup = item;
    this.$mdSidenav('group-applications').toggle();
  }

  selectGroupType(type) {
    this.groupType = type;
  }

  showAddGroupModal() {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddGroupController',
      controllerAs: 'dialogAddGroupCtrl',
      templateUrl: 'app/configuration/admin/groups/dialog/add-group.dialog.html',
      currentName: "",
      action: "Add",
      clickOutsideToClose: true
    }).then( (name) => {
      if (name) {
        _this.GroupService.create({
          name: name,
          type: _this.groupType
        }).then(() => {
          _this.listGroups();
        });
      }
    });
  }

  showRenameGroupModal(ev, groupId, name) {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddGroupController',
      controllerAs: 'dialogAddGroupCtrl',
      templateUrl: 'app/configuration/admin/groups/dialog/add-group.dialog.html',
      currentName: name,
      action: "Edit",
      clickOutsideToClose: true
    }).then( (name) => {
      if (name) {
        _this.GroupService.update(groupId, name)
          .then(() => {
            _this.listGroups();
        });
      }
    });
  }

  showAddMemberModal(ev) {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddGroupMemberController',
      templateUrl: 'app/configuration/admin/groups/dialog/addMember.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      group: _this.selectedGroup
    }).then( (members) => {
      if (members) {
        _this.selectedGroup.members = _.unionWith(members, _this.selectedGroup.members, _.isEqual);
      }
    }, () => {
      // You cancelled the dialog
    });
  }

  removeGroup(ev, groupId, groupName) {
    var confirm = this.$mdDialog.confirm()
      .title('Would you like to remove the group "' + groupName + '" ?')
      .ariaLabel('delete-group')
      .ok('OK')
      .cancel('Cancel')
      .targetEvent(ev);
    var _this = this;
    this.$mdDialog.show(confirm).then( () => {
      _this.GroupService.remove(groupId).then( () => {
        _this.listGroups();
      });
    }, () => {
      _this.$mdDialog.cancel();
    });
  }

  removeMember(ev, username) {
    var confirm = this.$mdDialog.confirm()
      .title('Would you like to remove the user "' + username + '" ?')
      .ariaLabel('delete-member')
      .ok('OK')
      .cancel('Cancel')
      .targetEvent(ev);
    var _this = this;
    this.$mdDialog.show(confirm).then( () => {
      _this.GroupService.deleteMember(_this.selectedGroup.group.id, username).then( () => {
        _this.NotificationService.show('Member ' + username + ' has been removed from the group');
        _.remove(_this.selectedGroup.members, (m) => {
          return m.username === username;
        });

      });
    }, () => {
      _this.$mdDialog.cancel();
    });
  }

  updateMember(member) {
    var _this = this;
    this.GroupService.addOrUpdateMember(this.selectedGroup.group.id, member).then(()=>{
      _this.NotificationService.show('Member ' + member.username + ' has been updated');
    });
  }
}
export default GroupsController;
