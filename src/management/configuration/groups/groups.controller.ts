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
import * as _ from 'lodash';
import * as angular from 'angular';
import GroupService from "../../../services/group.service";
import ApplicationService from "../../../services/applications.service";
import ApiService from "../../../services/api.service";
import NotificationService from "../../../services/notification.service";
import RoleService from "../../../services/role.service";

class GroupsController {
  private groups: any[];
  private selectedGroup: any;
  private apiRoles: any;
  private applicationRoles: any;

  constructor(
    private GroupService: GroupService,
    private ApplicationService: ApplicationService,
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private $q,
    private $mdDialog,
    private $mdSidenav,
    private RoleService: RoleService
  ) {
    'ngInject';
    this.groups = [];
    this.initRoles();
    this.listGroups();
  }

  listGroups() {
    this.GroupService.list().then( listResponse => {
      const promises = _.map(listResponse.data, (group: any) => {
        return this.GroupService.getMembers(group.id).then( getMembersResponse  => {
          return {
            group,
            members: getMembersResponse.data
          };
        })
          .then(groupWithMembers => {
            return this.ApplicationService.listByGroup(groupWithMembers.group.id).then (applicationsResponse => {
              return {
                group: groupWithMembers.group,
                members: groupWithMembers.members,
                applications: applicationsResponse.data
              };
            }).then (groupWithMembersAndApps => {
              return this.ApiService.listByGroup(groupWithMembersAndApps.group.id).then (apiResponse => {
                return {
                  group: groupWithMembersAndApps.group,
                  members: groupWithMembersAndApps.members,
                  applications: groupWithMembersAndApps.applications,
                  apis: apiResponse.data
                };
              });
          });
        });
      });

      this.$q.all(promises).then(responses => {
        this.groups = responses;
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

  initRoles() {
    const that = this;
    this.RoleService.list("API").then(function (roles) {
      that.apiRoles = [{"scope":"API", "name": "", "system":false}].concat(roles);
    });
    this.RoleService.list("APPLICATION").then(function (roles) {
      that.applicationRoles = [{"scope":"APPLICATION", "name": "", "system":false}].concat(roles);
    });
  }

  showAddGroupModal() {
    let _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddGroupController',
      controllerAs: 'dialogAddGroupCtrl',
      template: require('./dialog/add-group.dialog.html'),
      currentName: '',
      currentDefaultApi: false,
      currentDefaultApplication: false,
      action: 'Add',
      clickOutsideToClose: true
    }).then( (newGroup) => {
      if (newGroup && newGroup.name) {
        _this.GroupService.create(newGroup).then(() => {
          _this.listGroups();
        });
      }
    });
  }

  showRenameGroupModal(ev, groupId, name, event_rules) {
    ev.stopPropagation();
    let _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddGroupController',
      controllerAs: 'dialogAddGroupCtrl',
      template: require('./dialog/add-group.dialog.html'),
      currentName: name,
      currentDefaultApi: _.indexOf(_.map(event_rules, "event"), "API_CREATE") >= 0,
      currentDefaultApplication: _.indexOf(_.map(event_rules, "event"), "APPLICATION_CREATE") >= 0,
      action: 'Edit',
      clickOutsideToClose: true
    }).then( (updatedGroup) => {
      if (updatedGroup && updatedGroup.name) {
        _this.GroupService.update(groupId, updatedGroup).then(() => {
          _this.listGroups();
        });
      }
    });
  }

  showAddMemberModal(ev) {
    let _this = this;
    this.$mdDialog.show({
      controller: 'DialogAddGroupMemberController',
      template: require('./dialog/addMember.dialog.html'),
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      group: _this.selectedGroup
    }).then( (members) => {
      if (members) {
        console.log(members);
        _this.selectedGroup.members = _.unionWith(members, _this.selectedGroup.members, _.isEqual);
      }
    }, () => {
      // you cancelled the dialog
    });
  }

  removeGroup(ev, groupId, groupName) {
    ev.stopPropagation();
    let _this = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Would you like to remove the group "' + groupName + '" ?',
        confirmButton: 'Remove'
      }
    }).then( (response) => {
      if (response) {
        _this.GroupService.remove(groupId).then( () => {
          _this.listGroups();
        });
      }
    });
  }

  removeMember(ev, member) {
    ev.stopPropagation();
    let _this = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        msg: '',
        title: 'Would you like to remove the user "' + member.displayName + '" ?',
        confirmButton: 'Remove'
      }
    }).then((response) => {
      if (response) {
        _this.GroupService.deleteMember(_this.selectedGroup.group.id, member.id).then( () => {
          _this.NotificationService.show('Member ' + member.displayName + ' has been removed from the group');
          _.remove(_this.selectedGroup.members, (m: any) => {
            return m.id === member.id;
          });
        });
      }
    });
  }

  updateMember(member) {
    if (member.roles) {
      let _this = this;
      let promise = this.GroupService.addOrUpdateMember(this.selectedGroup.group.id, member);
      if (promise) {
        promise.then(() => {
          _this.NotificationService.show('Member ' + member.displayName + ' has been updated');
        });
      }
    }
  }
}
export default GroupsController;
